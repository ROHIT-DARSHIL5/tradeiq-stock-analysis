package com.trading.engine.model;

// ============================================================
// StockData.java — Core Data Model (OHLCV)
//
// ENCAPSULATION DEMONSTRATION:
//   All fields are PRIVATE — external code cannot directly
//   access or modify them. Access is controlled through
//   PUBLIC getters/setters. This ensures data integrity.
//
// OHLCV = Open, High, Low, Close, Volume
//   The 5 data points that represent one trading period.
// ============================================================

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class StockData {

    // ---- Private Fields (ENCAPSULATION) ----
    // By making fields private, we prevent invalid data from
    // being assigned (e.g., negative prices).

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;       // Trading date

    private double open;          // Opening price of the period
    private double high;          // Highest price reached during period
    private double low;           // Lowest price reached during period
    private double close;         // Closing/settlement price
    private double adjClose;      // Adjusted close (accounts for dividends/splits)
    private long   volume;        // Number of shares traded

    // ---- Constructors ----

    /** Default constructor required by Jackson for JSON deserialization */
    public StockData() {}

    /**
     * Full constructor for programmatic creation of stock data points.
     * @param date     The trading date
     * @param open     Opening price
     * @param high     Day's highest price
     * @param low      Day's lowest price
     * @param close    Closing price
     * @param adjClose Dividend-adjusted closing price
     * @param volume   Total shares traded
     */
    public StockData(LocalDate date, double open, double high,
                     double low, double close, double adjClose, long volume) {
        this.date     = date;
        this.open     = open;
        this.high     = high;
        this.low      = low;
        this.close    = close;
        this.adjClose = adjClose;
        this.volume   = volume;
    }

    // ---- Getters (Read Access) ----
    // Exposing data in a controlled, read-only manner

    public LocalDate getDate()     { return date; }
    public double    getOpen()     { return open; }
    public double    getHigh()     { return high; }
    public double    getLow()      { return low; }
    public double    getClose()    { return close; }
    public double    getAdjClose() { return adjClose; }
    public long      getVolume()   { return volume; }

    // ---- Setters (Write Access with Validation) ----
    // Controlled write access — we could add validation here

    public void setDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date cannot be null");
        this.date = date;
    }

    public void setOpen(double open) {
        if (open < 0) throw new IllegalArgumentException("Price cannot be negative");
        this.open = open;
    }

    public void setHigh(double high) {
        if (high < 0) throw new IllegalArgumentException("Price cannot be negative");
        this.high = high;
    }

    public void setLow(double low) {
        if (low < 0) throw new IllegalArgumentException("Price cannot be negative");
        this.low = low;
    }

    public void setClose(double close) {
        if (close < 0) throw new IllegalArgumentException("Price cannot be negative");
        this.close = close;
    }

    public void setAdjClose(double adjClose) { this.adjClose = adjClose; }

    public void setVolume(long volume) {
        if (volume < 0) throw new IllegalArgumentException("Volume cannot be negative");
        this.volume = volume;
    }

    // ---- Derived Methods ----

    /**
     * Returns the price range (High - Low) for this period.
     * Useful for volatility calculations.
     */
    public double getRange() {
        return high - low;
    }

    /**
     * Returns the typical price: average of High, Low, Close.
     * Used in some technical indicators like CCI.
     * Formula: TP = (H + L + C) / 3
     */
    public double getTypicalPrice() {
        return (high + low + close) / 3.0;
    }

    /**
     * Returns true if this was a bullish (upward) candle.
     * Bullish: Close > Open (buyers dominated sellers)
     */
    public boolean isBullish() {
        return close > open;
    }

    @Override
    public String toString() {
        return String.format("StockData{date=%s, open=%.2f, high=%.2f, low=%.2f, close=%.2f, volume=%d}",
                date, open, high, low, close, volume);
    }
}
