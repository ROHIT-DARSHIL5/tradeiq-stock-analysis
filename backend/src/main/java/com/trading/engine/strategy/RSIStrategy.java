package com.trading.engine.strategy;

// ============================================================
// RSIStrategy.java — Relative Strength Index (RSI) Strategy
//
// INHERITANCE: extends TradingStrategy, inherits helpers
//
// WHAT IS RSI?
//   RSI is a momentum oscillator that measures the speed and
//   change of price movements. It oscillates between 0 and 100.
//
// FORMULA:
//   Step 1: Calculate daily price changes
//           Change(t) = Close(t) - Close(t-1)
//
//   Step 2: Separate gains and losses
//           If Change > 0: Gain = Change, Loss = 0
//           If Change < 0: Gain = 0,     Loss = |Change|
//
//   Step 3: Calculate Average Gain and Average Loss
//           AvgGain(first) = Sum of Gains over period / period
//           AvgGain(next)  = (Prev AvgGain × (period-1) + current Gain) / period
//                            [Wilder's smoothing method]
//
//   Step 4: Calculate Relative Strength (RS)
//           RS = AvgGain / AvgLoss
//
//   Step 5: Calculate RSI
//           RSI = 100 - (100 / (1 + RS))
//
// INTERPRETATION:
//   RSI > 70 → Overbought → SELL (price may reverse down)
//   RSI < 30 → Oversold  → BUY  (price may reverse up)
//   RSI 30-70 → Neutral  → HOLD
//
// Created by J. Welles Wilder Jr. in 1978.
// ============================================================

import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component  // Spring Component — registers this as a bean for auto-injection
public class RSIStrategy extends TradingStrategy {

    // RSI parameters — standard Wilder settings
    private static final int    RSI_PERIOD        = 14;   // Standard 14-day period
    private static final double OVERBOUGHT_LEVEL  = 70.0; // Sell threshold
    private static final double OVERSOLD_LEVEL    = 30.0; // Buy threshold
    private static final double EXTREME_OVERBOUGHT = 80.0;// Very strong sell
    private static final double EXTREME_OVERSOLD   = 20.0;// Very strong buy

    public RSIStrategy() {
        super(
            "RSI (14-period)",
            "Momentum oscillator measuring speed of price changes. " +
            "Oversold (<30) = BUY, Overbought (>70) = SELL.",
            "OSCILLATOR",
            "SHORT_TERM",
            RSI_PERIOD + 5
        );
    }

    @Override
    public StrategyResult analyze(List<StockData> data) {
        int n = data.size();

        // ---- Step 1: Calculate all RSI values ----
        double[] rsiValues = calculateRSI(data, RSI_PERIOD);

        // ---- Step 2: Get current, previous, and trend RSI values ----
        double currentRSI  = rsiValues[n - 1];
        double previousRSI = rsiValues[n - 2];
        double rsi5DaysAgo = n > 5 ? rsiValues[n - 6] : rsiValues[0];

        // ---- Step 3: Detect RSI divergence ----
        // Bullish divergence: Price making lower lows, RSI making higher lows
        // Bearish divergence: Price making higher highs, RSI making lower highs
        double currentPrice  = data.get(n - 1).getClose();
        double price5DaysAgo = data.get(n > 5 ? n - 6 : 0).getClose();
        boolean bullishDivergence = currentPrice < price5DaysAgo && currentRSI > rsi5DaysAgo;
        boolean bearishDivergence = currentPrice > price5DaysAgo && currentRSI < rsi5DaysAgo;

        // ---- Step 4: Detect RSI crossovers (crossing 50 = momentum shift) ----
        boolean crossedAbove50 = previousRSI < 50 && currentRSI >= 50;
        boolean crossedBelow50 = previousRSI > 50 && currentRSI <= 50;

        // ---- Step 5: Generate signal based on levels ----
        String signal;
        int    confidence;
        String reasoning;

        if (currentRSI <= EXTREME_OVERSOLD) {
            signal     = StrategyResult.BUY;
            confidence = 90;
            reasoning  = String.format(
                "RSI is EXTREMELY OVERSOLD at %.1f (below %s). " +
                "Stock is heavily oversold — strong potential for price bounce. " +
                "%s",
                currentRSI, EXTREME_OVERSOLD,
                bullishDivergence ? "BULLISH DIVERGENCE confirms reversal signal!" : ""
            );
        } else if (currentRSI <= OVERSOLD_LEVEL) {
            signal     = StrategyResult.BUY;
            confidence = 70 + (int)((OVERSOLD_LEVEL - currentRSI) * 1.5);
            reasoning  = String.format(
                "RSI is OVERSOLD at %.1f (below %s). " +
                "Indicates selling pressure is exhausted. Potential reversal upward. " +
                "RSI trend: %s (%.1f → %.1f).",
                currentRSI, OVERSOLD_LEVEL,
                currentRSI > previousRSI ? "recovering" : "still falling",
                previousRSI, currentRSI
            );
        } else if (currentRSI >= EXTREME_OVERBOUGHT) {
            signal     = StrategyResult.SELL;
            confidence = 90;
            reasoning  = String.format(
                "RSI is EXTREMELY OVERBOUGHT at %.1f (above %s). " +
                "Stock is heavily overbought — significant risk of price pullback. " +
                "%s",
                currentRSI, EXTREME_OVERBOUGHT,
                bearishDivergence ? "BEARISH DIVERGENCE confirms reversal risk!" : ""
            );
        } else if (currentRSI >= OVERBOUGHT_LEVEL) {
            signal     = StrategyResult.SELL;
            confidence = 70 + (int)((currentRSI - OVERBOUGHT_LEVEL) * 1.5);
            reasoning  = String.format(
                "RSI is OVERBOUGHT at %.1f (above %s). " +
                "Indicates buying pressure may be exhausted. Risk of pullback. " +
                "RSI trend: %s (%.1f → %.1f).",
                currentRSI, OVERBOUGHT_LEVEL,
                currentRSI < previousRSI ? "retreating" : "still rising",
                previousRSI, currentRSI
            );
        } else if (crossedAbove50) {
            signal     = StrategyResult.BUY;
            confidence = 60;
            reasoning  = String.format(
                "RSI crossed ABOVE 50 (now %.1f) — momentum turning BULLISH. " +
                "This mid-line crossover signals strengthening upward momentum.",
                currentRSI
            );
        } else if (crossedBelow50) {
            signal     = StrategyResult.SELL;
            confidence = 60;
            reasoning  = String.format(
                "RSI crossed BELOW 50 (now %.1f) — momentum turning BEARISH. " +
                "This mid-line crossover signals weakening buying pressure.",
                currentRSI
            );
        } else {
            signal     = StrategyResult.HOLD;
            confidence = 40;
            reasoning  = String.format(
                "RSI is NEUTRAL at %.1f (between %s and %s). " +
                "No strong overbought/oversold signal. Monitor for extreme levels.",
                currentRSI, OVERSOLD_LEVEL, OVERBOUGHT_LEVEL
            );
        }

        return StrategyResult.builder()
                .signal(signal)
                .confidence(confidence)
                .reasoning(reasoning)
                .indicator("RSI",          round2(currentRSI))
                .indicator("RSI_Prev",     round2(previousRSI))
                .indicator("Overbought",   OVERBOUGHT_LEVEL)
                .indicator("Oversold",     OVERSOLD_LEVEL)
                .build();
    }

    /**
     * Calculates RSI for all data points using Wilder's smoothing method.
     *
     * STEP-BY-STEP FORMULA:
     *   1. Gain(t) = max(Close(t) - Close(t-1), 0)
     *   2. Loss(t) = max(Close(t-1) - Close(t), 0)
     *   3. Initial AvgGain = average of first 14 gains
     *   4. Initial AvgLoss = average of first 14 losses
     *   5. Subsequent: AvgGain = (AvgGain × 13 + Gain) / 14
     *   6. RS = AvgGain / AvgLoss
     *   7. RSI = 100 - (100 / (1 + RS))
     *
     * @param data   Stock data list
     * @param period RSI period (standard = 14)
     * @return Array of RSI values (0-100 scale)
     */
    private double[] calculateRSI(List<StockData> data, int period) {
        double[] rsi     = new double[data.size()];
        double avgGain   = 0;
        double avgLoss   = 0;

        // Calculate initial average gain/loss (seed for Wilder smoothing)
        for (int i = 1; i <= period; i++) {
            double change = data.get(i).getClose() - data.get(i - 1).getClose();
            if (change > 0) avgGain += change;  // Positive change = gain
            else            avgLoss += Math.abs(change); // Negative change = loss
        }
        avgGain /= period;  // Average of first 'period' gains
        avgLoss /= period;  // Average of first 'period' losses

        // Calculate RSI for the seed period
        double rs = (avgLoss == 0) ? 100 : avgGain / avgLoss;
        rsi[period] = 100 - (100 / (1 + rs));

        // Wilder's smoothing for subsequent values
        for (int i = period + 1; i < data.size(); i++) {
            double change      = data.get(i).getClose() - data.get(i - 1).getClose();
            double currentGain = Math.max(change, 0);          // 0 if no gain
            double currentLoss = Math.max(-change, 0);         // 0 if no loss

            // Wilder's smoothing formula (exponential smoothing with period)
            avgGain = (avgGain * (period - 1) + currentGain) / period;
            avgLoss = (avgLoss * (period - 1) + currentLoss) / period;

            rs       = (avgLoss == 0) ? 100 : avgGain / avgLoss;
            rsi[i]   = 100 - (100 / (1 + rs));
        }

        return rsi;
    }
}
