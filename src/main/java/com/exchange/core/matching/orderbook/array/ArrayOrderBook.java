package com.exchange.core.matching.orderbook.array;

import com.exchange.core.config.AppConstants;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayOrderBook implements OrderBook, Snapshotable {

  private final int DEFAULT_PRICE_LEVEL_SIZE = 1024;
  // sorted in descending order => first bid is the highest price
  private final PriceLevelImpl[] bids = new PriceLevelImpl[DEFAULT_PRICE_LEVEL_SIZE];
  // sorted in ascending order => first ask is the lowest price
  private final PriceLevelImpl[] asks = new PriceLevelImpl[DEFAULT_PRICE_LEVEL_SIZE];
  private final String symbol;


  public ArrayOrderBook(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public List<Trade> match(Order taker) {
    List<Trade> trades = new ArrayList<>();
    if (taker.getSide() == OrderSide.BUY) {
      for (int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++) {
        PriceLevelImpl level = asks[i];
        if (level == null || taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
          break;
        }
        if (taker.getType() == OrderType.LIMIT) {
          if (taker.getPrice().compareTo(level.getPrice()) < 0) {
            break;
          }
          matchLimit(taker, level, trades);
        } else {
          matchMarket(taker, level, trades);
        }
      }
    } else {
      for (int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++) {
        PriceLevelImpl level = bids[i];
        if (level == null || taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
          break;
        }
        if (taker.getType() == OrderType.LIMIT) {
          if (taker.getPrice().compareTo(level.getPrice()) > 0) {
            break;
          }
          matchLimit(taker, level, trades);
        } else {
          matchMarket(taker, level, trades);
        }
      }
    }
    return trades;
  }


  private void matchLimit(Order taker, PriceLevelImpl level, List<Trade> trades) {
    final BigDecimal tradePrice = level.getPrice();
    Iterator<Order> iterator = level.getOrders().iterator();
    while (iterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      Order maker = iterator.next();
      BigDecimal tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
      BigDecimal tradeAmount = tradeQty.multiply(tradePrice);
      taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
      maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

      trades.add(new Trade(taker, maker, tradeQty, tradePrice, tradeAmount));

      if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
        iterator.remove();
      }
    }
  }

  private void matchMarket(Order taker, PriceLevelImpl level, List<Trade> trades) {
    Iterator<Order> ordIterator = level.getOrders().iterator();
    BigDecimal tradePrice = level.getPrice();
    while (ordIterator.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
      Order maker = ordIterator.next();

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
        ordIterator.remove();
      }
    }
  }

  @Override
  public void add(Order order) {
    if (order.getSide() == OrderSide.BUY) {
      for (int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++) {
        PriceLevelImpl level = bids[i];
        if (level == null) {
          bids[i] = new PriceLevelImpl(order);
          break;
        }
        if (order.getPrice().compareTo(level.getPrice()) == 0) {
          level.addOrder(order);
          break;
        }
        if (order.getPrice().compareTo(level.getPrice()) > 0) {
          moveLeft(i, new PriceLevelImpl(order), bids);
          break;
        }
      }
    } else {
      for (int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++) {
        PriceLevelImpl level = asks[i];
        if (level == null) {
          asks[i] = new PriceLevelImpl(order);
          break;
        }
        if (order.getPrice().compareTo(level.getPrice()) == 0) {
          level.addOrder(order);
          break;
        }
        if (order.getPrice().compareTo(level.getPrice()) < 0) {
          moveLeft(i, new PriceLevelImpl(order), asks);
          break;
        }
      }
    }
  }

  private void moveLeft(int index, PriceLevelImpl level, PriceLevelImpl[] arr) {
    int len = arr.length;
    if (arr[len - 1] != null) {
      throw new AppException("PriceLevel Overflow. Fail to move left");
    }
    for (int i = len - 2; i >= index; i--) {
      arr[i + 1] = arr[i];
    }
    arr[index] = level;
  }

  @Override
  public MarketData buildMarketData() {
    // find number & bids & asks
    int bidSize = 0, askSize = 0;
    for (int i = 0; i < bids.length; i++) {
      if (bids[i] == null) {
        break;
      }
      bidSize++;
    }
    for (int i = 0; i < asks.length; i++) {
      if (asks[i] == null) {
        break;
      }
      askSize++;
    }
    bidSize = Math.min(bidSize, AppConstants.DEFAULT_DEPTH);
    askSize = Math.min(askSize, AppConstants.DEFAULT_DEPTH);
    int depth = Math.max(bidSize, askSize);
    MarketData md = new MarketData();
    md.setDepth(depth);
    md.setSymbol(symbol);
    md.setTransactTime(System.currentTimeMillis());

    BigDecimal[][] bids = new BigDecimal[bidSize][];
    BigDecimal[][] asks = new BigDecimal[askSize][];
    for (int i = 0; i < bidSize; i++) {
      BigDecimal cumulativeQuantity = BigDecimal.ZERO;
      PriceLevelImpl level = this.bids[i];
      for (Order order : level.getOrders()) {
        cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
      }
      bids[i] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
    }
    for (int i = 0; i < askSize; i++) {
      BigDecimal cumulativeQuantity = BigDecimal.ZERO;
      PriceLevelImpl level = this.asks[i];
      for (Order order : level.getOrders()) {
        cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
      }
      asks[i] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
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
    for (int i = 0; i < bids.length; i++) {
      PriceLevelImpl level = bids[i];
      orders.addAll(level.getOrders());
    }
    for (int i = 0; i < asks.length; i++) {
      PriceLevelImpl level = asks[i];
      orders.addAll(level.getOrders());
    }
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