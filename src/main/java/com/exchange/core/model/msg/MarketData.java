package com.exchange.core.model.msg;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketData implements Message {

  private String symbol;
  private int depth;
  private long transactTime;
  private BigDecimal[][] bids;
  private BigDecimal[][] asks;
}
