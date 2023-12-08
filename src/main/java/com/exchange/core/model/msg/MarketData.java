package com.exchange.core.model.msg;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MarketData implements Message {

  private String symbol;
  private int depth;
  private long transactTime;
  private BigDecimal[][] bids;
  private BigDecimal[][] asks;
}
