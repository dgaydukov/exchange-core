package com.exchange.core.matching.orderbook.array;

import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;

public interface PriceLevel {
    BigDecimal getPrice();

    void add(Order order);

    boolean remove();

    boolean hasNext();
    // get next order in the line
    Order next();
    // reset iterator for hasNext/next method. Always call it before you want to iterate over the list of orders
    void resetIterator();
}
