package com.exchange.core.matching.orderbook.level;

import com.exchange.core.model.msg.Order;

/**
 * Class specifically designed for LinkedListOrderBook, so we can store bids/asks as linked list
 * For this add 2 files prev/next so PriceLevel can be iterable linked list
 */
public class OrderBookLevel extends LinkedListPriceLevel {
    public OrderBookLevel(Order order) {
        super(order);
    }

    public OrderBookLevel prev;
    public OrderBookLevel next;
}
