package com.exchange.core.matching.orderbook.book;

import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.level.LinkedListPriceLevel;
import com.exchange.core.matching.orderbook.level.OrderBookLevel;
import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkedListOrderBook implements OrderBook {
    private OrderBookLevel bestBid;
    private OrderBookLevel bestAsk;

    private final String symbol;
    private final Map<Long, Order> orderIdMap;

    public LinkedListOrderBook(String symbol){
        this.symbol = symbol;
        orderIdMap = new HashMap<>();
    }

    @Override
    public List<Trade> match(Order order) {
        return List.of();
    }

    @Override
    public boolean add(Order order) {
        if (order.getSide() == OrderSide.BUY){
            // iterate over bids to add with specified price
            if (bestBid == null){
                bestBid = new OrderBookLevel(order);
            } else {
                OrderBookLevel level = bestBid;
                while (level != null){
                    // append order to existing level
                    if (order.getPrice().compareTo(level.getPrice()) == 0) {
                        level.add(order);
                        break;
                    }
                    // insert new level
                    if (order.getPrice().compareTo(level.getPrice()) > 0) {
                        OrderBookLevel newLevel = new OrderBookLevel(order);
                        
                        break;
                    }
                    level = level.next;
                }
            }
        } else {

        }
        return true;
    }

    @Override
    public boolean update(Order order) {
        final long orderId = order.getOrderId();
        Order o = orderIdMap.get(orderId);
        if (o == null) {
            return false;
        }
        // if we change price we need to move order into new price level
        if (order.getPrice().compareTo(o.getPrice()) != 0) {
            // remove and add
            remove(orderId);
            add(order);
        } else {
            // if we change quantity, just change on order
            order.setQuoteOrderQty(order.getQuoteOrderQty());
        }
        return true;
    }

    @Override
    public boolean remove(long orderId) {
        Order order = orderIdMap.get(orderId);
        if (order == null){
            return false;
        }
        PriceLevel level = order.level;
        level.remove(order);
        return true;
    }

    @Override
    public MarketData buildMarketData() {
        return null;
    }
}
