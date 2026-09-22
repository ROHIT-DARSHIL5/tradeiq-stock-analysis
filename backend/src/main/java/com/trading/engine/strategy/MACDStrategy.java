package com.trading.engine.strategy;

// ============================================================
// MACDStrategy.java — MACD (Moving Average Convergence/Divergence)
//
// WHAT IS MACD?
//   MACD is a trend-following momentum indicator that shows
//   the relationship between two EMAs of a security's price.
//   Developed by Gerald Appel in the late 1970s.
//
// THREE COMPONENTS:
//   1. MACD Line    = EMA(12) − EMA(26)
//                   Measures momentum; positive = bullish
//
//   2. Signal Line  = EMA(9) of the MACD Line
//                   A smoothed trigger line for MACD crossovers
//
//   3. Histogram    = MACD Line − Signal Line
//                   Visual representation of the difference;
//                   positive bars = bullish momentum
//
// TRADING SIGNALS:
//   BULLISH CROSSOVER: MACD crosses above Signal → BUY
//   BEARISH CROSSOVER: MACD crosses below Signal → SELL
//   ZERO LINE CROSS:   MACD crosses zero → trend change
//   DIVERGENCE:        Price and MACD moving opposite = reversal
//
// FORMULA SUMMARY:
//   MACD = EMA₁₂(Close) − EMA₂₆(Close)
//   Signal = EMA₉(MACD)
//   Histogram = MACD − Signal
// ============================================================

import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MACDStrategy extends TradingStrategy {

    // Standard MACD parameters (12, 26, 9) — industry default
    private static final int FAST_PERIOD   = 12;   // Fast EMA period
    private static final int SLOW_PERIOD   = 26;   // Slow EMA period
    private static final int SIGNAL_PERIOD = 9;    // Signal line EMA period

    public MACDStrategy() {
        super(
            "MACD (12,26,9)",
            "Trend momentum indicator using difference of EMAs. " +
            "Signal line crossovers generate BUY/SELL signals.",
            "TREND_FOLLOWING",
            "MEDIUM_TERM",
            SLOW_PERIOD + SIGNAL_PERIOD + 5
        );
    }

    @Override
    public StrategyResult analyze(List<StockData> data) {
        int n = data.size();

        // ---- Step 1: Calculate the two EMAs ----
        double[] ema12 = calculateEMA(data, FAST_PERIOD);  // 12-day EMA
        double[] ema26 = calculateEMA(data, SLOW_PERIOD);  // 26-day EMA

        // ---- Step 2: Calculate MACD line ----
        // MACD(t) = EMA12(t) − EMA26(t)
        // We start from index SLOW_PERIOD-1 (first valid 26-EMA)
        List<Double> macdValues = new ArrayList<>();
        for (int i = SLOW_PERIOD - 1; i < n; i++) {
            macdValues.add(ema12[i] - ema26[i]);  // MACD = Fast EMA − Slow EMA
        }

        // ---- Step 3: Calculate Signal line (EMA of MACD line) ----
        double[] signalValues = calculateEMAFromList(macdValues, SIGNAL_PERIOD);

        // ---- Step 4: Calculate Histogram ----
        // Histogram = MACD − Signal (shows strength of trend)
        int signalStart = SIGNAL_PERIOD - 1;
        double currentMACD      = macdValues.get(macdValues.size() - 1);
        double prevMACD         = macdValues.get(macdValues.size() - 2);
        double currentSignal    = signalValues[signalValues.length - 1];
        double prevSignal       = signalValues[signalValues.length - 2];
        double currentHistogram = currentMACD - currentSignal;
        double prevHistogram    = prevMACD - prevSignal;

        // ---- Step 5: Detect crossovers ----
        boolean bullishCrossover = prevMACD <= prevSignal && currentMACD > currentSignal;
        boolean bearishCrossover = prevMACD >= prevSignal && currentMACD < currentSignal;

        // ---- Step 6: Check zero-line crosses ----
        boolean crossedAboveZero = prevMACD < 0 && currentMACD >= 0;
        boolean crossedBelowZero = prevMACD > 0 && currentMACD <= 0;

        // ---- Step 7: Histogram trend (3 bars) ----
        double thirdLastHist = macdValues.size() > 3
            ? macdValues.get(macdValues.size() - 3) - signalValues[signalValues.length - 3]
            : prevHistogram;
        boolean histogramIncreasing = currentHistogram > prevHistogram && prevHistogram > thirdLastHist;
        boolean histogramDecreasing = currentHistogram < prevHistogram && prevHistogram < thirdLastHist;

        // ---- Step 8: Generate signal ----
        String signal;
        int    confidence;
        String reasoning;

        if (bullishCrossover) {
            signal     = StrategyResult.BUY;
            confidence = currentMACD < 0 ? 75 : 65; // Crossover below zero is stronger
            reasoning  = String.format(
                "BULLISH CROSSOVER: MACD (%.4f) crossed above Signal (%.4f). " +
                "This is a high-confidence BUY signal. " +
                "Histogram turned positive: %.4f. %s",
                currentMACD, currentSignal, currentHistogram,
                currentMACD < 0 ? "Signal occurred below zero line — extra bullish!" :
                                  "Signal above zero line confirms existing uptrend."
            );
        } else if (bearishCrossover) {
            signal     = StrategyResult.SELL;
            confidence = currentMACD > 0 ? 75 : 65;
            reasoning  = String.format(
                "BEARISH CROSSOVER: MACD (%.4f) crossed below Signal (%.4f). " +
                "This is a high-confidence SELL signal. " +
                "Histogram turned negative: %.4f. %s",
                currentMACD, currentSignal, currentHistogram,
                currentMACD > 0 ? "Signal occurred above zero line — extra bearish!" :
                                  "Signal below zero line confirms existing downtrend."
            );
        } else if (crossedAboveZero) {
            signal     = StrategyResult.BUY;
            confidence = 70;
            reasoning  = String.format(
                "MACD crossed ABOVE ZERO LINE (now %.4f). " +
                "This confirms the start of a new bullish trend. " +
                "Momentum is turning positive.",
                currentMACD
            );
        } else if (crossedBelowZero) {
            signal     = StrategyResult.SELL;
            confidence = 70;
            reasoning  = String.format(
                "MACD crossed BELOW ZERO LINE (now %.4f). " +
                "This confirms the start of a new bearish trend. " +
                "Momentum is turning negative.",
                currentMACD
            );
        } else if (currentMACD > currentSignal) {
            signal     = StrategyResult.BUY;
            confidence = 50 + (histogramIncreasing ? 15 : 0);
            reasoning  = String.format(
                "MACD (%.4f) is above Signal (%.4f) — bullish bias. " +
                "Histogram: %.4f. Trend: %s.",
                currentMACD, currentSignal, currentHistogram,
                histogramIncreasing ? "STRENGTHENING (histogram expanding)" :
                                      "weakening (histogram contracting)"
            );
        } else {
            signal     = StrategyResult.SELL;
            confidence = 50 + (histogramDecreasing ? 15 : 0);
            reasoning  = String.format(
                "MACD (%.4f) is below Signal (%.4f) — bearish bias. " +
                "Histogram: %.4f. Trend: %s.",
                currentMACD, currentSignal, currentHistogram,
                histogramDecreasing ? "WORSENING (histogram expanding negatively)" :
                                      "improving (histogram contracting)"
            );
        }

        return StrategyResult.builder()
                .signal(signal)
                .confidence(confidence)
                .reasoning(reasoning)
                .indicator("MACD",         round2(currentMACD * 100) / 100.0)
                .indicator("Signal",       round2(currentSignal * 100) / 100.0)
                .indicator("Histogram",    round2(currentHistogram * 100) / 100.0)
                .indicator("EMA_12",       round2(ema12[n-1]))
                .indicator("EMA_26",       round2(ema26[n-1]))
                .build();
    }

    /**
     * Calculates EMA from a List<Double> (needed for Signal line calculation).
     * The parent class calculateEMA() works with List<StockData>, but we need
     * to compute EMA of MACD values (doubles), so we override it here.
     *
     * This is an example of method overloading — same concept, different param types.
     *
     * @param values List of double values to calculate EMA for
     * @param period EMA period
     * @return EMA array
     */
    private double[] calculateEMAFromList(List<Double> values, int period) {
        double[] ema = new double[values.size()];
        double k = 2.0 / (period + 1);

        // Seed with SMA of first 'period' values
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += values.get(i);
        }
        ema[period - 1] = sum / period;

        // Apply EMA formula for subsequent values
        for (int i = period; i < values.size(); i++) {
            ema[i] = values.get(i) * k + ema[i - 1] * (1 - k);
        }

        return ema;
    }
}
