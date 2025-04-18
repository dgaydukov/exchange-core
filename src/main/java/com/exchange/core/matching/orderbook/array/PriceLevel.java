package com.exchange.core.matching.orderbook.array;

import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;
import java.util.Iterator;

public interface PriceLevel {
    BigDecimal getPrice();

    void add(Order order);

    boolean remove(Order order);

    // always return first order in the list
    Order getFirst();

    Iterator<Order> getOrders();
}
