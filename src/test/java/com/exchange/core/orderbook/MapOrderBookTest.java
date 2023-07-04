package com.exchange.core.orderbook;


import com.exchange.core.model.Execution;
import com.exchange.core.model.MarketData;
import com.exchange.core.model.Order;
import com.exchange.core.model.enums.Side;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MapOrderBookTest {
    private OrderBook orderBook;

    @BeforeEach
    void beforeEach(){
        orderBook = new MapOrderBook(1, new SimpleGlobalCounter(), execution -> {});
    }

    @Test
    void orderIdUpdatedTest(){
        for(int i = 0; i < 3; i++){
            orderBook.addOrder(buildOrder());
        }
        Assertions.assertNotNull(orderBook.getOrderById(3), "Order with id=3 should exist");
        Assertions.assertNull(orderBook.getOrderById(4), "Order with id=4 should not be in orderbook");
    }

    @Test
    void marketDataTest(){
        Order bid1 = buildOrder();
        Order bid2 = buildOrder();
        Order bid3 = buildOrder();
        bid3.setPrice(105);
        orderBook.addOrder(bid1);
        orderBook.addOrder(bid2);
        orderBook.addOrder(bid3);

        Order ask1 = buildOrder();
        ask1.setSide(Side.SELL);
        ask1.setPrice(95);
        Order ask2 = buildOrder();
        ask2.setSide(Side.SELL);
        ask2.setPrice(90);
        Order ask3 = buildOrder();
        ask3.setSide(Side.SELL);
        ask3.setPrice(90);
        orderBook.addOrder(ask1);
        orderBook.addOrder(ask2);
        orderBook.addOrder(ask3);

        MarketData md = new MarketData();
        int[][] bids = {{100, 20}, {105, 10}};
        int[][] asks = {{95, 10}, {90, 20}};
        md.setBids(bids);
        md.setAsks(asks);
        Assertions.assertEquals(md, orderBook.getOrderBook(), "incorrect market data");
    }

    @Test
    void match2OrdersTest(){
        List<Execution> execList = new ArrayList<>();
        orderBook = new MapOrderBook(1, new SimpleGlobalCounter(), execList::add);

        Order bid = buildOrder(), ask = buildOrder();
        ask.setSide(Side.SELL);
        ask.setQuantity(1);
        orderBook.addOrder(bid);
        orderBook.addOrder(ask);
        Assertions.assertEquals(2, execList.size(), "2 executions should be available");
        Order ask2 = buildOrder();
        ask2.setSide(Side.SELL);
        ask2.setQuantity(1);
        orderBook.addOrder(ask2);
        Assertions.assertEquals(4, execList.size(), "4 executions should be available");
    }

    private Order buildOrder(){
        Order order = new Order();
        order.setQuantity(10);
        order.setPrice(100);
        order.setSide(Side.BUY);
        return order;
    }
}