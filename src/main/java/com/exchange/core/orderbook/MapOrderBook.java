package com.exchange.core.orderbook;

import com.exchange.core.config.AppConstants;
import com.exchange.core.model.Execution;
import com.exchange.core.model.MarketData;
import com.exchange.core.model.Message;
import com.exchange.core.model.Order;
import com.exchange.core.model.enums.OrderSide;

import java.math.BigDecimal;
import java.util.*;

public class MapOrderBook implements OrderBook {
    private final Map<BigDecimal, List<Order>> bidsMap;
    private final Map<BigDecimal, List<Order>> asksMap;
    private final String symbol;
    private final Queue<Message> outbound;

    public MapOrderBook(String symbol, Queue<Message> outbound){
        this.symbol = symbol;
        this.outbound = outbound;
        bidsMap = new TreeMap<>();
        asksMap = new TreeMap<>(Comparator.reverseOrder());
    }

    @Override
    public void addOrder(Order order) {
        order.setLeavesQty(order.getOrderQty());
        match(order);
        if (order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0){
            addToOrderBook(order);
        }
    }

    private void addToOrderBook(Order order){
        Map<BigDecimal, List<Order>> book = order.getSide() == OrderSide.BUY ? bidsMap : asksMap;
        book.merge(order.getPrice(), new ArrayList<>(List.of(order)), (o, v)->{
            o.addAll(v);
            return o;
        });
        outbound.add(buildMarketData());
    }

    private void match(Order taker) {
        List<Order> orders;
        if (taker.getSide() == OrderSide.BUY){
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

    private MarketData buildMarketData() {
        int depth = Math.max(bidsMap.size(), asksMap.size());
        if (depth > AppConstants.DEFAULT_DEPTH){
            depth = AppConstants.DEFAULT_DEPTH;
        }
        MarketData md = new MarketData();
        md.setDepth(depth);
        md.setSymbol(symbol);
        md.setTransactTime(System.currentTimeMillis());

        BigDecimal[][] bids = new BigDecimal[depth][];
        BigDecimal[][] asks = new BigDecimal[depth][];
        int bidIndex = 0, asksIndex = 0;
        for(Map.Entry<BigDecimal, List<Order>> e: bidsMap.entrySet()){
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            for(Order order: e.getValue()){
                cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
            }
            bids[bidIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
        }
        for(Map.Entry<BigDecimal, List<Order>> e: asksMap.entrySet()){
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            for(Order order: e.getValue()){
                cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
            }
            asks[asksIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
        }
        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }
}