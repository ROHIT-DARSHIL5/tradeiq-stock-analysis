package com.trading.engine.service;

// ============================================================
// StockDataService.java — Market Data Fetching Service
//
// DATA SOURCE: Yahoo Finance (Unofficial API — free, no key needed)
//   URL: https://query1.finance.yahoo.com/v8/finance/chart/{symbol}
//        ?interval=1d&range=1y
//
// WHAT THIS SERVICE DOES:
//   1. Sends HTTP GET request to Yahoo Finance
//   2. Parses the JSON response to extract OHLCV data
//   3. Returns a sorted list of StockData objects
//
// ENCAPSULATION: JSON parsing logic is hidden — callers only
//   see List<StockData>. Implementation can change without
//   affecting other classes.
// ============================================================

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trading.engine.model.StockData;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for fetching historical stock price data
 * from Yahoo Finance's chart API.
 *
 * @Service — Spring registers this as a singleton service bean.
 *           Injected via @Autowired in other classes.
 */
@Service
public class StockDataService {

    // Jackson ObjectMapper — thread-safe, reuse as singleton
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Java 11+ built-in HttpClient — replaces Apache HttpClient
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Yahoo Finance API endpoint template
    // interval=1d  → daily candles
    // range=1y     → 1 year of data (~252 trading days)
    private static final String YAHOO_URL =
        "https://query1.finance.yahoo.com/v8/finance/chart/%s?interval=1d&range=1y";

    /**
     * Fetches historical daily OHLCV data for a stock symbol.
     *
     * @param symbol Stock ticker symbol (e.g., "AAPL", "GOOGL", "TCS.NS")
     * @return List of StockData objects sorted chronologically (oldest first)
     * @throws RuntimeException if data fetch fails or symbol is invalid
     */
    public List<StockData> fetchHistoricalData(String symbol) {
        String url = String.format(YAHOO_URL, symbol.toUpperCase().trim());

        try {
            // ---- Build HTTP Request ----
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    // Yahoo Finance requires a browser-like User-Agent
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            // ---- Send Request and get Response ----
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            // Check HTTP status code
            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "Yahoo Finance returned HTTP " + response.statusCode() +
                    " for symbol: " + symbol);
            }

            // ---- Parse JSON Response ----
            return parseYahooResponse(response.body(), symbol);

        } catch (IOException e) {
            throw new RuntimeException(
                "Network error fetching data for " + symbol + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted for " + symbol, e);
        }
    }

    /**
     * Fetches basic stock metadata (company name, exchange, currency).
     *
     * @param symbol Stock ticker
     * @return Array of [companyName, exchange, currency]
     */
    public String[] fetchStockMetadata(String symbol) {
        String url = String.format(YAHOO_URL, symbol.toUpperCase().trim());
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode meta = root.path("chart").path("result").get(0).path("meta");

            String companyName = meta.path("shortName").asText(symbol.toUpperCase());
            String exchange    = meta.path("exchangeName").asText("Unknown");
            String currency    = meta.path("currency").asText("USD");
            String fullName    = meta.path("longName").asText(companyName);

            return new String[]{ fullName.isEmpty() ? companyName : fullName,
                                 exchange, currency };

        } catch (Exception e) {
            return new String[]{ symbol.toUpperCase(), "Unknown", "USD" };
        }
    }

    /**
     * Parses Yahoo Finance JSON response into a list of StockData objects.
     *
     * Yahoo Finance JSON structure:
     * {
     *   "chart": {
     *     "result": [{
     *       "meta": { ... },
     *       "timestamp": [unix_ts_1, unix_ts_2, ...],
     *       "indicators": {
     *         "quote": [{
     *           "open":   [p1, p2, ...],
     *           "high":   [p1, p2, ...],
     *           "low":    [p1, p2, ...],
     *           "close":  [p1, p2, ...],
     *           "volume": [v1, v2, ...]
     *         }],
     *         "adjclose": [{
     *           "adjclose": [p1, p2, ...]
     *         }]
     *       }
     *     }]
     *   }
     * }
     *
     * @param json   Raw JSON string from Yahoo Finance
     * @param symbol Stock symbol for error messages
     * @return Chronologically sorted list of StockData
     */
    private List<StockData> parseYahooResponse(String json, String symbol) throws IOException {
        JsonNode root   = objectMapper.readTree(json);

        // Navigate JSON tree to find the result array
        JsonNode result = root.path("chart").path("result");
        if (result.isMissingNode() || result.isEmpty()) {
            JsonNode error = root.path("chart").path("error");
            throw new RuntimeException(
                "No data found for symbol '" + symbol + "'. " +
                (error.isMissingNode() ? "Check symbol is valid." : error.toString())
            );
        }

        JsonNode firstResult = result.get(0);
        JsonNode timestamps  = firstResult.path("timestamp");
        JsonNode indicators  = firstResult.path("indicators");
        JsonNode quote       = indicators.path("quote").get(0);
        JsonNode adjClose    = indicators.path("adjclose").get(0).path("adjclose");

        // Extract individual OHLCV arrays
        JsonNode opens   = quote.path("open");
        JsonNode highs   = quote.path("high");
        JsonNode lows    = quote.path("low");
        JsonNode closes  = quote.path("close");
        JsonNode volumes = quote.path("volume");

        List<StockData> stockDataList = new ArrayList<>();

        // Iterate through each trading day
        for (int i = 0; i < timestamps.size(); i++) {
            // Skip null values (can occur during market holidays)
            if (closes.get(i).isNull() || opens.get(i).isNull()) continue;

            // Convert Unix timestamp (seconds) to LocalDate
            long      unixTimestamp = timestamps.get(i).asLong();
            LocalDate date          = Instant.ofEpochSecond(unixTimestamp)
                                            .atZone(ZoneId.of("America/New_York"))
                                            .toLocalDate();

            double open     = opens.get(i).asDouble();
            double high     = highs.get(i).asDouble();
            double low      = lows.get(i).asDouble();
            double close    = closes.get(i).asDouble();
            double adj      = adjClose.size() > i && !adjClose.get(i).isNull()
                              ? adjClose.get(i).asDouble() : close;
            long   volume   = volumes.get(i).asLong();

            stockDataList.add(new StockData(date, open, high, low, close, adj, volume));
        }

        if (stockDataList.isEmpty()) {
            throw new RuntimeException("No valid data points found for " + symbol);
        }

        return stockDataList;
    }
}
