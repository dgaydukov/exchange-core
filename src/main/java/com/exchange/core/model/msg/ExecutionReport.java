package com.exchange.core.model.msg;

import com.exchange.core.model.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExecutionReport implements Message {

  private String symbol;
  private long execId;
  private long orderId;
  private long counterOrderId;
  private Boolean isTaker;
  private BigDecimal orderQty;
  private BigDecimal leavesQty;
  private BigDecimal price;
  private BigDecimal lastQty;
  private BigDecimal lastPx;
  private OrderStatus status;
}