package com.exchange.core.matching.snapshot.snapshotable;

import com.exchange.core.MockData;
import com.exchange.core.matching.orderbook.MapOrderBook;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.snapshot.Snapshotable;
import com.exchange.core.model.SnapshotItem;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.SnapshotType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.AccountRepositoryImpl;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OrderBookSnapshotableTest {

  private final String SYMBOL = "BTC-USDT";
  private Snapshotable snapshotable;

  @BeforeEach
  public void initNewInstance() {
    snapshotable = new MapOrderBook(SYMBOL);
  }

  @Test
  public void snapshotTypeTest() {
    Assertions.assertEquals(SnapshotType.ORDER_BOOK, snapshotable.getType(),
        "snapshot type mismatch");
  }

  @Test
  public void createSnapshotTest() {
    OrderBook ob = (OrderBook) snapshotable;
    Order buy100 = MockData.getLimitBuy();
    buy100.setPrice(new BigDecimal("100"));
    Order sell120 = MockData.getLimitBuy();
    sell120.setSide(OrderSide.SELL);
    sell120.setPrice(new BigDecimal("120"));
    ob.add(buy100);
    ob.add(sell120);

    SnapshotItem item = snapshotable.create();
    Assertions.assertEquals(SnapshotType.ORDER_BOOK, item.getType(), "snapshot type mismatch");
    Assertions.assertTrue(item.getData() instanceof List);
    List<Order> orders = (List<Order>) item.getData();
    Assertions.assertEquals(2, orders.size(), "should be 2 orders");
    Order buy, sell;
    if (orders.get(0).getSide() == OrderSide.BUY) {
      buy = orders.get(0);
      sell = orders.get(1);
    } else {
      buy = orders.get(1);
      sell = orders.get(0);
    }
    Assertions.assertEquals(buy100, buy, "buy order mismatch");
    Assertions.assertEquals(sell120, sell, "sell order mismatch");
  }

  @Test
  public void loadSnapshotTest() {
    SnapshotItem item = new SnapshotItem();
    item.setType(SnapshotType.ORDER_BOOK);
    List<Order> orders = new ArrayList<>();
    Order buy100 = MockData.getLimitBuy();
    buy100.setPrice(new BigDecimal("100"));
    buy100.setLeavesQty(buy100.getOrderQty());
    Order sell120 = MockData.getLimitBuy();
    sell120.setSide(OrderSide.SELL);
    sell120.setPrice(new BigDecimal("120"));
    sell120.setLeavesQty(sell120.getOrderQty());
    orders.add(buy100);
    orders.add(sell120);
    item.setData(orders);
    snapshotable.load(item);

    OrderBook ob = (OrderBook) snapshotable;
    MarketData md = ob.buildMarketData();
    Assertions.assertEquals(SYMBOL, md.getSymbol(), "symbol mismatch");
    Assertions.assertEquals(1, md.getDepth(), "depth should be 1");
    Assertions.assertEquals(1, md.getBids().length, "bids size should be 1");
    Assertions.assertEquals(1, md.getAsks().length, "asks size should be 1");
    BigDecimal[][] bids = new BigDecimal[][]{
        {buy100.getPrice(), buy100.getLeavesQty()}
    };
    Assertions.assertArrayEquals(bids, md.getBids(), "bids mismatch");
    BigDecimal[][] asks = new BigDecimal[][]{
        {sell120.getPrice(), sell120.getLeavesQty()}
    };
    Assertions.assertArrayEquals(asks, md.getAsks(), "asks mismatch");
  }
}