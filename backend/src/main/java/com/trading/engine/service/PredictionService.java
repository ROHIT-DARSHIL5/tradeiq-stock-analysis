package com.trading.engine.service;

// ============================================================
// PredictionService.java — Price Prediction using Linear Regression
//
// MACHINE LEARNING: Simple Linear Regression
//   Finds the best-fit line through historical price data
//   and extends it into the future.
//
// FORMULA (Ordinary Least Squares):
//   Line equation: y = β₀ + β₁x
//   where:
//     y  = predicted price
//     x  = day number (1, 2, 3, ...)
//     β₁ = slope = (n·Σxy - Σx·Σy) / (n·Σx² - (Σx)²)
//     β₀ = intercept = (Σy - β₁·Σx) / n
//
// COEFFICIENT OF DETERMINATION (R²):
//   R² = 1 - (SS_res / SS_tot)
//   where:
//     SS_res = Σ(yᵢ - ŷᵢ)²   [sum of squared residuals]
//     SS_tot = Σ(yᵢ - ȳ)²    [total sum of squares]
//   R² closer to 1 = better fit; R² = 0 = no relationship
//
// We also apply a short-term momentum correction using the
// recent Weighted Moving Average to adjust for recent trends.
// ============================================================

import com.trading.engine.model.StockData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PredictionService {

    // Number of recent days to use for regression (last 60 days)
    private static final int REGRESSION_WINDOW = 60;
    // How many future days to predict
    private static final int PREDICTION_DAYS   = 30;

    /**
     * Generates price predictions for the next 30 days using linear regression
     * with momentum adjustment from Weighted Moving Average.
     *
     * @param data Historical stock data (oldest first)
     * @return List of prediction maps, each containing date and predicted price
     */
    public List<Map<String, Object>> generatePredictions(List<StockData> data) {
        // Use only the most recent window for regression (more relevant)
        int startIdx = Math.max(0, data.size() - REGRESSION_WINDOW);
        List<StockData> window = data.subList(startIdx, data.size());

        // ---- Step 1: Prepare regression variables ----
        int n = window.size();
        double sumX   = 0, sumY   = 0;
        double sumXY  = 0, sumX2  = 0;

        // Build x (day index) and y (closing price) arrays
        double[] x = new double[n];
        double[] y = new double[n];

        for (int i = 0; i < n; i++) {
            x[i] = i + 1;                          // Day number (1, 2, 3, ...)
            y[i] = window.get(i).getClose();        // Closing price

            sumX  += x[i];
            sumY  += y[i];
            sumXY += x[i] * y[i];   // Σ(xᵢ × yᵢ)
            sumX2 += x[i] * x[i];   // Σ(xᵢ²)
        }

        // ---- Step 2: Calculate regression coefficients ----
        // β₁ (slope) = (n·Σxy - Σx·Σy) / (n·Σx² - (Σx)²)
        double beta1 = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        // β₀ (intercept) = (Σy - β₁·Σx) / n
        double beta0 = (sumY - beta1 * sumX) / n;

        // ---- Step 3: Calculate R² (goodness of fit) ----
        double meanY    = sumY / n;
        double ssTot    = 0, ssRes = 0;

        for (int i = 0; i < n; i++) {
            double predicted = beta0 + beta1 * x[i];
            ssTot += Math.pow(y[i] - meanY, 2);      // (yᵢ - ȳ)²
            ssRes += Math.pow(y[i] - predicted, 2);   // (yᵢ - ŷᵢ)²
        }

        double r2 = 1 - (ssTot == 0 ? 0 : ssRes / ssTot);

        // ---- Step 4: Apply momentum adjustment ----
        // Use Weighted Moving Average (WMA) of last 10 days to detect
        // short-term momentum and apply a correction factor
        double wmaAdjustment = calculateWMAMomentum(window);

        // ---- Step 5: Generate future predictions ----
        List<Map<String, Object>> predictions = new ArrayList<>();
        LocalDate lastDate = window.get(window.size() - 1).getDate();

        // Select specific future days: 1, 3, 7, 14, 21, 30
        int[] futureDays = {1, 3, 7, 14, 21, 30};

        for (int futureDay : futureDays) {
            // x value for this future day
            double futureX = n + futureDay;

            // Regression prediction (pure trend extrapolation)
            double regressionPrice = beta0 + beta1 * futureX;

            // Apply momentum decay: correction fades over time
            // Adjustment decays exponentially: adj × e^(-0.05 × days)
            double decayFactor     = Math.exp(-0.05 * futureDay);
            double adjustedPrice   = regressionPrice + (wmaAdjustment * decayFactor);

            // Ensure price doesn't go negative (floor at 1 cent)
            adjustedPrice = Math.max(adjustedPrice, 0.01);

            // Calculate confidence interval (±1 standard error)
            double stdError       = calculateStdError(x, y, beta0, beta1, n);
            double marginOfError  = stdError * (1 + 0.02 * futureDay); // Widens with time

            Map<String, Object> prediction = new HashMap<>();
            prediction.put("day",        futureDay);
            prediction.put("date",       lastDate.plusDays(futureDay).toString());
            prediction.put("price",      Math.round(adjustedPrice * 100.0) / 100.0);
            prediction.put("upperBound", Math.round((adjustedPrice + marginOfError) * 100.0) / 100.0);
            prediction.put("lowerBound", Math.round(Math.max(adjustedPrice - marginOfError, 0.01) * 100.0) / 100.0);
            prediction.put("r2",         Math.round(r2 * 1000.0) / 1000.0);

            predictions.add(prediction);
        }

        return predictions;
    }

    /**
     * Returns the predicted price for a specific number of days in the future.
     *
     * @param data   Historical data
     * @param days   Days into the future (e.g., 7 or 30)
     * @return Predicted closing price
     */
    public double predictPrice(List<StockData> data, int days) {
        List<Map<String, Object>> predictions = generatePredictions(data);
        return predictions.stream()
                .filter(p -> (int) p.get("day") == days)
                .findFirst()
                .map(p -> (double) p.get("price"))
                .orElse(data.get(data.size() - 1).getClose()); // Fallback to current
    }

    /**
     * Returns the R² coefficient from the regression model.
     * R² = 1 means perfect prediction; 0 means no predictive power.
     *
     * @param data Historical data
     * @return R² value (0.0 to 1.0)
     */
    public double getRSquared(List<StockData> data) {
        int startIdx = Math.max(0, data.size() - REGRESSION_WINDOW);
        List<StockData> window = data.subList(startIdx, data.size());

        int n = window.size();
        double sumX  = 0, sumY  = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double xi = i + 1;
            double yi = window.get(i).getClose();
            sumX  += xi; sumY  += yi;
            sumXY += xi * yi; sumX2 += xi * xi;
        }

        double beta1 = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double beta0 = (sumY - beta1 * sumX) / n;
        double meanY = sumY / n;
        double ssTot = 0, ssRes = 0;

        for (int i = 0; i < n; i++) {
            double yi = window.get(i).getClose();
            ssTot += Math.pow(yi - meanY, 2);
            ssRes += Math.pow(yi - (beta0 + beta1 * (i + 1)), 2);
        }

        return ssTot == 0 ? 0 : 1 - ssRes / ssTot;
    }

    /**
     * Calculates WMA-based momentum adjustment.
     * WMA gives more weight to recent prices:
     *   WMA = Σ(weight_i × price_i) / Σ(weight_i)
     *   where weight_i = i (recent = higher weight)
     *
     * Returns the deviation from the linear trend.
     */
    private double calculateWMAMomentum(List<StockData> data) {
        int window = Math.min(10, data.size());
        double wmaSum     = 0;
        double weightSum  = 0;

        for (int i = data.size() - window; i < data.size(); i++) {
            int weight    = i - (data.size() - window) + 1;  // 1, 2, ..., 10
            wmaSum       += data.get(i).getClose() * weight;
            weightSum    += weight;
        }

        double wma          = wmaSum / weightSum;
        double simpleAvg    = data.subList(data.size() - window, data.size())
                               .stream().mapToDouble(StockData::getClose).average().orElse(wma);

        return wma - simpleAvg;  // Positive = upward momentum, Negative = downward
    }

    /**
     * Calculates the standard error of the regression.
     * Used to build confidence intervals around predictions.
     *
     * SE = sqrt( Σ(yᵢ - ŷᵢ)² / (n-2) )
     */
    private double calculateStdError(double[] x, double[] y, double beta0, double beta1, int n) {
        double ssRes = 0;
        for (int i = 0; i < n; i++) {
            double predicted = beta0 + beta1 * x[i];
            ssRes += Math.pow(y[i] - predicted, 2);
        }
        return n > 2 ? Math.sqrt(ssRes / (n - 2)) : 0;
    }
}
