package com.exchange.core.orderbook.array;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.msg.Order;
import com.exchange.core.orderbook.OrderBook;
import com.exchange.core.orderbook.post.PostOrderCheck;
import com.exchange.core.orderbook.pre.PreOrderCheck;

import java.math.BigDecimal;
import java.util.*;

public class ArrayOrderBook implements OrderBook {
    private final int DEFAULT_PRICE_LEVEL_SIZE = 1024;
    // sorted in descending order => first bid is the highest price
    private final PriceLevel[] bids = new PriceLevel[DEFAULT_PRICE_LEVEL_SIZE];
    // sorted in ascending order => first ask is the lowest price
    private final PriceLevel[] asks = new PriceLevel[DEFAULT_PRICE_LEVEL_SIZE];
    private final String symbol;
    private final PreOrderCheck preOrderCheck;
    private final PostOrderCheck postOrderCheck;


    public ArrayOrderBook(String symbol, PreOrderCheck preOrderCheck, PostOrderCheck postOrderCheck) {
        this.symbol = symbol;
        this.preOrderCheck = preOrderCheck;
        this.postOrderCheck = postOrderCheck;
    }

    @Override
    public void addOrder(Order order) {
        match(order);
        if (order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            addToOrderBook(order);
        }
    }

    private void match(Order taker) {
        if (taker.getSide() == OrderSide.BUY){
            for(int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++){
                PriceLevel level = asks[i];
                if (level == null || taker.getPrice().compareTo(level.getPrice()) > 0){
                    break;
                }
                match(level);
            }
        } else{
            for(int i = 0; i < DEFAULT_PRICE_LEVEL_SIZE; i++){
                PriceLevel level = bids[i];
                if (level == null || taker.getPrice().compareTo(level.getPrice()) < 0){
                    break;
                }
                match(level);
            }
        }
    }

    private void match(Iterator<Order> iterator){
        while (iterator.hasNext()){
            Order maker = iterator.next();
            if(maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0){
                iterator.remove();
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