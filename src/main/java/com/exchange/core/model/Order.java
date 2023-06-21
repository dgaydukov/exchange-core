package com.exchange.core.model;

import com.exchange.core.model.enums.Side;
import lombok.Data;

@Data
public class Order {
    private long orderId;
    private String clOrdId;
    private Side side;
    private int securityId;
    private int quantity;
    private int quantityScale;
    private int price;
    private int priceScale;
}