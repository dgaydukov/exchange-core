package com.exchange.core.orderbook;

import com.exchange.core.account.AccountRepository;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.SymbolConfigMessage;

import java.math.BigDecimal;
import java.util.*;

public class ArrayOrderBook implements OrderBook{

    private final NavigableMap<BigDecimal, List<Order>> bidsMap;
    private final NavigableMap<BigDecimal, List<Order>> asksMap;
    private final SymbolConfigMessage symbolConfig;
    private final GlobalCounter counter;
    private final Queue<Message> outbound;
    private final AccountRepository accountRepository;


    public ArrayOrderBook(SymbolConfigMessage symbol, GlobalCounter counter, Queue<Message> outbound, AccountRepository accountRepository) {

        this.symbolConfig = symbol;
        this.counter = counter;
        this.outbound = outbound;
        this.accountRepository = accountRepository;
    }

    @Override
    public void addOrder(Order order) {

    }
}