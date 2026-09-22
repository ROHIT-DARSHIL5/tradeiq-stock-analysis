package com.trading.engine.model;

// ============================================================
// StrategyResult.java — Strategy Analysis Output Model
//
// Encapsulates the result produced by ONE trading strategy.
// Contains the signal (BUY/SELL/HOLD), confidence level,
// reasoning text, and all calculated indicator values.
//
// DESIGN PATTERN: Builder Pattern
//   Instead of a constructor with 8 parameters, we use a Builder
//   that allows fluent, readable construction:
//     StrategyResult.builder()
//       .signal("BUY")
//       .confidence(78)
//       .build();
// ============================================================

import java.util.HashMap;
import java.util.Map;

public class StrategyResult {

    // ---- Signal Constants (prevents typos) ----
    public static final String BUY  = "BUY";
    public static final String SELL = "SELL";
    public static final String HOLD = "HOLD";

    // ---- Private Fields (ENCAPSULATION) ----
    private String strategyName;       // e.g., "Moving Average Crossover"
    private String strategyType;       // e.g., "TREND_FOLLOWING"
    private String signal;             // BUY | SELL | HOLD
    private int    confidence;         // 0-100 percent
    private String reasoning;          // Human-readable explanation
    private Map<String, Double> indicators; // Calculated indicator values
    private String timeframe;          // "SHORT_TERM" | "MEDIUM_TERM" | "LONG_TERM"

    // ---- Private Constructor (enforces Builder usage) ----
    private StrategyResult() {
        this.indicators = new HashMap<>();
    }

    // ---- Static Factory Method ----
    /** Returns a new Builder instance for fluent construction */
    public static Builder builder() {
        return new Builder();
    }

    // ====================================================
    // BUILDER PATTERN — Inner static class
    // Allows step-by-step construction of StrategyResult.
    // Each setter returns 'this' for method chaining.
    // ====================================================
    public static class Builder {
        private final StrategyResult result;

        private Builder() {
            result = new StrategyResult();
        }

        public Builder strategyName(String name) {
            result.strategyName = name;
            return this;
        }

        public Builder strategyType(String type) {
            result.strategyType = type;
            return this;
        }

        public Builder signal(String signal) {
            result.signal = signal;
            return this;
        }

        public Builder confidence(int confidence) {
            // Validate range: confidence must be 0-100
            result.confidence = Math.max(0, Math.min(100, confidence));
            return this;
        }

        public Builder reasoning(String reasoning) {
            result.reasoning = reasoning;
            return this;
        }

        public Builder indicator(String name, double value) {
            result.indicators.put(name, value);
            return this;
        }

        public Builder timeframe(String timeframe) {
            result.timeframe = timeframe;
            return this;
        }

        /** Finalizes and returns the constructed StrategyResult */
        public StrategyResult build() {
            if (result.signal == null) result.signal = HOLD;
            if (result.strategyName == null) result.strategyName = "Unknown";
            return result;
        }
    }

    // ---- Getters ----
    public String              getStrategyName() { return strategyName; }
    public String              getStrategyType() { return strategyType; }
    public String              getSignal()        { return signal; }
    public int                 getConfidence()    { return confidence; }
    public String              getReasoning()     { return reasoning; }
    public Map<String, Double> getIndicators()   { return indicators; }
    public String              getTimeframe()     { return timeframe; }

    // ---- Setters (needed for Jackson) ----
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public void setSignal(String signal)             { this.signal = signal; }
    public void setConfidence(int confidence)        { this.confidence = confidence; }
    public void setReasoning(String reasoning)       { this.reasoning = reasoning; }
    public void setIndicators(Map<String, Double> i) { this.indicators = i; }
    public void setTimeframe(String timeframe)       { this.timeframe = timeframe; }

    /** Returns true if this result recommends buying */
    public boolean isBullish() { return BUY.equals(signal); }

    /** Returns true if this result recommends selling */
    public boolean isBearish() { return SELL.equals(signal); }

    @Override
    public String toString() {
        return String.format("StrategyResult{strategy='%s', signal='%s', confidence=%d%%}",
                strategyName, signal, confidence);
    }
}
