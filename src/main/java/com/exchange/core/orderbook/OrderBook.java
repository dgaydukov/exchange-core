package com.exchange.core.orderbook;

import com.exchange.core.model.MarketData;
import com.exchange.core.model.Order;

public interface OrderBook {
    void match(Order order);

    void addOrder(Order order);

    MarketData getOrderBook();

    Order getOrderById(long orderId);
}