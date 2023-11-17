package com.exchange.core.orderbook;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.ErrorMessage;
import com.exchange.core.model.Message;
import com.exchange.core.model.Order;

import java.util.*;

public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks;
    private final Queue<Message> inbound;
    private final Queue<Message> outbound;
    private long orderId;

    public MatchingEngine(List<String> symbols, Queue<Message> inbound, Queue<Message> outbound) {
        orderBooks = new HashMap<>();
        symbols.forEach(s -> orderBooks.put(s, new MapOrderBook(outbound)));
        this.inbound = inbound;
        this.outbound = outbound;
        this.start();
    }

    private void start(){
        System.out.println("Starting matching thread");
        new Thread(this::run, "MatchingThread").start();
    }

    private void run() {
        while (true) {
            Message msg = inbound.poll();
            try {
                process(msg);
            } catch (Exception ex) {
                outbound.add(new ErrorMessage(ex.getMessage()));
            }
        }
    }

    private void process(Message msg){
        if (msg != null){
            if (msg instanceof Order order){
                order.setOrderId(++orderId);
                final String symbol = order.getSymbol();
                if (symbol == null){
                    throw new AppException("Symbol not found for oder: msg=" + msg);
                }
                OrderBook ob = orderBooks.get(order.getSymbol());
                if (ob == null){
                    throw new AppException("OrderBook not found for oder: msg=" + msg);
                }
                ob.addOrder(order);
            } else {
                throw new AppException("Undefined message: msg=" + msg);
            }
        }
    }
}
