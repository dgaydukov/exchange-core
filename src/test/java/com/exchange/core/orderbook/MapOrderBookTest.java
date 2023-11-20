package com.exchange.core.orderbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class MapOrderBookTest {
    private OrderBook orderBook;

    @BeforeEach
    void beforeEach(){

    }

    @Test
    public void test(){
        BigDecimal amount = BigDecimal.ZERO;
        System.out.println(amount);
        amount.add(new BigDecimal("100"));
        amount.subtract(new BigDecimal("30"));
        System.out.println(amount);
    }

}