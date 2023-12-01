package com.exchange.core.matching.orderbook;

import com.exchange.core.config.AppConstants;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class MapOrderBook implements OrderBook {

  private final NavigableMap<BigDecimal, List<Order>> bids = new TreeMap<>(
      Comparator.reverseOrder());
  private final NavigableMap<BigDecimal, List<Order>> asks = new TreeMap<>();
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
    Map<BigDecimal, List<Order>> counterMap = taker.getSide() == OrderSide.BUY ? asks : bids;
    Iterator<BigDecimal> iterator = counterMap.keySet().iterator();
    while (iterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      final BigDecimal tradePrice = iterator.next();
      List<Order> orders = counterMap.get(tradePrice);
      if (orders != null) {
        Iterator<Order> ordIterator = orders.iterator();
        while (ordIterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
          Order maker = ordIterator.next();

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
            ordIterator.remove();
          }
        }
        if (orders.size() == 0) {
          iterator.remove();
        }
      }
    }
    return trades;
  }


  private List<Trade> matchLimit(Order taker) {
    List<Trade> trades = new ArrayList<>();
    Map<BigDecimal, List<Order>> counterMap;
    if (taker.getSide() == OrderSide.BUY) {
      counterMap = asks.headMap(taker.getPrice(), true);
    } else {
      counterMap = bids.headMap(taker.getPrice(), true);
    }
    Iterator<BigDecimal> iterator = counterMap.keySet().iterator();
    while (iterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      final BigDecimal tradePrice = iterator.next();
      List<Order> orders = counterMap.get(tradePrice);
      if (orders != null) {
        Iterator<Order> ordIterator = orders.iterator();
        while (ordIterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
          Order maker = ordIterator.next();
          BigDecimal tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
          BigDecimal tradeAmount = tradeQty.multiply(tradePrice);
          taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
          maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

          trades.add(new Trade(taker, maker, tradeQty, tradePrice, tradeAmount));

          if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
            ordIterator.remove();
          }
        }
        if (orders.size() == 0) {
          iterator.remove();
        }
      }
    }
    return trades;
  }

  @Override
  public void add(Order order) {
    Map<BigDecimal, List<Order>> book = order.getSide() == OrderSide.BUY ? bids : asks;
    book.merge(order.getPrice(), new ArrayList<>(List.of(order)), (o, v) -> {
      o.addAll(v);
      return o;
    });
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