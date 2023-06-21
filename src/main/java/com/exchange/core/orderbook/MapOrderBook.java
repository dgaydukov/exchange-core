package com.exchange.core.orderbook;

import com.exchange.core.model.MarketData;
import com.exchange.core.model.Order;
import com.exchange.core.model.enums.Side;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MapOrderBook implements OrderBook {
    private int securityId;

    public MapOrderBook(int securityId){
        this.securityId = securityId;
    }

    /**
     * since we have OrderBook per instrument, make sure you have one maxOrderId/executionId counter across all instruments
     */
    private long maxOrderId;
    private long maxExecutionId;

    Map<Integer, List<Order>> bidsMap = new TreeMap<>();
    Map<Integer, List<Order>> asksMap = new TreeMap<>(Comparator.reverseOrder());
    Map<Long, Order> orderMap = new HashMap<>();
    List<Exception> executionReportList = new ArrayList<>();

    @Override
    public void match(Order order) {
        List<Order> orders;
        if (order.getSide() == Side.BUY){
            orders = asksMap.get(order.getPrice());
        } else {
            orders = bidsMap.get(order.getPrice());
        }
        if (orders != null){
            Iterator<Order> iterator = orders.iterator();
            while (iterator.hasNext()){
                // match order & decrease quantity
                Order ord = iterator.next();
                int quantity = Math.abs(order.getQuantity() - ord.getQuantity());
                order.setQuantity(order.getQuantity() - quantity);
                ord.setQuantity(ord.getQuantity() - quantity);
                if (ord.getQuantity() == 0){
                    iterator.remove();
                    orderMap.remove(ord.getOrderId());
                }
            }
        }
    }

    @Override
    public void addOrder(Order order) {
        order.setOrderId(++maxOrderId);
        match(order);
        if (order.getQuantity() > 0){
            (order.getSide() == Side.BUY ? bidsMap : asksMap).merge(order.getPrice(), new ArrayList<>(List.of(order)), (o, v)->{
                o.addAll(v);
                return o;
            });
            orderMap.put(order.getOrderId(), order);
        }
    }

    @Override
    public MarketData getOrderBook() {
        MarketData md = new MarketData();
        int[][] bids = new int[bidsMap.size()][];
        int[][] asks = new int[asksMap.size()][];
        int bidIndex = 0, asksIndex = 0;
        for(Map.Entry<Integer, List<Order>> e: bidsMap.entrySet()){
            int cumulativeQuantity = 0;
            for(Order order: e.getValue()){
                cumulativeQuantity += order.getQuantity();
            }
            bids[bidIndex++] = new int[]{e.getKey(), cumulativeQuantity};
        }
        for(Map.Entry<Integer, List<Order>> e: asksMap.entrySet()){
            int cumulativeQuantity = 0;
            for(Order order: e.getValue()){
                cumulativeQuantity += order.getQuantity();
            }
            asks[asksIndex++] = new int[]{e.getKey(), cumulativeQuantity};
        }
        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }

    @Override
    public Order getOrderById(long orderId) {
        return orderMap.get(orderId);
    }
}