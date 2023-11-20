package com.exchange.core;

import com.exchange.core.model.AccountBalance;
import com.exchange.core.model.Message;
import com.exchange.core.model.Order;
import com.exchange.core.model.SymbolConfigMessage;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.orderbook.MatchingEngine;
import com.exchange.core.orderbook.SleepWaitStrategy;
import com.exchange.core.orderbook.WaitStrategy;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;

public class App {
    public static void main(String[] args) {
        final String BASE = "BTC";
        final String QUOTE = "USDT";
        final String SYMBOL = BASE+"/"+QUOTE;
        SymbolConfigMessage symbolMsg = new SymbolConfigMessage();
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
        AccountBalance user1 = new AccountBalance();
        user1.setAccountId(1);
        user1.setAsset(QUOTE);
        user1.setAmount(new BigDecimal("1000"));
        inbound.add(user1);
        AccountBalance user2 = new AccountBalance();
        user2.setAccountId(2);
        user2.setAsset(BASE);
        user2.setAmount(new BigDecimal("10"));
        inbound.add(user2);

        // adding 2 orders
        System.out.println("adding 2 orders...");
        Order buy = new Order();
        buy.setSymbol(SYMBOL);
        buy.setType(OrderType.LIMIT);
        buy.setSide(OrderSide.BUY);
        buy.setPrice(new BigDecimal("100"));
        buy.setOrderQty(new BigDecimal("5.5"));
        buy.setAccount(user1.getAccountId());
        inbound.add(buy);
        Order sell = new Order();
        sell.setSymbol(SYMBOL);
        buy.setType(OrderType.LIMIT);
        sell.setSide(OrderSide.SELL);
        sell.setPrice(new BigDecimal("100"));
        sell.setOrderQty(new BigDecimal("3"));
        sell.setAccount(user2.getAccountId());
        inbound.add(sell);
    }
}