package com.trading.engine.strategy;

// ============================================================
// TradingStrategy.java — Abstract Base Class
//
// ABSTRACTION DEMONSTRATION:
//   This abstract class defines WHAT every strategy must do
//   (the contract) without specifying HOW it is done.
//   The 'analyze()' method is abstract — subclasses MUST
//   provide their own implementation.
//
// TEMPLATE METHOD PATTERN:
//   execute() is a template method — it defines the algorithm
//   skeleton (validate → analyze → enrich) while letting
//   subclasses fill in the 'analyze()' step.
//
// INHERITANCE DEMONSTRATION:
//   MovingAverageStrategy, RSIStrategy, MACDStrategy,
//   BollingerBandsStrategy, StochasticStrategy all EXTEND
//   this class and inherit its concrete methods.
// ============================================================

import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;

import java.util.List;

/**
 * Abstract base class for all trading strategies.
 * Every strategy inherits the validation and execution framework
 * but provides its own signal calculation logic.
 */
public abstract class TradingStrategy {

    // ---- Protected Fields (accessible to subclasses) ----
    // 'protected' = accessible within this class AND all subclasses
    // but NOT from external classes. This is a form of encapsulation.

    protected final String name;         // Strategy display name
    protected final String description;  // Brief explanation
    protected final String type;         // TREND_FOLLOWING | OSCILLATOR | VOLATILITY | MOMENTUM
    protected final String timeframe;    // SHORT_TERM | MEDIUM_TERM | LONG_TERM
    protected int          minDataPoints;// Minimum data points required

    // ---- Constructor ----
    /**
     * Protected constructor — only subclasses can call it via super().
     * This forces all strategies to provide name, description, type, timeframe.
     */
    protected TradingStrategy(String name, String description,
                               String type, String timeframe, int minDataPoints) {
        this.name          = name;
        this.description   = description;
        this.type          = type;
        this.timeframe     = timeframe;
        this.minDataPoints = minDataPoints;
    }

    // ============================================================
    // ABSTRACT METHOD — The core of Abstraction
    //
    // This declares a method that MUST be overridden by subclasses.
    // We know every strategy must analyze data, but each strategy
    // does it differently (MA uses averages, RSI uses gains/losses).
    //
    // This is the CONTRACT that all strategies must fulfill.
    // ============================================================
    /**
     * Analyzes stock data and produces a trading signal.
     * Each subclass implements its own indicator calculations.
     *
     * @param data List of historical OHLCV data (newest last)
     * @return StrategyResult containing signal, confidence, indicators
     */
    public abstract StrategyResult analyze(List<StockData> data);

    // ============================================================
    // TEMPLATE METHOD — Concrete method in abstract class
    //
    // This defines the SKELETON of the execution algorithm.
    // Subclasses cannot override this (we could make it final).
    // The step-by-step process is fixed; only 'analyze()' varies.
    // ============================================================
    /**
     * Template method that orchestrates the full analysis pipeline:
     * 1. Validate input data has enough points
     * 2. Call the subclass-specific analyze() method
     * 3. Enrich the result with strategy metadata
     *
     * @param data Historical stock data
     * @return Enriched StrategyResult with metadata
     */
    public final StrategyResult execute(List<StockData> data) {
        // Step 1: Validate
        if (data == null || data.size() < minDataPoints) {
            return StrategyResult.builder()
                    .strategyName(name)
                    .strategyType(type)
                    .signal(StrategyResult.HOLD)
                    .confidence(0)
                    .reasoning("Insufficient data: need at least " + minDataPoints +
                               " data points, got " + (data == null ? 0 : data.size()))
                    .timeframe(timeframe)
                    .build();
        }

        // Step 2: Delegate to subclass-specific analysis (Polymorphism!)
        StrategyResult result = analyze(data);

        // Step 3: Enrich with metadata (common to all strategies)
        result.setStrategyName(name);
        result.setStrategyType(type);
        result.setTimeframe(timeframe);

        return result;
    }

    // ---- Helper Methods (available to all subclasses) ----
    // These are shared utility methods that avoid code duplication.

    /**
     * Calculates the Simple Moving Average (SMA) of closing prices.
     *
     * FORMULA: SMA(n) = (P₁ + P₂ + ... + Pₙ) / n
     * where P = closing price, n = period
     *
     * @param data   Price data list
     * @param period Number of periods to average
     * @param endIdx Index of the last element to include
     * @return SMA value, or -1 if insufficient data
     */
    protected double calculateSMA(List<StockData> data, int period, int endIdx) {
        if (endIdx < period - 1) return -1;  // Not enough data
        double sum = 0;
        for (int i = endIdx - period + 1; i <= endIdx; i++) {
            sum += data.get(i).getClose();
        }
        return sum / period;
    }

    /**
     * Calculates the Exponential Moving Average (EMA).
     *
     * FORMULA:
     *   EMA(today) = Price(today) × k + EMA(yesterday) × (1 − k)
     *   where k = 2 / (period + 1)   [smoothing factor]
     *
     * EMA gives MORE weight to recent prices than SMA.
     * This makes it more responsive to new information.
     *
     * @param data   Price data list
     * @param period EMA period (e.g., 12 or 26)
     * @return Array of EMA values corresponding to each data point
     */
    protected double[] calculateEMA(List<StockData> data, int period) {
        double[] ema = new double[data.size()];
        double k = 2.0 / (period + 1);  // Smoothing factor

        // Seed the first EMA value with SMA of first 'period' values
        double seedSMA = 0;
        for (int i = 0; i < period; i++) {
            seedSMA += data.get(i).getClose();
        }
        ema[period - 1] = seedSMA / period;

        // Calculate EMA for remaining periods using the formula
        for (int i = period; i < data.size(); i++) {
            ema[i] = data.get(i).getClose() * k + ema[i - 1] * (1 - k);
        }

        return ema;
    }

    /**
     * Calculates standard deviation of closing prices.
     *
     * FORMULA:
     *   σ = sqrt( Σ(Pᵢ - μ)² / n )
     *   where μ = mean, n = number of periods
     *
     * @param data   Price data
     * @param mean   Pre-calculated mean of the data
     * @param start  Start index
     * @param end    End index (inclusive)
     * @return Standard deviation (σ)
     */
    protected double calculateStdDev(List<StockData> data, double mean, int start, int end) {
        double sumSquaredDiff = 0;
        int n = end - start + 1;
        for (int i = start; i <= end; i++) {
            double diff = data.get(i).getClose() - mean;
            sumSquaredDiff += diff * diff;  // (Pᵢ - μ)²
        }
        return Math.sqrt(sumSquaredDiff / n);
    }

    /**
     * Rounds a double to 2 decimal places (for price display).
     */
    protected double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ---- Getters ----
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getType()        { return type; }
    public String getTimeframe()   { return timeframe; }
    public int    getMinDataPoints(){ return minDataPoints; }

    @Override
    public String toString() {
        return String.format("TradingStrategy{name='%s', type='%s', timeframe='%s'}",
                name, type, timeframe);
    }
}
