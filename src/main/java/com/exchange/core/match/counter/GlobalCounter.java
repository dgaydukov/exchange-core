package com.exchange.core.match.counter;

/**
 * since we have OrderBook per instrument, make sure you have one maxOrderId/executionId counter across all instruments
 */
public interface GlobalCounter {
    long getNextOrderId();
    long getNextExecutionId();
}