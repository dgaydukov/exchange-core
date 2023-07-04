package com.exchange.core.model;

import lombok.Data;

@Data
public class Execution {
    private long id;
    private long orderId;
    private long matchedOrderId;
    private boolean isMaker;
    private int securityId;
    private int quantity;
    private int quantityScale;
    private int price;
    private int priceScale;

    public Execution(long orderId, long matchedOrderId, boolean isMaker, int securityId, int quantity, int price){
        this.orderId = orderId;
        this.matchedOrderId = matchedOrderId;
        this.isMaker = isMaker;
        this.securityId = securityId;
        this.quantity = quantity;
        this.price = price;
    }
}