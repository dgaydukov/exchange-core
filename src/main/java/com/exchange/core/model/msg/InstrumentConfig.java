package com.exchange.core.model.msg;

import lombok.Data;

@Data
public class InstrumentConfig implements Message {

  private String symbol;
  private String base;
  private String quote;
}
