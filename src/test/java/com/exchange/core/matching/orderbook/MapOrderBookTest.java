package com.exchange.core.matching.orderbook;

import com.exchange.core.config.AppConstants;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Or;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class MapOrderBookTest {
    private final String SYMBOL = "BTC/USDT";

    private OrderBook getNewOrderBook(){
        return new MapOrderBook(SYMBOL);
    }

    @Test
    public void addOrderTest(){
        OrderBook ob = getNewOrderBook();
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
        OrderBook ob = getNewOrderBook();

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
        OrderBook ob = getNewOrderBook();

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
        OrderBook ob = getNewOrderBook();
        Order buy = getLimitBuy();
        Order sell = getLimitBuy();
        sell.setSide(OrderSide.SELL);
        List<Trade> trades = ob.match(sell);
        Assertions.assertNotNull(trades);
        Assertions.assertEquals(0, trades.size(), "trades number should be 0 for empty orderbook");

        ob.add(buy);
        trades = ob.match(sell);
        Assertions.assertEquals(1, trades.size(), "should be 1 trade");
        Trade trade = trades.get(0);
        Assertions.assertEquals(BigDecimal.ZERO, trade.getTaker().getLeavesQty(), "leavesQty should be 0 for taker");
        Assertions.assertEquals(BigDecimal.ZERO, trade.getMaker().getLeavesQty(), "leavesQty should be 0 for taker");
        Assertions.assertEquals(new BigDecimal("10"), trade.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("100"), trade.getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("1000"), trade.getTradeAmount(), "tradeAmount should be 1000");
    }

    @Test
    public void limitBuyOrderTest(){
        OrderBook ob = getNewOrderBook();
        add3SellOrders(ob);

        Order buy = getLimitBuy();
        buy.setPrice(new BigDecimal("180"));
        buy.setLeavesQty(new BigDecimal("40"));
        List<Trade> trades = ob.match(buy);
        Assertions.assertEquals(2, trades.size(), "should be 2 trades");
        Trade trade1 = trades.get(0);
        Assertions.assertEquals(new BigDecimal("10"), trade1.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("100"), trade1.getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("1000"), trade1.getTradeAmount(), "tradeAmount should be 1000");
        Trade trade2 = trades.get(1);
        Assertions.assertEquals(new BigDecimal("10"), trade2.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("150"), trade2.getTradePrice(), "tradePrice should be 150");
        Assertions.assertEquals(new BigDecimal("1500"), trade2.getTradeAmount(), "tradeAmount should be 1500");
        Assertions.assertEquals(new BigDecimal("20"), trade2.getTaker().getLeavesQty(), "leavesQty should be 20");
    }

    @Test
    public void limitBuyLessOrderTest(){
        OrderBook ob = getNewOrderBook();
        add3SellOrders(ob);

        Order buy = getLimitBuy();
        buy.setPrice(new BigDecimal("200"));
        buy.setLeavesQty(new BigDecimal("5"));
        List<Trade> trades = ob.match(buy);
        Assertions.assertEquals(1, trades.size(), "should be 1 trade");
        Trade trade = trades.get(0);
        Assertions.assertEquals(new BigDecimal("5"), trade.getTradeQty(), "tradeQty should be 5");
        Assertions.assertEquals(new BigDecimal("100"), trade.getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("500"), trade.getTradeAmount(), "tradeAmount should be 500");
        Assertions.assertEquals(new BigDecimal("0"), trade.getTaker().getLeavesQty(), "leavesQty should be 0");
    }

    @Test
    public void marketBuyOrderTest(){
        OrderBook ob = getNewOrderBook();
        add3SellOrders(ob);

        Order buy = getLimitBuy();
        buy.setType(OrderType.MARKET);
        buy.setLeavesQty(new BigDecimal("3000"));
        List<Trade> trades = ob.match(buy);
        Assertions.assertEquals(3, trades.size(), "should be 3 trades");
        Trade trade1 = trades.get(0);
        Assertions.assertEquals(new BigDecimal("10"), trade1.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("100"), trade1.getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("1000"), trade1.getTradeAmount(), "tradeAmount should be 1000");
        Trade trade2 = trades.get(1);
        Assertions.assertEquals(new BigDecimal("10"), trade2.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("150"), trade2.getTradePrice(), "tradePrice should be 150");
        Assertions.assertEquals(new BigDecimal("1500"), trade2.getTradeAmount(), "tradeAmount should be 1500");
        Trade trade3 = trades.get(2);
        Assertions.assertTrue(trade3.getTradeQty().compareTo(new BigDecimal("2.5")) == 0, "tradeQty should be 2.5");
        Assertions.assertTrue(trade3.getTradePrice().compareTo(new BigDecimal("200")) == 0, "tradePrice should be 200");
        Assertions.assertTrue(trade3.getTradeAmount().compareTo(new BigDecimal("500")) == 0, "tradeAmount should be 500");
        Assertions.assertTrue(trade3.getTaker().getLeavesQty().compareTo(new BigDecimal("0")) == 0, "taker leavesQty should be 0");
        Assertions.assertTrue(trade3.getMaker().getLeavesQty().compareTo(new BigDecimal("7.5")) == 0, "maker leavesQty should be 7.5");
    }
    @Test
    public void marketBuyFloatingPointErrorTest(){
        OrderBook ob = getNewOrderBook();

        Order sell = getLimitBuy();
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal("219"));
        sell.setLeavesQty(new BigDecimal("1"));
        ob.add(sell);

        Order buy = getLimitBuy();
        buy.setType(OrderType.MARKET);
        BigDecimal buyTradeAmount = new BigDecimal("111");
        buy.setLeavesQty(buyTradeAmount);
        List<Trade> trades = ob.match(buy);
        Assertions.assertEquals(1, trades.size(), "should be 1 trade");
        Trade trade = trades.get(0);
        BigDecimal tradeQty = buyTradeAmount.divide(sell.getPrice(), AppConstants.ROUNDING_SCALE, RoundingMode.DOWN);
        Assertions.assertEquals(tradeQty, trade.getTradeQty(), "tradeQty should be "+tradeQty);
        Assertions.assertEquals(sell.getPrice(), trade.getTradePrice(), "tradePrice should be "+sell.getPrice());
        Assertions.assertEquals(buyTradeAmount, trade.getTradeAmount(), "tradeAmount should be "+buyTradeAmount);
        BigDecimal makerLeavesQty = new BigDecimal("1").subtract(tradeQty);
        Assertions.assertEquals(makerLeavesQty, trade.getMaker().getLeavesQty(), "taker leavesQty should be "+makerLeavesQty);
        Assertions.assertEquals(new BigDecimal("0"), trade.getTaker().getLeavesQty(), "taker leavesQty should be 0");
    }

    @Test
    public void limitSellOrderTest(){
        OrderBook ob = getNewOrderBook();
        add3BuyOrders(ob);

        Order sell = getLimitBuy();
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal("85"));
        sell.setLeavesQty(new BigDecimal("30"));
        List<Trade> trades = ob.match(sell);
        Assertions.assertEquals(2, trades.size(), "should be 2 trades");
        Trade trade1 = trades.get(0);
        Assertions.assertEquals(new BigDecimal("10"), trade1.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("100"), trade1.getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("1000"), trade1.getTradeAmount(), "tradeAmount should be 1000");
        Assertions.assertEquals(new BigDecimal("0"), trade1.getMaker().getLeavesQty(), "maker leavesQty should be 0");
        Trade trade2 = trades.get(1);
        Assertions.assertEquals(new BigDecimal("10"), trade2.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("90"), trade2.getTradePrice(), "tradePrice should be 90");
        Assertions.assertEquals(new BigDecimal("900"), trade2.getTradeAmount(), "tradeAmount should be 900");
        Assertions.assertEquals(new BigDecimal("0"), trade2.getMaker().getLeavesQty(), "maker leavesQty should be 0");
        Assertions.assertEquals(new BigDecimal("10"), trade2.getTaker().getLeavesQty(), "taker leavesQty should be 10");
    }

    @Test
    public void marketSellOrderTest(){
        OrderBook ob = getNewOrderBook();
        add3BuyOrders(ob);

        Order sell = getLimitBuy();
        sell.setType(OrderType.MARKET);
        sell.setSide(OrderSide.SELL);
        sell.setLeavesQty(new BigDecimal("40"));
        List<Trade> trades = ob.match(sell);
        Assertions.assertEquals(3, trades.size(), "should be 3 trades");
        Trade trade1 = trades.get(0);
        Assertions.assertEquals(new BigDecimal("10"), trade1.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("100"), trade1.getTradePrice(), "tradePrice should be 100");
        Assertions.assertEquals(new BigDecimal("1000"), trade1.getTradeAmount(), "tradeAmount should be 1000");
        Assertions.assertEquals(new BigDecimal("0"), trade1.getMaker().getLeavesQty(), "maker leavesQty should be 0");
        Trade trade2 = trades.get(1);
        Assertions.assertEquals(new BigDecimal("10"), trade2.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("90"), trade2.getTradePrice(), "tradePrice should be 90");
        Assertions.assertEquals(new BigDecimal("900"), trade2.getTradeAmount(), "tradeAmount should be 900");
        Assertions.assertEquals(new BigDecimal("0"), trade2.getMaker().getLeavesQty(), "maker leavesQty should be 0");
        Trade trade3 = trades.get(2);
        Assertions.assertEquals(new BigDecimal("10"), trade3.getTradeQty(), "tradeQty should be 10");
        Assertions.assertEquals(new BigDecimal("80"), trade3.getTradePrice(), "tradePrice should be 80");
        Assertions.assertEquals(new BigDecimal("800"), trade3.getTradeAmount(), "tradeAmount should be 800");
        Assertions.assertEquals(new BigDecimal("0"), trade3.getMaker().getLeavesQty(), "maker leavesQty should be 0");
        Assertions.assertEquals(new BigDecimal("10"), trade3.getTaker().getLeavesQty(), "maker leavesQty should be 10");

    }

    private void add3SellOrders(OrderBook ob){
        Order sell = getLimitBuy();
        sell.setSide(OrderSide.SELL);
        ob.add(sell);

        Order sell150 = getLimitBuy();
        sell150.setSide(OrderSide.SELL);
        sell150.setPrice(new BigDecimal("150"));
        ob.add(sell150);

        Order sell200 = getLimitBuy();
        sell200.setSide(OrderSide.SELL);
        sell200.setPrice(new BigDecimal("200"));
        ob.add(sell200);
    }

    private void add3BuyOrders(OrderBook ob){
        Order buy100 = getLimitBuy();
        ob.add(buy100);

        Order buy90 = getLimitBuy();
        buy90.setPrice(new BigDecimal("90"));
        ob.add(buy90);

        Order buy80 = getLimitBuy();
        buy80.setPrice(new BigDecimal("80"));
        ob.add(buy80);
    }

    private Order getLimitBuy(){
        Order order = new Order();
        order.setSymbol(SYMBOL);
        order.setType(OrderType.LIMIT);
        order.setSide(OrderSide.BUY);
        order.setPrice(new BigDecimal("100"));
        order.setLeavesQty(new BigDecimal("10"));
        return order;
    }
}