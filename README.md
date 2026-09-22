# TradeIQ — AI-Powered Trading Strategy Analysis & Stock Prediction Engine

A full-stack stock analysis application that combines **technical-analysis strategies, live-at-runtime market-data retrieval, and linear-regression forecasting** in a Java Spring Boot backend with a responsive browser-based dashboard.

> **Educational project:** TradeIQ is a technical-analysis and software-engineering project. Its BUY/SELL/HOLD outputs and price projections are experimental and are **not financial advice or guaranteed predictions**.



##  What TradeIQ Does

TradeIQ lets a user enter a stock ticker and receive:

- Daily historical OHLCV market data
- Technical-indicator analysis across **5 independent strategies**
- BUY / SELL / HOLD signals
- Per-strategy confidence and reasoning
- Majority-vote overall signal aggregation
- 7-day and 30-day linear-regression price projections
- R² model-fit measurement
- Prediction uncertainty bounds
- 52-week high/low and average-volume metrics
- Watchlist summaries
- Multi-stock comparison through the backend API
- Interactive Chart.js visualizations

---

## Architecture

```text
┌──────────────────────────────────────────────────────────────┐
│                         TradeIQ                              │
├──────────────────────────────┬───────────────────────────────┤
│         FRONTEND             │           BACKEND              │
│                              │                               │
│ HTML / CSS / JavaScript      │ Java 17 + Spring Boot         │
│ Chart.js                     │ REST API                      │
│                              │                               │
│ Stock Search ───────────────►│ StockController               │
│ Dashboard ◄──────────────────│       │                       │
│ Charts                       │       ▼                       │
│ Strategy Cards               │ AnalysisService              │
│ Prediction View              │       │                       │
│ Watchlist                    │ ┌─────┴───────────────┐       │
│                              │ ▼                     ▼       │
│                              │ Trading Strategies    │       │
│                              │ (5 implementations)   │       │
│                              │                       │       │
│                              │ PredictionService     │       │
│                              │                       │       │
│                              │ StockDataService      │       │
│                              └──────────┬────────────┘       │
│                                         │                    │
└─────────────────────────────────────────┼────────────────────┘
                                          ▼
                                Yahoo Finance Chart API
                                          │
                                          ▼
                                  Daily OHLCV Data
```

---

## Repository Structure

```text
TradeIQ/
│
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/trading/engine/
│           │   ├── TradingEngineApplication.java
│           │   ├── config/
│           │   │   └── CorsConfig.java
│           │   ├── controller/
│           │   │   └── StockController.java
│           │   ├── model/
│           │   │   ├── AnalysisResponse.java
│           │   │   ├── StockData.java
│           │   │   └── StrategyResult.java
│           │   ├── service/
│           │   │   ├── StockDataService.java
│           │   │   ├── AnalysisService.java
│           │   │   └── PredictionService.java
│           │   └── strategy/
│           │       ├── TradingStrategy.java
│           │       ├── MovingAverageStrategy.java
│           │       ├── RSIStrategy.java
│           │       ├── MACDStrategy.java
│           │       ├── BollingerBandsStrategy.java
│           │       └── StochasticStrategy.java
│           └── resources/
│               └── application.properties
│
├── frontend/
│   └── index.html
│
└── docs/
    └── TradeIQ_Project_Documentation.docx
```

---

# Analysis Pipeline

```text
Stock Symbol
     │
     ▼
Yahoo Finance
     │
     ▼
1 Year Daily OHLCV Data
     │
     ▼
Data Parsing & Validation
     │
     ├──────────────┬──────────────┬──────────────┬──────────────┐
     ▼              ▼              ▼              ▼              ▼
    EMA             RSI            MACD       Bollinger      Stochastic
     │              │              │              │              │
     └──────────────┴──────────────┴──────────────┴──────────────┘
                                  │
                                  ▼
                         Strategy Results
                                  │
                                  ▼
                         Majority Aggregation
                                  │
                                  ▼
                        BUY / SELL / HOLD
                                  │
                                  ▼
                       Linear Regression
                                  │
                                  ▼
                    7-Day / 30-Day Forecast
```

---

# Trading Strategies

TradeIQ implements five strategy classes under a common abstract `TradingStrategy` base class.

| Strategy | Parameters | Category | Core Idea |
|---|---|---|---|
| EMA Crossover | 12 / 26 | Trend Following | Short/long EMA crossover |
| RSI | 14 | Oscillator | Overbought / oversold momentum |
| MACD | 12 / 26 / 9 | Trend + Momentum | MACD/signal-line relationship |
| Bollinger Bands | 20 / 2σ | Volatility | Price relative to volatility bands |
| Stochastic Oscillator | 14 / 3 | Oscillator | Close relative to recent price range |

Each strategy produces a `StrategyResult` containing signal information, confidence, reasoning, indicator values, strategy type, and timeframe.

---

## 1. EMA Crossover

The system calculates 12-period and 26-period exponential moving averages.

```text
EMA(12) > EMA(26)
        │
        ▼
 Short-term trend stronger
        │
        ▼
      BUY bias

EMA(12) < EMA(26)
        │
        ▼
 Short-term trend weaker
        │
        ▼
     SELL bias
```

The implementation also considers the long-term trend when generating its signal.

---

## 2. RSI — Relative Strength Index

TradeIQ uses a 14-period RSI to measure recent price momentum.

```text
RSI < 30  → Oversold condition
RSI > 70  → Overbought condition
```

The strategy evaluates the current and previous RSI values to determine the resulting signal and reasoning.

---

## 3. MACD

The MACD implementation uses:

```text
MACD Line   = EMA(12) − EMA(26)
Signal Line = EMA(9) of MACD
Histogram   = MACD − Signal
```

The strategy evaluates MACD/signal relationships and momentum conditions to generate its result.

---

## 4. Bollinger Bands

The implementation uses a 20-period moving average with a 2-standard-deviation band.

```text
Middle = SMA(20)

Upper  = SMA(20) + 2σ
Lower  = SMA(20) − 2σ
```

The strategy uses price position relative to the bands and volatility conditions when generating signals.

---

## 5. Stochastic Oscillator

TradeIQ calculates `%K` over a 14-period window and `%D` as a 3-period average of `%K`.

```text
%K = (Close − Lowest Low) /
     (Highest High − Lowest Low) × 100

%D = SMA(3) of %K
```

The implementation uses oscillator levels and `%K/%D` relationships to generate signals.

---

# Signal Aggregation

Instead of relying on one indicator, TradeIQ runs all five strategies and aggregates their outputs.

```text
        EMA ────────┐
        RSI ────────┤
       MACD ────────┤
 Bollinger ─────────┤──► Majority Vote ──► Overall Signal
 Stochastic ────────┘
```

The backend counts:

- Bullish signals
- Bearish signals
- Neutral/HOLD signals

The overall signal is selected from the majority condition, with ties resulting in `HOLD`.

The overall confidence is calculated from the average confidence reported by the individual strategies.

---

# 🔮 Price Prediction

TradeIQ includes an experimental forecasting component based on **Ordinary Least Squares (OLS) linear regression**.

### Regression Window

The most recent **60 daily observations** are used when available.

```text
y = β₀ + β₁x
```

where:

- `x` = sequential day index
- `y` = closing price
- `β₀` = intercept
- `β₁` = slope

### R²

The model calculates the coefficient of determination:

```text
R² = 1 − SSres / SStot
```

This measures how closely the fitted line explains the historical prices in the selected regression window.

### Momentum Adjustment

The implementation also calculates a short-term weighted-moving-average momentum adjustment using the latest 10 observations.

The adjustment is exponentially decayed as the forecast horizon increases.

### Forecast Horizons

The application generates projections for:

```text
1 day
3 days
7 days
14 days
21 days
30 days
```

The dashboard highlights the 7-day and 30-day predictions.

---

# 📡 Market Data

TradeIQ retrieves **daily historical OHLCV data at runtime from Yahoo Finance's Chart API**.

The backend requests approximately **one year of daily data** for the selected ticker.

The data pipeline extracts:

- Open
- High
- Low
- Close
- Adjusted Close
- Volume
- Trading date

Examples of supported ticker formats include:

```text
AAPL
MSFT
GOOGL
TSLA
AMZN
META
NVDA

RELIANCE.NS
TCS.NS
INFY.NS
HDFCBANK.NS
```

> The repository does **not** contain a static stock-price dataset. Market data is fetched when the backend runs.

---

# REST API

The Spring Boot backend exposes six REST endpoints.

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/health` | Backend health check |
| `GET` | `/api/analyze/{symbol}` | Full stock analysis |
| `POST` | `/api/compare` | Compare multiple stocks |
| `GET` | `/api/strategies` | List available strategies |
| `GET` | `/api/history/{symbol}` | Retrieve historical price data |
| `POST` | `/api/watchlist` | Generate lightweight watchlist summaries |

### Example

```text
GET /api/analyze/AAPL
```

The response contains market metadata, price history, strategy results, aggregated signal information, and prediction results.

---

# Frontend

The frontend is implemented as a lightweight single-page application using:

- HTML5
- CSS3
- Vanilla JavaScript
- Chart.js

The interface includes:

- Stock search
- Popular-stock shortcuts
- Current price information
- Historical price visualization
- Strategy signal cards
- Confidence indicators
- Prediction visualization
- Watchlist support
- Responsive layout

The UI uses a dark trading-terminal style with responsive cards and interactive charts.

---

# Backend

The backend uses:

- **Java 17**
- **Spring Boot 3.2**
- Spring Web
- Jackson
- Java `HttpClient`
- Maven

### Backend layers

```text
Controller
    │
    ▼
Service Layer
    │
    ├── StockDataService
    ├── AnalysisService
    └── PredictionService
    │
    ▼
Strategy Layer
    │
    ├── EMA
    ├── RSI
    ├── MACD
    ├── Bollinger Bands
    └── Stochastic
    │
    ▼
Yahoo Finance
```

---

# Design Patterns & OOP Concepts

One of the main engineering goals of the backend is to demonstrate object-oriented design rather than placing every strategy in one large class.

### Strategy Pattern

Each technical-analysis method is implemented as a separate strategy class.

```text
TradingStrategy
      │
      ├── MovingAverageStrategy
      ├── RSIStrategy
      ├── MACDStrategy
      ├── BollingerBandsStrategy
      └── StochasticStrategy
```

### Template Method Pattern

`TradingStrategy.execute()` defines the common workflow:

```text
Validate Data
     ↓
Run Strategy-Specific Analysis
     ↓
Attach Common Metadata
     ↓
Return StrategyResult
```

### Runtime Polymorphism

`AnalysisService` operates on:

```java
List<TradingStrategy>
```

The same:

```java
strategy.execute(data)
```

call dispatches to the appropriate concrete strategy implementation.

### Dependency Injection

Spring Boot automatically injects the strategy implementations and services through constructor injection.

### Builder Pattern

`StrategyResult.builder()` is used to construct strategy-result objects with multiple fields.

### Encapsulation

Market data is represented through `StockData`, while data-fetching and JSON parsing are isolated inside `StockDataService`.

---

# Example Workflow

```text
1. User enters AAPL
        ↓
2. Frontend calls /api/analyze/AAPL
        ↓
3. Spring Boot receives request
        ↓
4. StockDataService fetches Yahoo Finance data
        ↓
5. JSON is parsed into StockData objects
        ↓
6. AnalysisService executes 5 strategies
        ↓
7. Strategy results are aggregated
        ↓
8. PredictionService calculates forecasts
        ↓
9. AnalysisResponse is returned as JSON
        ↓
10. Frontend renders charts and analysis
```

---

# Running Locally

## Requirements

- Java 17+
- Maven 3.8+
- Internet connection for Yahoo Finance market-data requests
- Modern web browser

## 1. Start the backend

Open a terminal in the `backend` directory:

```bash
cd backend
mvn spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

---

## 2. Verify the backend

Open:

```text
http://localhost:8080/api/health
```

Expected response includes:

```json
{
  "status": "UP",
  "service": "Trading Strategy Engine",
  "version": "1.0.0"
}
```

---

## 3. Open the frontend

Open:

```text
frontend/index.html
```

in a modern browser.

The frontend is configured to communicate with:

```text
http://localhost:8080/api
```

---

## 4. Analyze a stock

Enter a ticker such as:

```text
AAPL
```

or:

```text
TCS.NS
```

and select **ANALYZE**.

---

# Documentation

A detailed project document is included in:

```text
docs/TradeIQ_Project_Documentation.docx
```

It contains additional explanations of:

- Object-oriented design
- Trading indicators
- Mathematical formulas
- Linear regression
- Yahoo Finance data structure
- REST API design
- Design patterns
- Code-level explanations
- Project Q&A

---

# Limitations

This project is intentionally educational and has several limitations.

- Technical indicators are rule-based and do not guarantee profitable signals.
- Linear regression is a simple forecasting model and is not designed to capture complex market dynamics.
- R² measures historical fit within the selected regression window; it does **not** guarantee future predictive accuracy.
- Forecast bounds are model-derived uncertainty bounds, not guaranteed statistical confidence intervals.
- Yahoo Finance data availability depends on the external service and ticker symbol.
- The application does not perform portfolio optimization, risk-adjusted position sizing, or live order execution.
- The frontend currently expects the backend to run locally on port `8080`.

---

# Future Improvements

- Add backtesting across historical periods
- Add transaction-cost modelling
- Add additional technical indicators
- Add feature-based ML models
- Compare forecasting models
- Add portfolio-level analysis
- Add risk metrics such as volatility and drawdown
- Add authentication and persistent watchlists
- Containerize backend with Docker
- Deploy the frontend and backend
- Add automated unit and integration tests

---

# Author

**Rohit Darshil**

B.Tech Artificial Intelligence & Data Science  
Amrita Vishwa Vidyapeetham, Coimbatore

---

<p align="center">
  <b>Java • Spring Boot • Algorithms • Machine Learning • Financial Analytics</b>
</p>
