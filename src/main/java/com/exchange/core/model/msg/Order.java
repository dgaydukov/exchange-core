package com.exchange.core.model.msg;

import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import java.math.BigDecimal;
import lombok.Data;
import lombok.ToString;

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
   * These  fields used only for linked list iteration for PriceLevel
   * To avoid infinite recursion we exclude these 2 fields from to string
   */
  @ToString.Exclude
  public Order prev;
  @ToString.Exclude
  public Order next;
  /**
   * This fields used for quick navigation to fetch PriceLevel in which this order leaves
   */
  @ToString.Exclude
  public PriceLevel level;
}