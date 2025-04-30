package com.exchange.core.matching.orderbook.book;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * Specific tests for only ArrayOrderBook
 */
public class ArrayOrderBookTest {
    private OrderBook ob;

    @BeforeEach
    public void beforeEach(){
        ob = new ArrayOrderBook(MockData.SYMBOL);
    }

    @Test
    public void orderBookBidsAscArrayOverflowTest(){
        for (int i = 1; i <= 1024; i++){
            Order buy = MockData.getLimitBuy();
            buy.setPrice(new BigDecimal(i));
            Assertions.assertTrue(ob.add(buy), "order should be added successfully");
        }
        Order buy = MockData.getLimitBuy();
        buy.setPrice(new BigDecimal(1025));
        AppException ex = Assertions.assertThrows(AppException.class, () -> {
            ob.add(buy);
        });
        Assertions.assertEquals(ex.getMessage(), "PriceLevel array overflow: failed to move right");
    }

    @Test
    public void orderBookBidsDescArrayOverflowTest(){
        for (int i = 1024; i > 0; i--){
            Order buy = MockData.getLimitBuy();
            buy.setPrice(new BigDecimal(i));
            Assertions.assertTrue(ob.add(buy), "order should be added successfully");
        }
        Order buy = MockData.getLimitBuy();
        buy.setPrice(new BigDecimal("0.5"));
        AppException ex = Assertions.assertThrows(AppException.class, () -> {
            ob.add(buy);
        });
        Assertions.assertEquals(ex.getMessage(), "PriceLevel array overflow: failed to add");
    }



    @Test
    public void orderBookSellsAscArrayOverflowTest(){
        for (int i = 1; i <= 1024; i++){
            Order sell = MockData.getLimitBuy();
            sell.setSide(OrderSide.SELL);
            sell.setPrice(new BigDecimal(i));
            Assertions.assertTrue(ob.add(sell), "order should be added successfully");
        }
        Order sell = MockData.getLimitBuy();
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal(1025));
        AppException ex = Assertions.assertThrows(AppException.class, () -> {
            ob.add(sell);
        });
        Assertions.assertEquals(ex.getMessage(), "PriceLevel array overflow: failed to add");
    }

    @Test
    public void orderBookSellsDescArrayOverflowTest(){
        for (int i = 1024; i > 0; i--){
            Order sell = MockData.getLimitBuy();
            sell.setSide(OrderSide.SELL);
            sell.setPrice(new BigDecimal(i));
            Assertions.assertTrue(ob.add(sell), "order should be added successfully");
        }
        Order sell = MockData.getLimitBuy();
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal("0.5"));
        AppException ex = Assertions.assertThrows(AppException.class, () -> {
            ob.add(sell);
        });
        Assertions.assertEquals(ex.getMessage(), "PriceLevel array overflow: failed to move right");
    }

    @Test
    public void orderBookMatchOverflowTest(){
        for (int i = 1; i <= 1024; i++){
            Order buy = MockData.getLimitBuy();
            buy.setPrice(new BigDecimal(i));
            buy.setLeavesQty(buy.getOrderQty());
            Assertions.assertTrue(ob.add(buy), "order should be added successfully");
        }
        // match 1 sell order for all possible qty
        Order sell = MockData.getLimitBuy();
        sell.setSide(OrderSide.SELL);
        sell.setType(OrderType.MARKET);
        sell.setLeavesQty(new BigDecimal(10 * 1024));
        ob.match(sell);

        // now we should be able to add again 1024 price levels
        for (int i = 1; i <= 1024; i++){
            Order buy = MockData.getLimitBuy();
            buy.setPrice(new BigDecimal(i));
            buy.setLeavesQty(buy.getOrderQty());
            Assertions.assertTrue(ob.add(buy), "order should be added successfully");
        }
    }
}
