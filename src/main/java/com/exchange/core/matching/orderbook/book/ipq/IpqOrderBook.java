package com.exchange.core.matching.orderbook.book.ipq;

import com.exchange.core.config.AppConstants;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.book.ipq.queue.IndexedPriorityQueue;
import com.exchange.core.matching.orderbook.book.ipq.queue.IndexedPriorityQueueImpl;
import com.exchange.core.matching.orderbook.book.ipq.queue.SortOrder;
import com.exchange.core.matching.orderbook.level.LinkedListPriceLevel;
import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Using custom data structures known as IndexedPriorityQueue to store bids/asks
 * PriorityQueue is enough to store bids/asks, and indexed means we can fetch any PriceLevel by price as index
 */
public class IpqOrderBook implements OrderBook {
    private final String symbol;
    private final IndexedPriorityQueue<BigDecimal, PriceLevel> bidsQueue;
    private final IndexedPriorityQueue<BigDecimal, PriceLevel> asksQueue;
    Map<Long, Order> orderIdMap = new HashMap<>();


    public IpqOrderBook(String symbol){
        this(symbol, 1024, 256);
    }

    public IpqOrderBook(String symbol, int initialBookSize, int bookGrowSize){
        this.symbol = symbol;
        bidsQueue = new IndexedPriorityQueueImpl<>(SortOrder.DESC, initialBookSize, bookGrowSize);
        asksQueue = new IndexedPriorityQueueImpl<>(SortOrder.ASC, initialBookSize, bookGrowSize);
    }

    @Override
    public List<Trade> match(Order taker) {
        List<Trade> trades = new ArrayList<>();
        if (taker.getSide() == OrderSide.BUY) {
            while (asksQueue.size() > 0 && taker.getLeavesQty().compareTo(BigDecimal.ZERO) != 0){
                PriceLevel level = asksQueue.peek();
                if (taker.getType() == OrderType.LIMIT) {
                    if (taker.getPrice().compareTo(level.getPrice()) < 0) {
                        break;
                    }
                    matchLimit(taker, level, trades);
                } else {
                    matchMarket(taker, level, trades);
                }
                if (!level.hasNext()){
                    asksQueue.poll();
                }
            }
        } else {
            while (bidsQueue.size() > 0 && taker.getLeavesQty().compareTo(BigDecimal.ZERO) != 0){
                PriceLevel level = bidsQueue.peek();
                if (taker.getType() == OrderType.LIMIT) {
                    if (taker.getPrice().compareTo(level.getPrice()) > 0) {
                        break;
                    }
                    matchLimit(taker, level, trades);
                } else {
                    matchMarket(taker, level, trades);
                }
                // check if level is empty and remove PriceLevel from array
                if (!level.hasNext()){
                    bidsQueue.poll();
                }
            }
        }
        return trades;
    }


    private void matchLimit(Order taker, PriceLevel level, List<Trade> trades) {
        final BigDecimal tradePrice = level.getPrice();
        level.resetIterator();
        while (level.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            Order maker = level.next();
            BigDecimal tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
            BigDecimal tradeAmount = tradeQty.multiply(tradePrice);
            taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
            maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

            trades.add(new Trade(taker, maker, tradeQty, tradePrice, tradeAmount));

            if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                level.remove();
            }
        }
    }

    private void matchMarket(Order taker, PriceLevel level, List<Trade> trades) {
        level.resetIterator();
        BigDecimal tradePrice = level.getPrice();
        while (level.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            Order maker = level.next();
            BigDecimal tradeQty, tradeAmount;
            if (taker.getSide() == OrderSide.BUY) {
                BigDecimal takerTradeAmount = taker.getLeavesQty();
                BigDecimal takerTradeQty = takerTradeAmount.divide(tradePrice, AppConstants.ROUNDING_SCALE,
                        RoundingMode.DOWN);

                tradeQty = takerTradeQty.min(maker.getLeavesQty());
                tradeAmount = tradeQty.multiply(tradePrice);
                if (maker.getLeavesQty().compareTo(takerTradeQty) > 0) {
                    tradeAmount = takerTradeAmount;
                }

                taker.setLeavesQty(taker.getLeavesQty().subtract(tradeAmount));
                maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

            } else {
                tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
                tradeAmount = tradeQty.multiply(tradePrice);
                taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
                maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));
            }

            trades.add(new Trade(taker, maker, tradeQty, tradePrice, tradeAmount));
            if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                level.remove();
            }
        }
    }

    @Override
    public boolean add(Order order) {
        IndexedPriorityQueue<BigDecimal, PriceLevel> pq = order.getSide() == OrderSide.BUY ? bidsQueue : asksQueue;

        PriceLevel level = pq.getExact(order.getPrice());
        if (level != null){
            level.add(order);
        }
        level = new LinkedListPriceLevel(order);
        pq.offer(level.getPrice(), level);
        orderIdMap.put(order.getOrderId(), order);

        return true;
    }

    @Override
    public boolean update(Order order) {
        return false;
    }

    @Override
    public boolean remove(long orderId) {
        Order order = orderIdMap.remove(orderId);
        if (order == null){
            return false;
        }
        PriceLevel level = order.getSide() == OrderSide.BUY
                ? bidsQueue.getExact(order.getPrice()) : asksQueue.getExact(order.getPrice());
        level.remove(order);
        return true;
    }

    @Override
    public Order getOrder(long orderId) {
        return orderIdMap.get(orderId);
    }

    @Override
    public MarketData buildMarketData() {
        int bidSize = Math.min(bidsQueue.size(), AppConstants.DEFAULT_DEPTH);
        int askSize = Math.min(asksQueue.size(), AppConstants.DEFAULT_DEPTH);
        int depth = Math.max(bidSize, askSize);
        MarketData md = new MarketData();
        md.setDepth(depth);
        md.setSymbol(symbol);
        md.setTransactTime(System.currentTimeMillis());

        BigDecimal[][] bids = new BigDecimal[bidSize][];
        BigDecimal[][] asks = new BigDecimal[askSize][];
        int bidsIndex = 0;
        int asksIndex = 0;

        bidsQueue.resetIterator();
        while (bidsQueue.hasNext() && bidsIndex < bidSize){
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            PriceLevel level = bidsQueue.next();
            level.resetIterator();
            while (level.hasNext()) {
                cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
            }
            bids[bidsIndex++] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
        }
        asksQueue.resetIterator();
        while (asksQueue.hasNext() && asksIndex < askSize){
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            PriceLevel level = asksQueue.next();
            level.resetIterator();
            while (level.hasNext()) {
                cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
            }
            asks[asksIndex++] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
        }
        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }
}
