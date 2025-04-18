package com.exchange.core.matching.orderbook.array;

import com.exchange.core.model.msg.Order;
import java.util.Iterator;

public interface PriceLeve2 {
    void add(Order order);

    Order getFirst();

    boolean remote(Order order);

    Iterator<Order> getOrders();
}
