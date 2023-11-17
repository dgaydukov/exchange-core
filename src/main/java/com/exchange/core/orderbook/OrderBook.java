package com.exchange.core.orderbook;

import com.exchange.core.model.Order;

public interface OrderBook {
    void addOrder(Order order);
}