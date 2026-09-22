package com.trading.engine.service;

// ============================================================
// AnalysisService.java — Strategy Orchestration (POLYMORPHISM!)
//
// POLYMORPHISM DEMONSTRATION (Runtime/Dynamic):
//   This service holds a List<TradingStrategy> — a list of the
//   ABSTRACT TYPE. At runtime, Java determines which actual class
//   (MovingAverageStrategy, RSIStrategy, etc.) to call.
//
//   strategies.forEach(strategy -> strategy.execute(data))
//
//   Even though we call the same method on each element, each
//   one runs ITS OWN implementation. This is polymorphism.
//
// DEPENDENCY INJECTION:
//   Spring automatically finds all @Component beans that extend
//   TradingStrategy and injects them as a list. We don't need
//   to manually instantiate any strategy — Spring handles it.
// ============================================================

import com.trading.engine.model.AnalysisResponse;
import com.trading.engine.model.StockData;
import com.trading.engine.model.StrategyResult;
import com.trading.engine.strategy.TradingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    // ---- Dependencies (Injected by Spring) ----
    private final List<TradingStrategy> strategies;  // All strategies (POLYMORPHISM)
    private final StockDataService      stockDataService;
    private final PredictionService     predictionService;

    /**
     * Constructor Injection — Spring injects all TradingStrategy beans.
     * This is how polymorphism is used: we have a list of the abstract type,
     * but each element is a concrete strategy subclass.
     *
     * @param strategies        All strategy beans detected by Spring
     * @param stockDataService  Data fetching service
     * @param predictionService Price prediction service
     */
    @Autowired
    public AnalysisService(List<TradingStrategy> strategies,
                           StockDataService stockDataService,
                           PredictionService predictionService) {
        this.strategies        = strategies;
        this.stockDataService  = stockDataService;
        this.predictionService = predictionService;
    }

    /**
     * Performs complete analysis for a single stock symbol.
     * Fetches data, runs all strategies, generates predictions.
     *
     * @param symbol Stock ticker symbol
     * @return Complete AnalysisResponse with all results
     */
    public AnalysisResponse analyze(String symbol) {
        // ---- Step 1: Fetch Historical Data ----
        List<StockData> data = stockDataService.fetchHistoricalData(symbol);
        String[] meta        = stockDataService.fetchStockMetadata(symbol);

        // ---- Step 2: Extract Key Metrics ----
        int    n            = data.size();
        double currentPrice = data.get(n - 1).getClose();
        double prevPrice    = data.get(n - 2).getClose();
        double priceChange  = currentPrice - prevPrice;
        double changePct    = (priceChange / prevPrice) * 100;

        // 52-week high and low
        double high52 = data.stream().mapToDouble(StockData::getHigh).max().orElse(currentPrice);
        double low52  = data.stream().mapToDouble(StockData::getLow).min().orElse(currentPrice);

        // Average volume (last 20 days)
        long avgVolume = (long) data.subList(Math.max(0, n-20), n)
                .stream().mapToLong(StockData::getVolume).average().orElse(0);

        // ---- Step 3: Run ALL Strategies (POLYMORPHISM IN ACTION) ----
        // Java uses RUNTIME DISPATCH here:
        // - For element MovingAverageStrategy → runs MovingAverageStrategy.analyze()
        // - For element RSIStrategy           → runs RSIStrategy.analyze()
        // - For element MACDStrategy          → runs MACDStrategy.analyze()
        // All called via the SAME abstract method signature: strategy.execute(data)
        List<StrategyResult> results = strategies.stream()
                .map(strategy -> strategy.execute(data))  // ← POLYMORPHIC CALL
                .toList();

        // ---- Step 4: Aggregate Signals ----
        int bullishCount = (int) results.stream().filter(StrategyResult::isBullish).count();
        int bearishCount = (int) results.stream().filter(StrategyResult::isBearish).count();
        int neutralCount = results.size() - bullishCount - bearishCount;

        // Weighted confidence average (each strategy's confidence matters)
        double avgConfidence = results.stream()
                .mapToInt(StrategyResult::getConfidence)
                .average().orElse(50);

        // Overall signal based on majority
        String overallSignal;
        if (bullishCount > bearishCount && bullishCount > neutralCount) {
            overallSignal = StrategyResult.BUY;
        } else if (bearishCount > bullishCount && bearishCount > neutralCount) {
            overallSignal = StrategyResult.SELL;
        } else if (bullishCount == bearishCount) {
            overallSignal = StrategyResult.HOLD; // Tie = neutral
        } else {
            overallSignal = StrategyResult.HOLD;
        }

        // ---- Step 5: Build Overall Reasoning ----
        String overallReasoning = buildOverallReasoning(
                overallSignal, bullishCount, bearishCount, neutralCount,
                currentPrice, symbol, results);

        // ---- Step 6: Generate Price Predictions ----
        List<Map<String, Object>> predictions = predictionService.generatePredictions(data);
        double predicted7d  = predictionService.predictPrice(data, 7);
        double predicted30d = predictionService.predictPrice(data, 30);
        double r2           = predictionService.getRSquared(data);

        String predTrend;
        double change30d = predicted30d - currentPrice;
        if (change30d > currentPrice * 0.02) {
            predTrend = "UPWARD";
        } else if (change30d < -currentPrice * 0.02) {
            predTrend = "DOWNWARD";
        } else {
            predTrend = "SIDEWAYS";
        }

        // ---- Step 7: Build Response ----
        AnalysisResponse response = new AnalysisResponse();
        response.setSymbol(symbol.toUpperCase());
        response.setCompanyName(meta[0]);
        response.setExchange(meta[1]);
        response.setCurrency(meta[2]);
        response.setCurrentPrice(Math.round(currentPrice * 100.0) / 100.0);
        response.setPriceChange(Math.round(priceChange * 100.0) / 100.0);
        response.setPriceChangePct(Math.round(changePct * 100.0) / 100.0);
        response.setWeekHigh52(Math.round(high52 * 100.0) / 100.0);
        response.setWeekLow52(Math.round(low52 * 100.0) / 100.0);
        response.setAvgVolume(avgVolume);
        response.setPriceHistory(data);
        response.setStrategyResults(results);
        response.setOverallSignal(overallSignal);
        response.setOverallConfidence((int) avgConfidence);
        response.setOverallReasoning(overallReasoning);
        response.setBullishCount(bullishCount);
        response.setBearishCount(bearishCount);
        response.setNeutralCount(neutralCount);
        response.setPredictions(predictions);
        response.setPredictedPrice7d(Math.round(predicted7d * 100.0) / 100.0);
        response.setPredictedPrice30d(Math.round(predicted30d * 100.0) / 100.0);
        response.setPredictionTrend(predTrend);
        response.setPredictionConfidence(Math.round(r2 * 100.0) / 100.0);
        response.setDataPoints(n);

        return response;
    }

    /**
     * Compares multiple stocks and returns their analyses.
     * Useful for portfolio comparison or screening.
     *
     * @param symbols List of stock symbols
     * @return List of AnalysisResponse objects, one per symbol
     */
    public List<AnalysisResponse> compareStocks(List<String> symbols) {
        return symbols.stream()
                .map(this::analyze)
                .toList();
    }

    /**
     * Returns metadata about all available strategies.
     * Used by the frontend to display strategy descriptions.
     */
    public List<Map<String, String>> getStrategyInfo() {
        return strategies.stream()
                .map(s -> Map.of(
                        "name",        s.getName(),
                        "description", s.getDescription(),
                        "type",        s.getType(),
                        "timeframe",   s.getTimeframe()
                ))
                .toList();
    }

    /**
     * Builds a human-readable summary of all strategy signals.
     */
    private String buildOverallReasoning(String signal, int bullish, int bearish,
                                          int neutral, double price, String symbol,
                                          List<StrategyResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "%s: %d of %d strategies signal %s, %d signal SELL, %d signal HOLD. ",
            symbol.toUpperCase(), bullish, results.size(),
            StrategyResult.BUY, bearish, neutral
        ));

        if (StrategyResult.BUY.equals(signal)) {
            sb.append("The majority of technical indicators suggest BULLISH conditions. ");
            sb.append("Consider buying with a defined stop-loss. ");
        } else if (StrategyResult.SELL.equals(signal)) {
            sb.append("The majority of technical indicators suggest BEARISH conditions. ");
            sb.append("Consider reducing position or waiting for technical reversal. ");
        } else {
            sb.append("Mixed signals — no clear directional bias. ");
            sb.append("Consider waiting for confluence of multiple BUY or SELL signals. ");
        }

        // Find highest confidence signal
        results.stream()
               .max((a, b) -> Integer.compare(a.getConfidence(), b.getConfidence()))
               .ifPresent(top -> sb.append(String.format(
                   "Highest confidence: %s (%s, %d%%).",
                   top.getStrategyName(), top.getSignal(), top.getConfidence()
               )));

        return sb.toString();
    }
}
