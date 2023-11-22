package com.exchange.core.orderbook.map;

import com.exchange.core.config.AppConstants;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.*;
import com.exchange.core.orderbook.OrderBook;
import com.exchange.core.orderbook.post.PostOrderCheck;
import com.exchange.core.orderbook.pre.PreOrderCheck;

import java.math.BigDecimal;
import java.util.*;

public class MapOrderBook implements OrderBook {
    private final NavigableMap<BigDecimal, List<Order>> bids = new TreeMap<>();
    private final NavigableMap<BigDecimal, List<Order>> asks = new TreeMap<>(Comparator.reverseOrder());
    private final String symbol;
    private final PreOrderCheck preOrderCheck;
    private final PostOrderCheck postOrderCheck;

    public MapOrderBook(String symbol, PreOrderCheck preOrderCheck, PostOrderCheck postOrderCheck) {
        this.symbol = symbol;
        this.preOrderCheck = preOrderCheck;
        this.postOrderCheck = postOrderCheck;
    }

    @Override
    public void addOrder(Order order) {
        if (!preOrderCheck.validateOrder(order)) {
            return;
        }
        preOrderCheck.updateNewOrder(order);
        preOrderCheck.lockBalance(order);
        postOrderCheck.sendExecReportNew(order);
        match(order);
        // if order not fully matched we should either add to orderbook or cancel if it's market order
        if (order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            if (order.getType() == OrderType.MARKET){
                postOrderCheck.cancelOrder(order);
            } else {
                add(order);
            }
        }
        postOrderCheck.sendMarketData(buildMarketData());
    }

    /**
     * LIMIT BUY => we match all sells with price equals or below
     */
    private void match(Order taker) {
        Map<BigDecimal, List<Order>> counterMap;
        if (taker.getSide() == OrderSide.BUY) {
            counterMap = asks.headMap(taker.getPrice());
        } else {
            counterMap = bids.headMap(taker.getPrice());
        }
        Iterator<BigDecimal> iterator = counterMap.keySet().iterator();
        while (iterator.hasNext()) {
            final BigDecimal tradePrice = iterator.next();
            List<Order> orders = counterMap.get(tradePrice);
            if (orders != null) {
                Iterator<Order> ordIterator = orders.iterator();
                while (ordIterator.hasNext()) {
                    Order maker = ordIterator.next();
                    BigDecimal tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
                    BigDecimal tradeAmount = tradeQty.multiply(tradePrice);
                    taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
                    maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

                    postOrderCheck.settleTrade(taker, maker, tradeQty, tradeAmount);
                    postOrderCheck.sendExecReportTrade(taker, maker, tradeQty, tradePrice);

                    if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                        ordIterator.remove();
                    }
                }
                if (orders.size() == 0) {
                    iterator.remove();
                }
            }
        }
    }

    private void add(Order order){
        Map<BigDecimal, List<Order>> book = order.getSide() == OrderSide.BUY ? bids : asks;
        book.merge(order.getPrice(), new ArrayList<>(List.of(order)), (o, v) -> {
            o.addAll(v);
            return o;
        });
    }

    private MarketData buildMarketData() {
        int depth = Math.max(bids.size(), asks.size());
        if (depth > AppConstants.DEFAULT_DEPTH) {
            depth = AppConstants.DEFAULT_DEPTH;
        }
        MarketData md = new MarketData();
        md.setDepth(depth);
        md.setSymbol(symbol);
        md.setTransactTime(System.currentTimeMillis());

        BigDecimal[][] bids = new BigDecimal[depth][];
        BigDecimal[][] asks = new BigDecimal[depth][];
        int bidIndex = 0, asksIndex = 0;
        for (Map.Entry<BigDecimal, List<Order>> e : this.bids.entrySet()) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            for (Order order : e.getValue()) {
                cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
            }
            bids[bidIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
        }
        for (Map.Entry<BigDecimal, List<Order>> e : this.asks.entrySet()) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            for (Order order : e.getValue()) {
                cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
            }
            asks[asksIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
        }
        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }
}