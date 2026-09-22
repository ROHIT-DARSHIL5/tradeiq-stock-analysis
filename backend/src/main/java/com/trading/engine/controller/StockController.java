package com.trading.engine.controller;

// ============================================================
// StockController.java — REST API Controller
//
// This is the HTTP layer of the application. It receives
// HTTP requests from the frontend, delegates to services,
// and returns JSON responses.
//
// @RestController = @Controller + @ResponseBody
//   Every method return value is automatically serialized
//   to JSON by Jackson.
//
// REST API ENDPOINTS:
//   GET  /api/health                → Health check
//   GET  /api/analyze/{symbol}      → Full stock analysis
//   POST /api/compare               → Compare multiple stocks
//   GET  /api/strategies            → List all strategies
//   GET  /api/history/{symbol}      → Price history only
// ============================================================

import com.trading.engine.model.AnalysisResponse;
import com.trading.engine.model.StockData;
import com.trading.engine.service.AnalysisService;
import com.trading.engine.service.StockDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller that handles all incoming HTTP requests.
 *
 * @RestController — combines @Controller + @ResponseBody
 *                   All return values are JSON-serialized
 * @RequestMapping — all endpoints are prefixed with /api
 * @CrossOrigin    — additional CORS header for this controller
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StockController {

    // Services injected by Spring's dependency injection
    private final AnalysisService  analysisService;
    private final StockDataService stockDataService;

    @Autowired
    public StockController(AnalysisService analysisService,
                           StockDataService stockDataService) {
        this.analysisService  = analysisService;
        this.stockDataService = stockDataService;
    }

    // ===========================================================
    // ENDPOINT 1: Health Check
    // GET /api/health
    // Returns server status — useful to verify backend is running
    // ===========================================================
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status",    "UP",
            "service",   "Trading Strategy Engine",
            "version",   "1.0.0",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ===========================================================
    // ENDPOINT 2: Full Stock Analysis
    // GET /api/analyze/{symbol}
    //
    // @PathVariable — extracts {symbol} from the URL path
    // Example: GET /api/analyze/AAPL
    //
    // Returns complete analysis: strategies, prediction, recommendation
    // ===========================================================
    @GetMapping("/analyze/{symbol}")
    public ResponseEntity<?> analyzeStock(@PathVariable String symbol) {
        try {
            // Validate symbol format
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Symbol cannot be empty"
                ));
            }

            // Sanitize: allow letters, numbers, dots, hyphens (for international stocks)
            String cleanSymbol = symbol.trim().toUpperCase()
                    .replaceAll("[^A-Z0-9\\.\\-\\^]", "");

            if (cleanSymbol.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid symbol format: " + symbol
                ));
            }

            // Delegate to AnalysisService — runs all 5 strategies + prediction
            AnalysisResponse response = analysisService.analyze(cleanSymbol);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Return structured error response
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error",   "Analysis failed for symbol: " + symbol,
                "details", e.getMessage(),
                "hint",    "Check that the symbol is valid (e.g., AAPL, MSFT, GOOGL)"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   "Internal server error",
                "details", e.getMessage()
            ));
        }
    }

    // ===========================================================
    // ENDPOINT 3: Compare Multiple Stocks
    // POST /api/compare
    // Body: { "symbols": ["AAPL", "MSFT", "GOOGL"] }
    //
    // @RequestBody — deserializes JSON body to a Map
    // ===========================================================
    @PostMapping("/compare")
    public ResponseEntity<?> compareStocks(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> symbols = body.get("symbols");
            if (symbols == null || symbols.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Please provide a 'symbols' list in the request body"
                ));
            }
            if (symbols.size() > 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Maximum 10 symbols allowed per comparison request"
                ));
            }

            List<AnalysisResponse> results = analysisService.compareStocks(symbols);
            return ResponseEntity.ok(Map.of(
                "count",   results.size(),
                "results", results
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   "Comparison failed",
                "details", e.getMessage()
            ));
        }
    }

    // ===========================================================
    // ENDPOINT 4: List Available Strategies
    // GET /api/strategies
    //
    // Returns information about all 5 trading strategies.
    // Used by the frontend to build the strategy panel.
    // ===========================================================
    @GetMapping("/strategies")
    public ResponseEntity<?> getStrategies() {
        List<Map<String, String>> strategies = analysisService.getStrategyInfo();
        return ResponseEntity.ok(Map.of(
            "count",      strategies.size(),
            "strategies", strategies
        ));
    }

    // ===========================================================
    // ENDPOINT 5: Price History Only
    // GET /api/history/{symbol}
    //
    // Returns only OHLCV data without strategy analysis.
    // Useful for chart rendering without full analysis overhead.
    // ===========================================================
    @GetMapping("/history/{symbol}")
    public ResponseEntity<?> getPriceHistory(@PathVariable String symbol) {
        try {
            String cleanSymbol = symbol.trim().toUpperCase();
            List<StockData> data = stockDataService.fetchHistoricalData(cleanSymbol);
            return ResponseEntity.ok(Map.of(
                "symbol",     cleanSymbol,
                "count",      data.size(),
                "priceData",  data
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "Could not fetch history for: " + symbol,
                "details", e.getMessage()
            ));
        }
    }

    // ===========================================================
    // ENDPOINT 6: Batch Analysis for Watchlist
    // POST /api/watchlist
    // Body: { "symbols": ["AAPL", "TSLA", ...] }
    //
    // Returns lightweight summary for each symbol in the watchlist
    // (no full history — just price, signal, confidence)
    // ===========================================================
    @PostMapping("/watchlist")
    public ResponseEntity<?> getWatchlistSummary(@RequestBody Map<String, List<String>> body) {
        try {
            List<String> symbols = body.get("symbols");
            if (symbols == null || symbols.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No symbols provided"));
            }

            List<Map<String, Object>> summaries = symbols.stream().map(sym -> {
                try {
                    AnalysisResponse analysis = analysisService.analyze(sym.trim().toUpperCase());
                    return Map.<String, Object>of(
                        "symbol",     analysis.getSymbol(),
                        "name",       analysis.getCompanyName(),
                        "price",      analysis.getCurrentPrice(),
                        "change",     analysis.getPriceChange(),
                        "changePct",  analysis.getPriceChangePct(),
                        "signal",     analysis.getOverallSignal(),
                        "confidence", analysis.getOverallConfidence()
                    );
                } catch (Exception e) {
                    return Map.<String, Object>of(
                        "symbol", sym.toUpperCase(),
                        "error",  e.getMessage()
                    );
                }
            }).toList();

            return ResponseEntity.ok(summaries);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
