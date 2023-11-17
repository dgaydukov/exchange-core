package com.exchange.core;

import com.exchange.core.model.Message;
import com.exchange.core.model.Order;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.orderbook.MatchingEngine;
import com.exchange.core.orderbook.SleepWaitStrategy;
import com.exchange.core.orderbook.WaitStrategy;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class App {
    public static void main(String[] args) {
        final String BTC = "BTC/USDT";
        final String ETH = "ETH/USDT";
        final WaitStrategy wait = new SleepWaitStrategy();
        final Queue<Message> inbound = new LinkedList<>();
        final Queue<Message> outbound = new LinkedList<>();
        List<String> symbols = List.of(BTC, ETH);
        // listener for outbound messages
        new Thread(()->{
            while (true){
                Message msg = outbound.poll();
                if (msg != null){
                    System.out.println("outbound => " + msg);
                }
                wait.idle();
            }
        }, "OutBoundQueueListener").start();

        // start matching engine
        MatchingEngine me = new MatchingEngine(symbols, inbound, outbound);
        me.start();

        // adding 2 orders
        System.out.println("adding 2 orders...");
        Order buy = new Order();
        buy.setSymbol(BTC);
        buy.setSide(OrderSide.BUY);
        buy.setPrice(new BigDecimal("100"));
        buy.setOrderQty(new BigDecimal("5.5"));
        inbound.add(buy);
        Order sell = new Order();
        sell.setSymbol(BTC);
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal("100"));
        sell.setOrderQty(new BigDecimal("3"));
        inbound.add(sell);
    }
}