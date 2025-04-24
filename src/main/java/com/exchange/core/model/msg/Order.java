package com.exchange.core.model.msg;

import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class Order implements Message {
  private String symbol;
  private long orderId;
  private String clOrdId;
  private int account;
  private OrderSide side;
  private OrderType type;
  private BigDecimal orderQty;
  private BigDecimal leavesQty;
  private BigDecimal quoteOrderQty;
  private BigDecimal price;

  /**
   * These 2 fields used only for linked list iteration for PriceLevel
   */
  public Order prev;
  public Order next;
}