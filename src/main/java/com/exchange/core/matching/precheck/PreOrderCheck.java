package com.exchange.core.matching.precheck;

import com.exchange.core.model.msg.Order;

public interface PreOrderCheck {
    boolean validateOrder(Order order);

    void updateNewOrder(Order order);

    void lockBalance(Order order);
}