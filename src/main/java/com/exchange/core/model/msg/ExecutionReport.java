package com.exchange.core.model.msg;

import com.exchange.core.model.enums.OrderStatus;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExecutionReport implements Message {

  private String symbol;
  private long execId;
  private long orderId;
  private String clOrdId;
  private long counterOrderId;
  private Boolean isTaker;
  private BigDecimal orderQty;
  private BigDecimal leavesQty;
  private BigDecimal price;
  private BigDecimal lastQty;
  private BigDecimal lastPx;
  private OrderStatus status;
}