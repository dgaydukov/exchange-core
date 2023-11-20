package com.exchange.core.model.msg;

import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.Message;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExecReport implements Message {
    private String symbol;
    private long execId;
    private long orderId;
    private long counterOrderId;
    private Boolean isTaker;
    private BigDecimal orderQty;
    private BigDecimal leavesQty;
    private BigDecimal price;
    private OrderStatus status;
}