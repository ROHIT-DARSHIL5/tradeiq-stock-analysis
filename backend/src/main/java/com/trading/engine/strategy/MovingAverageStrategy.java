package com.trading.engine.strategy;

// ============================================================
// MovingAverageStrategy.java — EMA/SMA Crossover Strategy
//
// INHERITANCE DEMONSTRATION:
//   This class EXTENDS TradingStrategy, inheriting all its
//   protected fields, helper methods (calculateEMA, calculateSMA),
//   and the execute() template method.
//
// STRATEGY LOGIC:
//   Uses two Exponential Moving Averages (EMA):
//   - Short EMA (12-period): responds quickly to price changes
//   - Long EMA  (26-period): slower, shows overall trend
//
//   GOLDEN CROSS: Short EMA crosses ABOVE Long EMA → BUY signal
//   DEATH CROSS:  Short EMA crosses BELOW Long EMA → SELL signal
//
// FORMULA:
//   EMA(t) = Price(t) × [2/(n+1)] + EMA(t-1) × [1 - 2/(n+1)]
// ============================================================

import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Trend-following strategy based on EMA crossovers.
 * Works best in trending markets; generates false signals in sideways markets.
 *
 * @Component — Spring will auto-detect and register this as a bean.
 */
@Component
public class MovingAverageStrategy extends TradingStrategy {

    // Strategy parameters
    private static final int SHORT_PERIOD   = 12;   // Fast EMA (days)
    private static final int LONG_PERIOD    = 26;   // Slow EMA (days)
    private static final int SMA_200        = 200;  // Long-term trend filter

    /**
     * Constructor calls super() to initialize the abstract parent.
     * INHERITANCE: super() is required when parent has no default constructor.
     */
    public MovingAverageStrategy() {
        super(
            "EMA Crossover (12/26)",                            // name
            "Identifies trend reversals using short and long EMA crossovers. " +
            "Golden Cross = BUY, Death Cross = SELL.",          // description
            "TREND_FOLLOWING",                                   // type
            "MEDIUM_TERM",                                       // timeframe
            LONG_PERIOD + 5                                     // minimum data points needed
        );
    }

    /**
     * POLYMORPHISM: This method OVERRIDES the abstract analyze() from parent.
     * When called through a List<TradingStrategy>, Java's runtime dispatch
     * ensures THIS implementation runs, not any other strategy's.
     *
     * @param data Historical OHLCV data (oldest first)
     * @return StrategyResult with BUY/SELL/HOLD signal
     */
    @Override
    public StrategyResult analyze(List<StockData> data) {
        int n = data.size();

        // ---- Step 1: Calculate EMAs ----
        // Inherited from TradingStrategy (demonstrates code reuse via inheritance)
        double[] shortEMA = calculateEMA(data, SHORT_PERIOD);  // 12-day EMA array
        double[] longEMA  = calculateEMA(data, LONG_PERIOD);   // 26-day EMA array

        // ---- Step 2: Get current and previous values ----
        double currentShort  = shortEMA[n - 1];   // Today's 12-EMA
        double currentLong   = longEMA[n - 1];    // Today's 26-EMA
        double previousShort = shortEMA[n - 2];   // Yesterday's 12-EMA
        double previousLong  = longEMA[n - 2];    // Yesterday's 26-EMA

        // ---- Step 3: Calculate 200-day SMA for long-term trend ----
        double sma200 = -1;
        if (n >= SMA_200) {
            sma200 = calculateSMA(data, SMA_200, n - 1);
        }

        // ---- Step 4: Calculate MACD as difference between EMAs ----
        // This gives us the EMA spread (momentum strength)
        double emaDiff       = currentShort - currentLong;
        double prevEmaDiff   = previousShort - previousLong;
        double currentPrice  = data.get(n - 1).getClose();

        // ---- Step 5: Detect crossover ----
        boolean goldenCross  = previousShort <= previousLong && currentShort > currentLong;
        boolean deathCross   = previousShort >= previousLong && currentShort < currentLong;
        boolean bullishTrend = currentShort > currentLong;   // Short above long = bullish
        boolean aboveSMA200  = sma200 > 0 && currentPrice > sma200; // Above 200 = long bull

        // ---- Step 6: Calculate signal strength ----
        // Spread percentage = how far apart the two EMAs are
        double spreadPct = Math.abs(emaDiff) / currentLong * 100;

        // ---- Step 7: Generate signal ----
        String signal;
        int    confidence;
        String reasoning;

        if (goldenCross) {
            // Golden Cross just occurred — strong BUY
            signal     = StrategyResult.BUY;
            confidence = aboveSMA200 ? 85 : 70;
            reasoning  = String.format(
                "GOLDEN CROSS detected: 12-EMA (%.2f) crossed above 26-EMA (%.2f). " +
                "This is a classic BUY signal indicating a bullish trend reversal. " +
                "%s EMA spread: %.2f%%.",
                currentShort, currentLong,
                aboveSMA200 ? "Price is above 200-day SMA (strong uptrend confirmed)." : "",
                spreadPct
            );
        } else if (deathCross) {
            // Death Cross just occurred — strong SELL
            signal     = StrategyResult.SELL;
            confidence = !aboveSMA200 ? 85 : 70;
            reasoning  = String.format(
                "DEATH CROSS detected: 12-EMA (%.2f) crossed below 26-EMA (%.2f). " +
                "This is a classic SELL signal indicating a bearish trend reversal. " +
                "EMA spread: %.2f%%.",
                currentShort, currentLong, spreadPct
            );
        } else if (bullishTrend) {
            // No fresh crossover, but short above long → uptrend continuing
            signal     = StrategyResult.BUY;
            confidence = 50 + (int)(spreadPct * 3);
            confidence = Math.min(confidence, 75);
            reasoning  = String.format(
                "Bullish trend continuing: 12-EMA (%.2f) above 26-EMA (%.2f). " +
                "EMA spread: %.2f%%. Trend is %s.",
                currentShort, currentLong, spreadPct,
                aboveSMA200 ? "strongly confirmed by 200-SMA" : "in early stage"
            );
        } else {
            // Short below long → downtrend continuing
            signal     = StrategyResult.SELL;
            confidence = 50 + (int)(spreadPct * 3);
            confidence = Math.min(confidence, 75);
            reasoning  = String.format(
                "Bearish trend continuing: 12-EMA (%.2f) below 26-EMA (%.2f). " +
                "EMA spread: %.2f%%. Consider waiting for a Golden Cross before buying.",
                currentShort, currentLong, spreadPct
            );
        }

        // ---- Step 8: Build and return result using Builder Pattern ----
        return StrategyResult.builder()
                .signal(signal)
                .confidence(confidence)
                .reasoning(reasoning)
                .indicator("EMA_12",       round2(currentShort))
                .indicator("EMA_26",       round2(currentLong))
                .indicator("EMA_Spread",   round2(emaDiff))
                .indicator("EMA_Spread_Pct", round2(spreadPct))
                .indicator("SMA_200",      sma200 > 0 ? round2(sma200) : -1)
                .indicator("Price",        round2(currentPrice))
                .build();
    }
}
