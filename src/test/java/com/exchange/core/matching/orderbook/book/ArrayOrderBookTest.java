package com.exchange.core.matching.orderbook.book;

import com.exchange.core.MockData;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.msg.Order;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class ArrayOrderBookTest {
    private OrderBook ob;

    @BeforeEach
    public void beforeEach(){
        ob = new ArrayOrderBook(MockData.SYMBOL);
    }

    @Test
    public void orderBookBidsArrayOverflowTest(){
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
        Assertions.assertEquals(ex.getMessage(), "PriceLevel Array Overflow.");
    }

    @Test
    public void orderBookAsksArrayOverflowTest(){
        for (int i = 1; i <= 1024; i++){
            Order sell = MockData.getLimitBuy();
            sell.setSide(OrderSide.SELL);
            sell.setPrice(new BigDecimal(i));
            Assertions.assertTrue(ob.add(sell), "order should be added successfully");
        }
        Order sell = MockData.getLimitBuy();
        sell.setPrice(new BigDecimal(1025));
        AppException ex = Assertions.assertThrows(AppException.class, () -> {
            ob.add(sell);
        });
        Assertions.assertEquals(ex.getMessage(), "PriceLevel Array Overflow.");
    }
}
