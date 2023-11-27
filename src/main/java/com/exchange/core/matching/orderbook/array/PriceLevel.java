package com.exchange.core.matching.orderbook.array;

import com.exchange.core.exceptions.AppException;
import com.exchange.core.model.msg.Order;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.*;

public class PriceLevel {
    @Getter
    private BigDecimal price;
    private List<Order> orders;
    private int index;

    public PriceLevel(Order order){
        price = order.getPrice();
        orders = new ArrayList<>();
        addOrder(order);
    }

    public void addOrder(Order order){
        if (price.compareTo(order.getPrice()) != 0){
            throw new AppException("Fail to add order: price mismatch");
        }
        orders.add(order);
    }

    public List<Order> getOrders(){
        return orders;
    }
}