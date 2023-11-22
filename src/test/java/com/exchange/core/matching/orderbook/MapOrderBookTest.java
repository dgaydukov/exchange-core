package com.exchange.core.matching.orderbook;

import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public class MapOrderBookTest {
    private final String SYMBOL = "BTC/USDT";

    @Test
    public void addOrderTest(){
        OrderBook ob = new MapOrderBook(SYMBOL);
        MarketData md = ob.buildMarketData();
        Assertions.assertNotNull(md);
        Assertions.assertEquals(SYMBOL, md.getSymbol(), "symbols mismatch");
        Assertions.assertEquals(0, md.getDepth(), "depth should be 0 for empty orderbook");
        Assertions.assertEquals(0, md.getBids().length, "bids should be 0 for empty orderbook");
        Assertions.assertEquals(0, md.getAsks().length, "asks should be 0 for empty orderbook");

        Order buy = getLimitBuy();
        ob.add(buy);
        md = ob.buildMarketData();
        Assertions.assertEquals(1, md.getDepth(), "depth should be 1");
        Assertions.assertEquals(1, md.getBids().length, "bids should be 1");
        Assertions.assertEquals(0, md.getAsks().length, "asks should be 0");
    }

    @Test
    public void bidsMarketDataTest(){
        OrderBook ob = new MapOrderBook(SYMBOL);

        Order buy = getLimitBuy();
        ob.add(buy);
        Order buy90 = getLimitBuy();
        buy90.setPrice(new BigDecimal("90"));
        buy90.setLeavesQty(new BigDecimal("20"));
        ob.add(buy90);
        Order buy80 = getLimitBuy();
        buy80.setPrice(new BigDecimal("80"));
        buy80.setLeavesQty(new BigDecimal("30"));
        ob.add(buy80);
        Order buy120 = getLimitBuy();
        buy120.setPrice(new BigDecimal("120"));
        ob.add(buy120);

        MarketData md = ob.buildMarketData();
        Assertions.assertEquals(4, md.getDepth(), "depth should be 4");
        Assertions.assertEquals(4, md.getBids().length, "bids should be 4");
        BigDecimal[][] bids = new BigDecimal[][]{
                {buy120.getPrice(), buy120.getLeavesQty()},
                {buy.getPrice(), buy.getLeavesQty()},
                {buy90.getPrice(), buy90.getLeavesQty()},
                {buy80.getPrice(), buy80.getLeavesQty()},
        };
        Assertions.assertArrayEquals(bids, md.getBids(), "bids mismatch");
        Assertions.assertEquals(0, md.getAsks().length, "asks should be 0");
    }

    @Test
    public void asksMarketDataTest(){
        OrderBook ob = new MapOrderBook(SYMBOL);

        Order sell = getLimitBuy();
        sell.setSide(OrderSide.SELL);
        ob.add(sell);
        Order sell90 = getLimitBuy();
        sell90.setSide(OrderSide.SELL);
        sell90.setPrice(new BigDecimal("90"));
        sell90.setLeavesQty(new BigDecimal("20"));
        ob.add(sell90);
        Order sell80 = getLimitBuy();
        sell80.setSide(OrderSide.SELL);
        sell80.setPrice(new BigDecimal("80"));
        sell80.setLeavesQty(new BigDecimal("30"));
        ob.add(sell80);
        Order sell120 = getLimitBuy();
        sell120.setSide(OrderSide.SELL);
        sell120.setPrice(new BigDecimal("120"));
        ob.add(sell120);

        MarketData md = ob.buildMarketData();
        Assertions.assertEquals(4, md.getDepth(), "depth should be 4");
        Assertions.assertEquals(4, md.getAsks().length, "asks should be 4");
        BigDecimal[][] asks = new BigDecimal[][]{
                {sell80.getPrice(), sell80.getLeavesQty()},
                {sell90.getPrice(), sell90.getLeavesQty()},
                {sell.getPrice(), sell.getLeavesQty()},
                {sell120.getPrice(), sell120.getLeavesQty()},
        };
        Assertions.assertArrayEquals(asks, md.getAsks(), "asks mismatch");
        Assertions.assertEquals(0, md.getBids().length, "bids should be 0");
    }

    @Test
    public void samePriceMatchingTest(){
        OrderBook ob = new MapOrderBook(SYMBOL);
        Order buy = getLimitBuy();
        Order sell = getLimitBuy();
        sell.setSide(OrderSide.SELL);
        List<Trade> trades = ob.match(sell);
        Assertions.assertNotNull(trades);
        Assertions.assertEquals(0, trades.size(), "trades number should be 0 for empty orderbook");

        ob.add(buy);
        trades = ob.match(sell);
        Assertions.assertEquals(1, trades.size(), "should be 1 trade");
        Assertions.assertEquals(BigDecimal.ZERO, trades.get(0).getTaker().getLeavesQty(), "leavesQty should be 0 for taker");
        Assertions.assertEquals(BigDecimal.ZERO, trades.get(0).getMaker().getLeavesQty(), "leavesQty should be 0 for taker");
        Assertions.assertEquals(new BigDecimal("10"), trades.get(0).getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("100"), trades.get(0).getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("1000"), trades.get(0).getTradeAmount(), "tradeAmount should be 1000");
    }

    public Order getLimitBuy(){
        Order order = new Order();
        order.setSymbol(SYMBOL);
        order.setType(OrderType.LIMIT);
        order.setSide(OrderSide.BUY);
        order.setPrice(new BigDecimal("100"));
        order.setLeavesQty(new BigDecimal("10"));
        return order;
    }
}