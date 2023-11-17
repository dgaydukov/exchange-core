package com.exchange.core.orderbook;

import com.exchange.core.config.AppConstants;
import com.exchange.core.model.ExecReport;
import com.exchange.core.model.MarketData;
import com.exchange.core.model.Message;
import com.exchange.core.model.Order;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.*;

public class MapOrderBook implements OrderBook {
    private final Map<BigDecimal, List<Order>> bidsMap;
    private final Map<BigDecimal, List<Order>> asksMap;
    private final String symbol;
    private final GlobalCounter counter;
    private final Queue<Message> outbound;


    public MapOrderBook(String symbol, GlobalCounter counter, Queue<Message> outbound){
        this.symbol = symbol;
        this.counter = counter;
        this.outbound = outbound;
        bidsMap = new TreeMap<>();
        asksMap = new TreeMap<>(Comparator.reverseOrder());
    }

    @Override
    public void addOrder(Order order) {
        order.setOrderId(counter.getNextOrderId());
        order.setLeavesQty(order.getOrderQty());
        sendNewExecReport(order);
        match(order);
        if (order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0){
            Map<BigDecimal, List<Order>> book = order.getSide() == OrderSide.BUY ? bidsMap : asksMap;
            book.merge(order.getPrice(), new ArrayList<>(List.of(order)), (o, v)->{
                o.addAll(v);
                return o;
            });
        }
        outbound.add(buildMarketData());
    }

    private void sendNewExecReport(Order order){
        ExecReport exec = orderToExecReport(order);
        exec.setStatus(OrderStatus.NEW);
        outbound.add(exec);
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
                BigDecimal min = taker.getLeavesQty().min(maker.getLeavesQty());
                taker.setLeavesQty(taker.getLeavesQty().subtract(min));
                maker.setLeavesQty(maker.getLeavesQty().subtract(min));

                ExecReport execTaker = orderToExecReport(taker);
                execTaker.setExecId(counter.getNextExecutionId());
                execTaker.setIsTaker(true);
                execTaker.setCounterOrderId(maker.getOrderId());
                execTaker.setStatus(OrderStatus.PARTIALLY_FILLED);
                if (taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                    execTaker.setStatus(OrderStatus.FILLED);
                }
                ExecReport execMaker = orderToExecReport(maker);
                execMaker.setExecId(counter.getNextExecutionId());
                execMaker.setIsTaker(false);
                execMaker.setCounterOrderId(taker.getOrderId());
                execMaker.setStatus(OrderStatus.PARTIALLY_FILLED);
                if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                    execMaker.setStatus(OrderStatus.FILLED);
                    // remove maker order from orderbook
                    iterator.remove();
                }

                outbound.add(execTaker);
                outbound.add(execMaker);
            }
        }
    }

    private ExecReport orderToExecReport(Order order){
        ExecReport exec = new ExecReport();
        exec.setOrderId(order.getOrderId());
        exec.setSymbol(order.getSymbol());
        exec.setPrice(order.getPrice());
        exec.setOrderQty(order.getOrderQty());
        exec.setLeavesQty(order.getLeavesQty());
        return exec;
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