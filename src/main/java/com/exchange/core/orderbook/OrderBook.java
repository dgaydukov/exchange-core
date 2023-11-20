package com.exchange.core.orderbook;

import com.exchange.core.model.msg.Order;

public interface OrderBook {
    void addOrder(Order order);
}