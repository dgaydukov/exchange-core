package com.exchange.core.model;

import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Trade {

  private Order taker;
  private Order maker;
  private BigDecimal tradeQty;
  private BigDecimal tradePrice;
  private BigDecimal tradeAmount;
}
