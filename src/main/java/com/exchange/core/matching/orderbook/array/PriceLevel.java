package com.exchange.core.matching.orderbook.array;

import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;

public interface PriceLevel {
    BigDecimal getPrice();

    void add(Order order);

    boolean remove(Order order);

    // get next order in the line
    Order next();

    boolean hasNext();
    // reset iterator for next method
    void resetIterator();
}
