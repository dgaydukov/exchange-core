package com.exchange.core.matching.orderbook.book;

import com.exchange.core.config.AppConstants;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.ipq.IndexedPriorityQueue;
import com.exchange.core.matching.orderbook.ipq.IndexedPriorityQueueImpl;
import com.exchange.core.matching.orderbook.ipq.SortOrder;
import com.exchange.core.matching.orderbook.level.LinkedListPriceLevel;
import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Using custom data structures known as IndexedPriorityQueue to store bids/asks
 * PriorityQueue is enough to store bids/asks, and indexed means we can fetch any PriceLevel by price as index
 */
public class IpqOrderBook implements OrderBook {
    private final String symbol;
    private final IndexedPriorityQueue<BigDecimal, PriceLevel> bids;
    private final IndexedPriorityQueue<BigDecimal, PriceLevel> asks;
    Map<Long, Order> orderIdMap = new HashMap<>();

    public IpqOrderBook(String symbol, int initialBookSize, int bookGrowSize){
        this.symbol = symbol;
        bids = new IndexedPriorityQueueImpl<>(SortOrder.DESC, initialBookSize, bookGrowSize);
        asks = new IndexedPriorityQueueImpl<>(SortOrder.ASC, initialBookSize, bookGrowSize);
    }

    @Override
    public List<Trade> match(Order order) {
        return List.of();
    }

    @Override
    public boolean add(Order order) {
        IndexedPriorityQueue<BigDecimal, PriceLevel> pq = order.getSide() == OrderSide.BUY ? bids : asks;

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
                ? bids.getExact(order.getPrice()) : asks.getExact(order.getPrice());
        level.remove(order);
        return true;
    }

    @Override
    public MarketData buildMarketData() {
        int bidSize = Math.min(bids.size(), AppConstants.DEFAULT_DEPTH);
        int askSize = Math.min(asks.size(), AppConstants.DEFAULT_DEPTH);
        int depth = Math.max(bidSize, askSize);
        MarketData md = new MarketData();
        md.setDepth(depth);
        md.setSymbol(symbol);
        md.setTransactTime(System.currentTimeMillis());

        BigDecimal[][] bids = new BigDecimal[bidSize][];
        BigDecimal[][] asks = new BigDecimal[askSize][];

        for (int i = 0; i < bidSize; i++) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            PriceLevel level = this.bids[i];
            level.resetIterator();
            while (level.hasNext()) {
                cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
            }
            bids[i] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
        }
        for (int i = 0; i < askSize; i++) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            PriceLevel level = this.asks[i];
            level.resetIterator();
            while (level.hasNext()) {
                cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
            }
            asks[i] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
        }
        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }
}
