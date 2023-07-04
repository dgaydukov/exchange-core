package com.exchange.core.orderbook;

import com.exchange.core.model.Execution;
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
import java.util.function.Consumer;

public class MapOrderBook implements OrderBook {
    private int securityId;
    private GlobalCounter globalCounter;
    private Consumer<Execution> execReportConsumer;

    public MapOrderBook(int securityId, GlobalCounter globalCounter, Consumer<Execution> execReportConsumer){
        this.securityId = securityId;
        this.globalCounter = globalCounter;
        this.execReportConsumer = execReportConsumer;
    }

    Map<Integer, List<Order>> bidsMap = new TreeMap<>();
    Map<Integer, List<Order>> asksMap = new TreeMap<>(Comparator.reverseOrder());
    Map<Long, Order> orderMap = new HashMap<>();

    @Override
    public void match(Order taker) {
        List<Order> orders;
        if (taker.getSide() == Side.BUY){
            orders = asksMap.get(taker.getPrice());
        } else {
            orders = bidsMap.get(taker.getPrice());
        }
        if (orders != null){
            Iterator<Order> iterator = orders.iterator();
            while (iterator.hasNext()){
                // match order & decrease quantity
                Order maker = iterator.next();
                int quantity = Math.abs(taker.getQuantity() - maker.getQuantity());
                taker.setQuantity(taker.getQuantity() - quantity);
                maker.setQuantity(maker.getQuantity() - quantity);
                if (maker.getQuantity() == 0){
                    iterator.remove();
                    orderMap.remove(maker.getOrderId());
                }
                Execution execTaker = new Execution(taker.getOrderId(), maker.getOrderId(), false, securityId, quantity, taker.getPrice());
                Execution execMaker = new Execution(maker.getOrderId(), taker.getOrderId(), true, securityId, quantity, maker.getPrice());
                execReportConsumer.accept(execTaker);
                execReportConsumer.accept(execMaker);
            }
        }
    }

    @Override
    public void addOrder(Order order) {
        order.setOrderId(globalCounter.getNextOrderId());
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