package com.trading.engine.strategy;

// ============================================================
// BollingerBandsStrategy.java — Bollinger Bands Volatility Strategy
//
// WHAT ARE BOLLINGER BANDS?
//   Developed by John Bollinger in the 1980s. Three bands that
//   adapt to volatility — they widen during volatile periods
//   and narrow during calm periods.
//
// FORMULA:
//   Middle Band  = SMA(20)                    [20-day simple moving average]
//   Upper Band   = SMA(20) + (2 × σ)         [σ = standard deviation]
//   Lower Band   = SMA(20) − (2 × σ)
//
//   %B Indicator = (Price − Lower Band) / (Upper Band − Lower Band)
//                  0 = at lower band, 1 = at upper band, 0.5 = at middle
//
//   Bandwidth    = (Upper − Lower) / Middle × 100
//                  Low bandwidth = low volatility = potential breakout coming
//
// TRADING SIGNALS:
//   Price touches lower band → Oversold → BUY
//   Price touches upper band → Overbought → SELL
//   Bandwidth squeeze followed by expansion → Breakout signal
//
// ============================================================

import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BollingerBandsStrategy extends TradingStrategy {

    private static final int    PERIOD     = 20;   // Standard 20-day period
    private static final double MULTIPLIER = 2.0;  // Standard 2 standard deviations

    public BollingerBandsStrategy() {
        super(
            "Bollinger Bands (20,2)",
            "Volatility-based strategy. Price near lower band = BUY, " +
            "price near upper band = SELL. Squeeze signals breakout.",
            "VOLATILITY",
            "SHORT_TERM",
            PERIOD + 10
        );
    }

    @Override
    public StrategyResult analyze(List<StockData> data) {
        int n = data.size();

        // ---- Step 1: Calculate current Bollinger Bands ----
        double sma    = calculateSMA(data, PERIOD, n - 1);   // Middle Band (inherited)
        double stdDev = calculateStdDev(data, sma, n - PERIOD, n - 1); // σ (inherited)

        double upperBand = sma + (MULTIPLIER * stdDev);   // Upper Band = SMA + 2σ
        double lowerBand = sma - (MULTIPLIER * stdDev);   // Lower Band = SMA - 2σ
        double currentPrice = data.get(n - 1).getClose();

        // ---- Step 2: Calculate %B ----
        // %B = (Price − Lower) / (Upper − Lower)
        // Tells us WHERE in the band the price currently sits
        double percentB   = (currentPrice - lowerBand) / (upperBand - lowerBand);
        double bandwidth  = (upperBand - lowerBand) / sma * 100; // % bandwidth

        // ---- Step 3: Calculate previous bands for squeeze detection ----
        double prevSMA    = calculateSMA(data, PERIOD, n - 2);
        double prevStdDev = calculateStdDev(data, prevSMA, n - PERIOD - 1, n - 2);
        double prevBandwidth = (prevSMA + 2*prevStdDev - (prevSMA - 2*prevStdDev)) / prevSMA * 100;

        // ---- Step 4: Check for Bollinger Squeeze ----
        // Squeeze = bandwidth < 4% for 3+ consecutive periods (very low volatility)
        // After a squeeze, expect a significant price move (breakout)
        boolean squeeze = bandwidth < 4.0;

        // ---- Step 5: Check price vs bands with multiple zones ----
        // Zone classification: where is price relative to the bands?
        boolean aboveUpper   = currentPrice > upperBand;
        boolean nearUpper    = percentB > 0.9 && currentPrice <= upperBand;
        boolean nearLower    = percentB < 0.1 && currentPrice >= lowerBand;
        boolean belowLower   = currentPrice < lowerBand;
        boolean midUpper     = percentB > 0.5;

        // ---- Step 6: Check if bands are expanding (breakout momentum) ----
        boolean expanding    = bandwidth > prevBandwidth;

        // ---- Step 7: Generate signal ----
        String signal;
        int    confidence;
        String reasoning;

        if (belowLower) {
            signal     = StrategyResult.BUY;
            confidence = 85;
            reasoning  = String.format(
                "Price (%.2f) BELOW LOWER BAND (%.2f) — extreme oversold condition. " +
                "%%B = %.3f. This is a strong mean-reversion BUY signal. " +
                "Price is statistically likely to revert toward the middle band (%.2f).",
                currentPrice, lowerBand, percentB, sma
            );
        } else if (nearLower) {
            signal     = StrategyResult.BUY;
            confidence = 72;
            reasoning  = String.format(
                "Price (%.2f) TOUCHING LOWER BAND (%.2f) — oversold. " +
                "%%B = %.3f (near 0 = at lower band). " +
                "Bollinger Bands suggest price is stretched to the downside. " +
                "Bandwidth: %.1f%%.",
                currentPrice, lowerBand, percentB, bandwidth
            );
        } else if (aboveUpper) {
            signal     = StrategyResult.SELL;
            confidence = 85;
            reasoning  = String.format(
                "Price (%.2f) ABOVE UPPER BAND (%.2f) — extreme overbought condition. " +
                "%%B = %.3f. Strong mean-reversion SELL signal. " +
                "Price is statistically likely to revert toward middle band (%.2f).",
                currentPrice, upperBand, percentB, sma
            );
        } else if (nearUpper) {
            signal     = StrategyResult.SELL;
            confidence = 72;
            reasoning  = String.format(
                "Price (%.2f) TOUCHING UPPER BAND (%.2f) — overbought. " +
                "%%B = %.3f (near 1 = at upper band). " +
                "Risk of pullback. Bandwidth: %.1f%%.",
                currentPrice, upperBand, percentB, bandwidth
            );
        } else if (squeeze) {
            signal     = StrategyResult.HOLD;
            confidence = 55;
            reasoning  = String.format(
                "BOLLINGER SQUEEZE detected! Bandwidth is very low: %.1f%%. " +
                "Low volatility period — a significant breakout is imminent. " +
                "Direction unclear; wait for price to break above (%.2f) or below (%.2f).",
                bandwidth, upperBand, lowerBand
            );
        } else if (midUpper) {
            signal     = StrategyResult.HOLD;
            confidence = 50;
            reasoning  = String.format(
                "Price (%.2f) in upper half of bands. %%B = %.3f. " +
                "Mild bullish bias but no extreme level reached. Bandwidth: %.1f%%.",
                currentPrice, percentB, bandwidth
            );
        } else {
            signal     = StrategyResult.HOLD;
            confidence = 45;
            reasoning  = String.format(
                "Price (%.2f) in lower half of bands but not oversold. %%B = %.3f. " +
                "Wait for price to reach band extremes for higher-confidence signals.",
                currentPrice, percentB
            );
        }

        return StrategyResult.builder()
                .signal(signal)
                .confidence(confidence)
                .reasoning(reasoning)
                .indicator("Upper_Band",  round2(upperBand))
                .indicator("Middle_Band", round2(sma))
                .indicator("Lower_Band",  round2(lowerBand))
                .indicator("Percent_B",   round2(percentB * 100) / 100.0)
                .indicator("Bandwidth",   round2(bandwidth))
                .indicator("Std_Dev",     round2(stdDev))
                .build();
    }
}
