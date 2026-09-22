package com.trading.engine.strategy;

// ============================================================
// StochasticStrategy.java — Stochastic Oscillator Strategy
//
// WHAT IS THE STOCHASTIC OSCILLATOR?
//   Developed by George Lane in the 1950s. It compares a
//   security's closing price to its price range over a given
//   period. The theory: in an uptrend, prices close near
//   highs; in a downtrend, prices close near lows.
//
// FORMULA:
//   %K = (Close − Lowest Low(n)) / (Highest High(n) − Lowest Low(n)) × 100
//
//   %D = SMA(3) of %K   [signal line — smoothed version]
//
//   where n = lookback period (typically 14)
//
// INTERPRETATION:
//   %K < 20 → Oversold  → BUY
//   %K > 80 → Overbought → SELL
//   %K crosses above %D → Bullish → BUY
//   %K crosses below %D → Bearish → SELL
//
// ============================================================

import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StochasticStrategy extends TradingStrategy {

    private static final int    K_PERIOD       = 14;   // %K lookback period
    private static final int    D_PERIOD       = 3;    // %D smoothing period
    private static final double OVERBOUGHT     = 80.0; // Overbought threshold
    private static final double OVERSOLD       = 20.0; // Oversold threshold

    public StochasticStrategy() {
        super(
            "Stochastic Oscillator (14,3)",
            "Momentum indicator comparing closing price to recent price range. " +
            "Oversold (<20) = BUY, Overbought (>80) = SELL. %K/%D crossovers.",
            "OSCILLATOR",
            "SHORT_TERM",
            K_PERIOD + D_PERIOD + 3
        );
    }

    @Override
    public StrategyResult analyze(List<StockData> data) {
        int n = data.size();

        // ---- Step 1: Calculate %K for all data points ----
        double[] kValues = calculateK(data, K_PERIOD);

        // ---- Step 2: Calculate %D = SMA(3) of %K ----
        double[] dValues = calculateD(kValues, D_PERIOD);

        // ---- Step 3: Get current and previous values ----
        double currentK  = kValues[n - 1];
        double currentD  = dValues[n - 1];
        double previousK = kValues[n - 2];
        double previousD = dValues[n - 2];

        // ---- Step 4: Detect crossovers ----
        boolean kCrossedAboveD = previousK <= previousD && currentK > currentD;
        boolean kCrossedBelowD = previousK >= previousD && currentK < currentD;

        // ---- Step 5: Zone classification ----
        boolean oversold   = currentK < OVERSOLD;
        boolean overbought = currentK > OVERBOUGHT;

        // ---- Step 6: Generate signal ----
        String signal;
        int    confidence;
        String reasoning;

        if (oversold && kCrossedAboveD) {
            // Strongest buy signal: oversold + bullish crossover
            signal     = StrategyResult.BUY;
            confidence = 88;
            reasoning  = String.format(
                "STRONG BUY: %%K (%.1f) crossed above %%D (%.1f) in OVERSOLD zone (<%.0f). " +
                "This dual confirmation (oversold + crossover) is a high-probability reversal signal.",
                currentK, currentD, OVERSOLD
            );
        } else if (overbought && kCrossedBelowD) {
            // Strongest sell signal: overbought + bearish crossover
            signal     = StrategyResult.SELL;
            confidence = 88;
            reasoning  = String.format(
                "STRONG SELL: %%K (%.1f) crossed below %%D (%.1f) in OVERBOUGHT zone (>%.0f). " +
                "Dual confirmation of bearish reversal.",
                currentK, currentD, OVERBOUGHT
            );
        } else if (oversold) {
            signal     = StrategyResult.BUY;
            confidence = 65;
            reasoning  = String.format(
                "%%K (%.1f) in OVERSOLD territory (<%.0f). Price momentum is extremely weak. " +
                "Wait for %%K to cross above %%D (%.1f) for confirmation before buying.",
                currentK, OVERSOLD, currentD
            );
        } else if (overbought) {
            signal     = StrategyResult.SELL;
            confidence = 65;
            reasoning  = String.format(
                "%%K (%.1f) in OVERBOUGHT territory (>%.0f). Momentum may be exhausted. " +
                "Consider selling if %%K crosses below %%D (%.1f).",
                currentK, OVERBOUGHT, currentD
            );
        } else if (kCrossedAboveD) {
            signal     = StrategyResult.BUY;
            confidence = 60;
            reasoning  = String.format(
                "%%K (%.1f) crossed above %%D (%.1f) — bullish momentum shift. " +
                "Not in oversold zone, so moderate confidence.",
                currentK, currentD
            );
        } else if (kCrossedBelowD) {
            signal     = StrategyResult.SELL;
            confidence = 60;
            reasoning  = String.format(
                "%%K (%.1f) crossed below %%D (%.1f) — bearish momentum shift.",
                currentK, currentD
            );
        } else {
            signal     = StrategyResult.HOLD;
            confidence = 40;
            reasoning  = String.format(
                "%%K (%.1f) is neutral (between %.0f and %.0f). %%D = %.1f. " +
                "No strong signal. Monitor for entry into extreme zones.",
                currentK, OVERSOLD, OVERBOUGHT, currentD
            );
        }

        return StrategyResult.builder()
                .signal(signal)
                .confidence(confidence)
                .reasoning(reasoning)
                .indicator("Stoch_K",     round2(currentK))
                .indicator("Stoch_D",     round2(currentD))
                .indicator("Overbought",  OVERBOUGHT)
                .indicator("Oversold",    OVERSOLD)
                .build();
    }

    /**
     * Calculates Stochastic %K for all data points.
     *
     * %K = (Close − Lowest Low over period) / (Highest High − Lowest Low) × 100
     *
     * @param data   Stock price data
     * @param period Lookback period (14 days standard)
     * @return Array of %K values (0-100 scale)
     */
    private double[] calculateK(List<StockData> data, int period) {
        double[] k = new double[data.size()];

        for (int i = period - 1; i < data.size(); i++) {
            double highest = Double.MIN_VALUE;  // Highest high in period
            double lowest  = Double.MAX_VALUE;  // Lowest low in period

            // Find highest high and lowest low in the lookback window
            for (int j = i - period + 1; j <= i; j++) {
                highest = Math.max(highest, data.get(j).getHigh());
                lowest  = Math.min(lowest,  data.get(j).getLow());
            }

            double range = highest - lowest;
            if (range == 0) {
                k[i] = 50; // No range → neutral
            } else {
                // %K formula: how close is close to the range's top?
                k[i] = (data.get(i).getClose() - lowest) / range * 100;
            }
        }
        return k;
    }

    /**
     * Calculates %D = SMA of %K over dPeriod days.
     * %D is the "slow" stochastic — a smoothed version of %K.
     *
     * @param kValues Array of %K values
     * @param dPeriod Smoothing period (typically 3)
     * @return Array of %D values
     */
    private double[] calculateD(double[] kValues, int dPeriod) {
        double[] d = new double[kValues.length];
        for (int i = dPeriod - 1; i < kValues.length; i++) {
            double sum = 0;
            for (int j = i - dPeriod + 1; j <= i; j++) {
                sum += kValues[j];
            }
            d[i] = sum / dPeriod;
        }
        return d;
    }
}
