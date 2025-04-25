package com.exchange.core.matching.orderbook.book;

import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.model.Trade;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.util.List;

/**
 * Using custom data structures known as IndexedPriorityQueue to store bids/asks
 * PriorityQueue is enough to store bids/asks, and indexed means we can fetch any PriceLevel by price as index
 */
public class IpqOrderBook implements OrderBook {
    @Override
    public List<Trade> match(Order order) {
        return List.of();
    }

    @Override
    public boolean add(Order order) {
        return false;
    }

    @Override
    public boolean update(Order order) {
        return false;
    }

    @Override
    public boolean remove(long orderId) {
        return false;
    }

    @Override
    public MarketData buildMarketData() {
        return null;
    }
}
