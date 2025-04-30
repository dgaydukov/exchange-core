package com.exchange.core.model.msg;

import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

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
  @JsonIgnore
  @EqualsAndHashCode.Exclude
  public Order prev;
  @ToString.Exclude
  @JsonIgnore
  @EqualsAndHashCode.Exclude
  public Order next;
  /**
   * This fields used for quick navigation to fetch PriceLevel in which this order leaves
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @JsonIgnore
  public PriceLevel level;
}