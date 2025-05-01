package com.exchange.core.matching.orderbook.book;

import com.exchange.core.config.AppConstants;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.level.LinkedListPriceLevel;
import com.exchange.core.matching.orderbook.level.PriceLevel;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayOrderBook implements OrderBook, Snapshotable {
  // sorted in descending order => first bid is the highest price
  private final int[] bids;
  // sorted in ascending order => first ask is the lowest price
  private final int[] asks;
  private final PriceLevel[] book;

  private final String symbol;
  private final int priceLevelArrayDepth;
  private final Map<Long, Order> orderIdMap = new HashMap<>();


  public ArrayOrderBook(String symbol, int priceLevelArrayDepth, int maxPrice) {
    this.symbol = symbol;
    this.priceLevelArrayDepth = priceLevelArrayDepth;
    bids = new int[priceLevelArrayDepth];
    asks = new int[priceLevelArrayDepth];
    book = new PriceLevel[maxPrice+1];
  }

  public ArrayOrderBook(String symbol) {
    this(symbol, 1_000, 1_000_000);
  }

  @Override
  public List<Trade> match(Order taker) {
    List<Trade> trades = new ArrayList<>();
    if (taker.getSide() == OrderSide.BUY) {
      int posShift = 0;
      for (int i = 0; i < priceLevelArrayDepth; i++) {
        PriceLevel level = book[asks[i]];
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
        // check if level is empty and remove PriceLevel from array
        if (!level.hasNext()){
          posShift++;
        }
      }
      // shift array left for n positions
      if (posShift > 0){
        moveLeft(0, posShift, asks, book);
      }
    } else {
      int posShift = 0;
      for (int i = 0; i < priceLevelArrayDepth; i++) {
        PriceLevel level = book[bids[i]];
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
        // check if level is empty and remove PriceLevel from array
        if (!level.hasNext()){
          posShift++;
        }
      }
      // shift array left for n positions
      if (posShift > 0){
        moveLeft(0, posShift, bids, book);
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
        orderIdMap.remove(maker.getOrderId());
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
        orderIdMap.remove(maker.getOrderId());
        level.remove();
      }
    }
  }

  @Override
  public boolean add(Order order) {
    orderIdMap.put(order.getOrderId(), order);
    int orderPrice = order.getPrice().intValue();
    if (book[orderPrice] != null){
      book[orderPrice].add(order);
      return true;
    }
    if (order.getSide() == OrderSide.BUY) {
      for (int i = 0; i < priceLevelArrayDepth; i++) {
        int price = bids[i];
        if (price == 0) {
          bids[i] = price;
          book[price] = new LinkedListPriceLevel(order);
          return true;
        }
        if (order.getPrice().intValue() > price) {
          moveRight(i, new LinkedListPriceLevel(order), price, bids, book, OrderSide.BUY);
          return true;
        }
      }
    } else {
      for (int i = 0; i < priceLevelArrayDepth; i++) {
        int price = asks[i];
        if (price == 0) {
          asks[i] = price;
          book[price] = new LinkedListPriceLevel(order);
          return true;
        }
        if (order.getPrice().intValue() < price) {
          moveRight(i, new LinkedListPriceLevel(order), price, asks, book, OrderSide.SELL);
          return true;
        }
      }
    }
    throw new AppException("PriceLevel array overflow: failed to add");
  }

  @Override
  public boolean update(Order order) {
    final long orderId = order.getOrderId();
    Order o = orderIdMap.get(orderId);
    if (o == null) {
      return false;
    }
    // update quantity
    o.setQuoteOrderQty(order.getQuoteOrderQty());
    // if price changed, we need to move order into new PriceLevel
    if (order.getPrice().compareTo(o.getPrice()) != 0) {
      remove(orderId);
      add(order);
    }
    return true;
  }

  // remove order from PriceLevel
  @Override
  public boolean remove(long orderId) {
    Order order = orderIdMap.remove(orderId);
    if (order == null){
      return false;
    }
    int price = order.getPrice().intValue();
    PriceLevel level = book[price];
    level.resetIterator();
    level.remove(order);
    // if level has no orders, remove it
    if (!level.hasNext()){
      book[price] = null;
      int[] arr = order.getSide() == OrderSide.BUY ? bids : asks;
      for (int i = 0; i < priceLevelArrayDepth; i++) {
        if (price == arr[i]){
          for (int j = i; j < priceLevelArrayDepth-1; j++){
            arr[j] = arr[j+1];
            if (arr[j] == 0){
              break;
            }
          }
        }
      }
    }
    return true;
  }

  @Override
  public Order getOrder(long orderId) {
    return orderIdMap.get(orderId);
  }

  private void moveLeft(int start, int shift, int[] arr, PriceLevel[] bookArr){
    for (int i = start; i < priceLevelArrayDepth - shift; i++) {
      if (arr[i + shift] == 0){
        break;
      }
      bookArr[arr[i]] = null;
      arr[i] = arr[i + shift];
      arr[i+shift] = 0;
    }
  }

  private void moveRight(int index, PriceLevel level, int price, int[] arr, PriceLevel[] bookArr, OrderSide side) {
    int len = arr.length;
    if (arr[len - 1] != 0) {
      throw new AppException((side == OrderSide.BUY ? "Bids" : "Asks") +
              " PriceLevel array overflow: failed to move right");
    }
    int pos = 0;
    for (int i = index; i < len; i++) {
      if (arr[i] == 0){
        pos = i;
        break;
      }
    }
    for (int i = pos; i > index; i--) {
      arr[i] = arr[i-1];
    }
    arr[index] = price;
    bookArr[price] = level;
  }

  @Override
  public MarketData buildMarketData() {
    // find number & bids & asks
    int bidSize = 0, askSize = 0;
    for (int i = 0; i < bids.length; i++) {
      if (bids[i] == 0) {
        break;
      }
      bidSize++;
    }
    for (int i = 0; i < asks.length; i++) {
      if (asks[i] == 0) {
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
      PriceLevel level = book[this.bids[i]];
      level.resetIterator();
      while (level.hasNext()) {
        cumulativeQuantity = cumulativeQuantity.add(level.next().getLeavesQty());
      }
      bids[i] = new BigDecimal[]{level.getPrice(), cumulativeQuantity};
    }
    for (int i = 0; i < askSize; i++) {
      BigDecimal cumulativeQuantity = BigDecimal.ZERO;
      PriceLevel level = book[this.asks[i]];
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

  @Override
  public SnapshotType getType() {
    return SnapshotType.ORDER_BOOK;
  }

  @Override
  public SnapshotItem create() {
    List<Order> orders = new ArrayList<>();
    for (int price: bids) {
      PriceLevel level = book[price];
      level.resetIterator();
      while (level.hasNext()) {
        orders.add(level.next());
      }
    }
    for (int price: asks) {
      PriceLevel level = book[price];
      level.resetIterator();
      while (level.hasNext()) {
        orders.add(level.next());
      }
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