package com.exchange.core.matching.counter;

/**
 * since we have OrderBook per instrument, make sure you are using same instance across all
 * instruments
 */
public interface GlobalCounter {

  long getNextOrderId();

  long getNextExecutionId();
}