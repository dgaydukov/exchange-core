package com.exchange.core.matching.orderbook.array;

import com.exchange.core.model.msg.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class PriceLevel implements Iterator<Order> {
    @Getter
    private BigDecimal price;
    private Queue<Order> orders;

    public PriceLevel(Order order){
        price = order.getPrice();
        orders = new LinkedList<>();
        addOrder(order);
    }

    public void addOrder(Order order){
        orders.add(order);
    }

    @Override
    public boolean hasNext(){
        return orders.size() > 0;
    }

    @Override
    public Order next(){
        return orders.peek();
    }

    @Override
    public void remove(){
        orders.poll();
    }
}
