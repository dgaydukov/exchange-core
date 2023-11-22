package com.exchange.core;

import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.matching.MatchingEngine;
import com.exchange.core.matching.waitstrategy.SleepWaitStrategy;
import com.exchange.core.matching.waitstrategy.WaitStrategy;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;

public class SpotExchange {
    public static void main(String[] args) {
        final String BASE = "BTC";
        final String QUOTE = "USDT";
        final String SYMBOL = BASE+"/"+QUOTE;
        InstrumentConfig symbolMsg = new InstrumentConfig();
        symbolMsg.setBase(BASE);
        symbolMsg.setQuote(QUOTE);
        symbolMsg.setSymbol(SYMBOL);

        final WaitStrategy wait = new SleepWaitStrategy();
        final Queue<Message> inbound = new LinkedList<>();
        final Queue<Message> outbound = new LinkedList<>();
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
        MatchingEngine me = new MatchingEngine(inbound, outbound);
        me.start();

        // adding symbol
        inbound.add(symbolMsg);

        // adding 2 users
        UserBalance userBalance1 = new UserBalance();
        userBalance1.setAccount(1);
        userBalance1.setAsset(QUOTE);
        userBalance1.setAmount(new BigDecimal("1000"));
        inbound.add(userBalance1);
        UserBalance userBalance2 = new UserBalance();
        userBalance2.setAccount(2);
        userBalance2.setAsset(BASE);
        userBalance2.setAmount(new BigDecimal("10"));
        inbound.add(userBalance2);

        // adding 2 orders
        Order buy = new Order();
        buy.setSymbol(SYMBOL);
        buy.setType(OrderType.LIMIT);
        buy.setSide(OrderSide.BUY);
        buy.setPrice(new BigDecimal("100"));
        buy.setOrderQty(new BigDecimal("5.5"));
        buy.setAccount(userBalance1.getAccount());
        inbound.add(buy);
        Order sell = new Order();
        sell.setSymbol(SYMBOL);
        buy.setType(OrderType.LIMIT);
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal("100"));
        sell.setOrderQty(new BigDecimal("3"));
        sell.setAccount(userBalance2.getAccount());
        inbound.add(sell);
    }
}