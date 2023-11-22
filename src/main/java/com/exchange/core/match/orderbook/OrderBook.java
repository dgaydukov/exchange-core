package com.exchange.core.match.orderbook;

import com.exchange.core.model.Trade;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.util.List;

public interface OrderBook {
    List<Trade> match(Order order);

    void add(Order order);

    MarketData buildMarketData();
}