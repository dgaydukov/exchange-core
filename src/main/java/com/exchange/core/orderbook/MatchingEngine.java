package com.exchange.core.orderbook;

import com.exchange.core.account.AccountRepository;
import com.exchange.core.account.AccountRepositoryImpl;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.*;
import com.exchange.core.orderbook.map.MapOrderBook;
import com.exchange.core.orderbook.post.PostOrderCheck;
import com.exchange.core.orderbook.post.PostOrderCheckImpl;
import com.exchange.core.orderbook.pre.PreOrderCheck;
import com.exchange.core.orderbook.pre.PreOrderCheckImpl;

import java.util.*;

public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks;
    private final Queue<Message> inbound;
    private final Queue<Message> outbound;
    private final AccountRepository accountRepository;
    private final GlobalCounter counter;

    public MatchingEngine(Queue<Message> inbound, Queue<Message> outbound) {
        accountRepository = new AccountRepositoryImpl();
        counter = new SimpleGlobalCounter();
        orderBooks = new HashMap<>();
        this.inbound = inbound;
        this.outbound = outbound;
    }

    public void start() {
        System.out.println("Starting matching engine...");
        new Thread(this::run, "MatchingThread").start();
    }

    private void run() {
        WaitStrategy wait = new SleepWaitStrategy();
        while (true) {
            Message msg = inbound.poll();
            if (msg != null) {
                System.out.println("inbound => " + msg);
                try {
                    process(msg);
                } catch (Exception ex) {
                    outbound.add(new ErrorMessage(ex.getMessage()));
                }
            }
            wait.idle();
        }
    }

    private void addOrderBook(SymbolConfigMessage msg) {
        PreOrderCheck preOrderCheck = new PreOrderCheckImpl(msg, counter, accountRepository, outbound);
        PostOrderCheck postOrderCheck = new PostOrderCheckImpl(msg, counter, accountRepository, outbound);
        final String symbol = msg.getSymbol();
        orderBooks.put(symbol, new MapOrderBook(symbol, preOrderCheck, postOrderCheck));
    }

    private void process(Message msg) {
        if (msg instanceof SymbolConfigMessage symbol) {
            addOrderBook(symbol);
        } else if (msg instanceof Order order) {
            final String symbol = order.getSymbol();
            if (symbol == null) {
                throw new AppException("Symbol not found for oder: msg=" + msg);
            }
            OrderBook ob = orderBooks.get(order.getSymbol());
            if (ob == null) {
                throw new AppException("OrderBook not found for oder: msg=" + msg);
            }
            ob.addOrder(order);
        } else if (msg instanceof AccountBalance ab) {
            accountRepository.addBalance(ab);
        } else {
            throw new AppException("Undefined message: msg=" + msg);
        }
    }
}