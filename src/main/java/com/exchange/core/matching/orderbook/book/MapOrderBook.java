package com.exchange.core.matching.orderbook.book;

import com.exchange.core.config.AppConstants;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.level.LinkedListPriceLevel;
import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class MapOrderBook implements OrderBook, Snapshotable {

  private final NavigableMap<BigDecimal, PriceLevel> bids = new TreeMap<>(
          Comparator.reverseOrder());
  private final NavigableMap<BigDecimal, PriceLevel> asks = new TreeMap<>();
  private final Map<Long, Order> orderIdMap = new HashMap<>();
  private final String symbol;

  public MapOrderBook(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public List<Trade> match(Order taker) {
    return switch (taker.getType()) {
      case MARKET -> matchMarket(taker);
      case LIMIT -> matchLimit(taker);
    };
  }

  private List<Trade> matchMarket(Order taker) {
    List<Trade> trades = new ArrayList<>();
    Map<BigDecimal, PriceLevel> counterMap = taker.getSide() == OrderSide.BUY ? asks : bids;
    Iterator<BigDecimal> iterator = counterMap.keySet().iterator();
    while (iterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      final BigDecimal tradePrice = iterator.next();
      PriceLevel level = counterMap.get(tradePrice);
      if (level == null) {
        continue;
      }
      level.resetIterator();
      while (level.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
        Order maker = level.next();

        BigDecimal tradeQty, tradeAmount;
        if (taker.getSide() == OrderSide.BUY) {
          BigDecimal takerTradeAmount = taker.getLeavesQty();
          BigDecimal takerTradeQty = takerTradeAmount.divide(tradePrice,
                  AppConstants.ROUNDING_SCALE, RoundingMode.DOWN);

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
          orderIdMap.remove(maker.getOrderId());
        }
      }
      if (!level.hasNext()) {
        iterator.remove();
      }
    }
    return trades;
  }


  private List<Trade> matchLimit(Order taker) {
    List<Trade> trades = new ArrayList<>();
    Map<BigDecimal, PriceLevel> counterMap;
    if (taker.getSide() == OrderSide.BUY) {
      counterMap = asks.headMap(taker.getPrice(), true);
    } else {
      counterMap = bids.headMap(taker.getPrice(), true);
    }
    Iterator<BigDecimal> iterator = counterMap.keySet().iterator();
    while (iterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      final BigDecimal tradePrice = iterator.next();
      PriceLevel level = counterMap.get(tradePrice);
      if (level == null) {
        continue;
      }
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
          orderIdMap.remove(maker.getOrderId());
        }
      }
      if (!level.hasNext()) {
        iterator.remove();
      }
    }
    return trades;
  }

  @Override
  public boolean add(Order order) {
    Map<BigDecimal, PriceLevel> book = order.getSide() == OrderSide.BUY ? bids : asks;
    book.merge(order.getPrice(), new LinkedListPriceLevel(order), (k, v) -> {
      v.add(order);
      return v;
    });
    orderIdMap.put(order.getOrderId(), order);
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
    Order order = orderIdMap.remove(orderId);
    if (order == null) {
      return false;
    }
    Map<BigDecimal, PriceLevel> book = order.getSide() == OrderSide.BUY ? bids : asks;
    PriceLevel level = book.get(order.getPrice());
    level.resetIterator();
    level.remove(order);
    if (!level.hasNext()){
      book.remove(level.getPrice());
    }
    return true;
  }

  @Override
  public Order getOrder(long orderId) {
    return orderIdMap.get(orderId);
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
    int bidIndex = 0, asksIndex = 0;
    for (Map.Entry<BigDecimal, PriceLevel> e : this.bids.entrySet()) {
      BigDecimal cumulativeQuantity = BigDecimal.ZERO;
      PriceLevel level = e.getValue();
      level.resetIterator();
      while (level.hasNext()) {
        cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
      }
      bids[bidIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
      if (bidIndex == bidSize) {
        break;
      }
    }
    for (Map.Entry<BigDecimal, PriceLevel> e : this.asks.entrySet()) {
      BigDecimal cumulativeQuantity = BigDecimal.ZERO;
      PriceLevel level = e.getValue();
      level.resetIterator();
      while (level.hasNext()) {
        cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
      }
      asks[asksIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
      if (asksIndex == askSize) {
        break;
      }
    }
    md.setBids(bids);
    md.setAsks(asks);
    return md;
  }

  @Override
  public SnapshotType getType() {
    return SnapshotType.ORDER_BOOK;
  }

  @Override
  public SnapshotItem create() {
    List<Order> orders = new ArrayList<>();
    bids.values().forEach(level -> {
      level.resetIterator();
      while (level.hasNext()) {
        orders.add(level.next());
      }
    });
    asks.values().forEach(level -> {
      level.resetIterator();
      while (level.hasNext()) {
        orders.add(level.next());
      }
    });
    SnapshotItem item = new SnapshotItem();
    item.setType(getType());
    item.setData(orders);
    return item;
  }

  @Override
  public void load(SnapshotItem data) {
    ((List<Order>) data.getData())
            .forEach(this::add);
  }
}