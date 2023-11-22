package com.exchange.core.matching.orderbook;

import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

        Order order = new Order();
    }
}
