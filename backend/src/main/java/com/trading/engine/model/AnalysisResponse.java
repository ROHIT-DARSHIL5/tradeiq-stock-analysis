package com.trading.engine.model;

// ============================================================
// AnalysisResponse.java — Complete API Response Model
//
// This is the top-level object returned by the /api/analyze
// endpoint. It aggregates all strategy results, price history,
// prediction data, and the final overall recommendation.
// ============================================================

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AnalysisResponse {

    // ---- Stock Metadata ----
    private String symbol;             // Ticker symbol e.g. "AAPL"
    private String companyName;        // Full name e.g. "Apple Inc."
    private double currentPrice;       // Latest closing price
    private double priceChange;        // Absolute change vs previous close
    private double priceChangePct;     // Percentage change
    private double weekHigh52;         // 52-week high price
    private double weekLow52;          // 52-week low price
    private long   avgVolume;          // Average daily volume
    private String currency;           // "USD"
    private String exchange;           // "NASDAQ"

    // ---- Historical Price Data ----
    private List<StockData> priceHistory;   // OHLCV data for the analysis period

    // ---- Strategy Analysis Results ----
    private List<StrategyResult> strategyResults;  // One result per strategy

    // ---- Overall Recommendation ----
    private String overallSignal;       // Aggregated BUY/SELL/HOLD
    private int    overallConfidence;   // Weighted average confidence
    private String overallReasoning;    // Summary explanation
    private int    bullishCount;        // # strategies saying BUY
    private int    bearishCount;        // # strategies saying SELL
    private int    neutralCount;        // # strategies saying HOLD

    // ---- Price Prediction ----
    private List<Map<String, Object>> predictions;  // Future price predictions
    private double predictedPrice7d;    // Predicted price in 7 days
    private double predictedPrice30d;   // Predicted price in 30 days
    private String predictionTrend;     // "UPWARD" | "DOWNWARD" | "SIDEWAYS"
    private double predictionConfidence;// Confidence in the prediction (R²)

    // ---- Metadata ----
    private LocalDateTime analysisTime; // When analysis was performed
    private String        dataSource;   // "Yahoo Finance"
    private int           dataPoints;   // Number of data points analyzed

    // ---- Constructors ----
    public AnalysisResponse() {
        this.analysisTime = LocalDateTime.now();
        this.dataSource   = "Yahoo Finance";
        this.currency     = "USD";
    }

    // ---- Getters and Setters ----
    public String getSymbol()               { return symbol; }
    public void setSymbol(String s)         { this.symbol = s; }

    public String getCompanyName()          { return companyName; }
    public void setCompanyName(String n)    { this.companyName = n; }

    public double getCurrentPrice()         { return currentPrice; }
    public void setCurrentPrice(double p)   { this.currentPrice = p; }

    public double getPriceChange()          { return priceChange; }
    public void setPriceChange(double c)    { this.priceChange = c; }

    public double getPriceChangePct()       { return priceChangePct; }
    public void setPriceChangePct(double p) { this.priceChangePct = p; }

    public double getWeekHigh52()           { return weekHigh52; }
    public void setWeekHigh52(double h)     { this.weekHigh52 = h; }

    public double getWeekLow52()            { return weekLow52; }
    public void setWeekLow52(double l)      { this.weekLow52 = l; }

    public long getAvgVolume()              { return avgVolume; }
    public void setAvgVolume(long v)        { this.avgVolume = v; }

    public String getCurrency()             { return currency; }
    public void setCurrency(String c)       { this.currency = c; }

    public String getExchange()             { return exchange; }
    public void setExchange(String e)       { this.exchange = e; }

    public List<StockData> getPriceHistory()              { return priceHistory; }
    public void setPriceHistory(List<StockData> h)        { this.priceHistory = h; }

    public List<StrategyResult> getStrategyResults()      { return strategyResults; }
    public void setStrategyResults(List<StrategyResult> r){ this.strategyResults = r; }

    public String getOverallSignal()                      { return overallSignal; }
    public void setOverallSignal(String s)                { this.overallSignal = s; }

    public int getOverallConfidence()                     { return overallConfidence; }
    public void setOverallConfidence(int c)               { this.overallConfidence = c; }

    public String getOverallReasoning()                   { return overallReasoning; }
    public void setOverallReasoning(String r)             { this.overallReasoning = r; }

    public int getBullishCount()                          { return bullishCount; }
    public void setBullishCount(int b)                    { this.bullishCount = b; }

    public int getBearishCount()                          { return bearishCount; }
    public void setBearishCount(int b)                    { this.bearishCount = b; }

    public int getNeutralCount()                          { return neutralCount; }
    public void setNeutralCount(int n)                    { this.neutralCount = n; }

    public List<Map<String, Object>> getPredictions()     { return predictions; }
    public void setPredictions(List<Map<String, Object>> p){ this.predictions = p; }

    public double getPredictedPrice7d()                   { return predictedPrice7d; }
    public void setPredictedPrice7d(double p)             { this.predictedPrice7d = p; }

    public double getPredictedPrice30d()                  { return predictedPrice30d; }
    public void setPredictedPrice30d(double p)            { this.predictedPrice30d = p; }

    public String getPredictionTrend()                    { return predictionTrend; }
    public void setPredictionTrend(String t)              { this.predictionTrend = t; }

    public double getPredictionConfidence()               { return predictionConfidence; }
    public void setPredictionConfidence(double c)         { this.predictionConfidence = c; }

    public LocalDateTime getAnalysisTime()                { return analysisTime; }
    public void setAnalysisTime(LocalDateTime t)          { this.analysisTime = t; }

    public String getDataSource()                         { return dataSource; }
    public void setDataSource(String s)                   { this.dataSource = s; }

    public int getDataPoints()                            { return dataPoints; }
    public void setDataPoints(int d)                      { this.dataPoints = d; }
}
