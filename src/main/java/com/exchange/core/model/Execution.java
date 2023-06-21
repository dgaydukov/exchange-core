package com.exchange.core.model;

import lombok.Data;

@Data
public class Execution {
    private long id;
    private long makerId;
    private long takerId;
    private int securityId;
    private int quantity;
    private int quantityScale;
    private int price;
    private int priceScale;
}