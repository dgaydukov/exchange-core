package com.exchange.core.model;

import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class Order implements Message {
    private String symbol;
    private long orderId;
    private String clOrdId;
    private OrderSide side;
    private OrderType type;
    private BigDecimal orderQty;
    private BigDecimal leavesQty;
    private BigDecimal quoteOrderQty;
    private BigDecimal price;
}