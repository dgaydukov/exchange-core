package com.exchange.core.orderbook.array;

import com.exchange.core.account.AccountRepository;
import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.SymbolConfigMessage;
import com.exchange.core.orderbook.GlobalCounter;
import com.exchange.core.orderbook.OrderBook;

import java.math.BigDecimal;
import java.util.*;

public class ArrayOrderBook implements OrderBook {
    private final int DEFAULT_PRICE_LEVEL_SIZE = 1024;
    // sorted in descending order => first bid is the highest price
    private final PriceLevel[] bids = new PriceLevel[DEFAULT_PRICE_LEVEL_SIZE];
    // sorted in ascending order => first ask is the lowest price
    private final PriceLevel[] asks = new PriceLevel[DEFAULT_PRICE_LEVEL_SIZE];
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
        match(order);
        addToOrderBook(order);
    }

    private void match(Order taker) {
        if (taker.getSide() == OrderSide.BUY){
            for(int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++){
                PriceLevel level = asks[i];
                if (level == null || taker.getPrice().compareTo(level.getPrice()) > 0){
                    break;
                }
                matchLevel(level);
            }
        } else{
            for(int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++){
                PriceLevel level = bids[i];
                if (level == null || taker.getPrice().compareTo(level.getPrice()) < 0){
                    break;
                }
                matchLevel(level);
            }
        }
    }

    private void matchLevel(PriceLevel level){
        while (level.hasNext()){
            Order maker = level.getNext();

            if(maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0){
                level.removeNext();
            }
        }
    }

    private void addToOrderBook(Order order) {
        if (order.getSide() == OrderSide.BUY) {
            for (int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++) {
                PriceLevel level = bids[i];
                if (level == null) {
                    bids[i] = new PriceLevel(order);
                    break;
                }
                if (order.getPrice().compareTo(level.getPrice()) == 0) {
                    level.addOrder(order);
                    break;
                }
                if (order.getPrice().compareTo(level.getPrice()) > 0) {
                    // move remaining array one index left
                    for (int j = DEFAULT_PRICE_LEVEL_SIZE; j >= i; j--) {
                        if (bids[j] != null){
                            // array price level overflow
                            throw new AppException("price level overflow");
                        }
                        if (j == i){
                            bids[j] = new PriceLevel(order);
                        } else {
                            bids[j] = bids[j+1];
                        }
                    }
                    break;
                }
            }
        } else {
            for (int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++){
                PriceLevel level = asks[i];
                if (level == null) {
                    asks[i] = new PriceLevel(order);
                    break;
                }
                if (order.getPrice().compareTo(level.getPrice()) == 0) {
                    level.addOrder(order);
                    break;
                }
                if (order.getPrice().compareTo(level.getPrice()) < 0) {
                    // move remaining array one index left
                    for (int j = DEFAULT_PRICE_LEVEL_SIZE; j >= i; j--) {
                        if (asks[j] != null){
                            // array price level overflow
                            throw new AppException("price level overflow");
                        }
                        if (j == i){
                            asks[j] = new PriceLevel(order);
                        } else {
                            asks[j] = asks[j+1];
                        }
                    }
                    break;
                }
            }
        }
    }
}