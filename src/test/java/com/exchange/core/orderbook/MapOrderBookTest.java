package com.exchange.core.orderbook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.TreeMap;

public class MapOrderBookTest {
    private OrderBook orderBook;

    @BeforeEach
    void beforeEach(){

    }

    @Test
    public void test(){
        TreeMap<Integer, String> db = new TreeMap<Integer, String>();
        db.put(1, "1000");
        db.put(2, "1011");
        db.put(3, "1102");
        db.put(4, "2023");
        db.put(5, "2034");
        System.out.println(db.tailMap(3)+" => "+db.headMap(3));

    }

}