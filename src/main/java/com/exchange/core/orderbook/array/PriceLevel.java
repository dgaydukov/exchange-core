package com.exchange.core.orderbook.array;

import com.exchange.core.model.msg.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;

public class PriceLevel {
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

    public boolean hasNext(){
        return orders.size() > 0;
    }

    public Order getNext(){
        return orders.peek();
    }

    public void removeNext(){
        orders.poll();
    }
}
