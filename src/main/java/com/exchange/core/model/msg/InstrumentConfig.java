package com.exchange.core.model.msg;

import com.exchange.core.model.enums.SecurityType;
import lombok.Data;

@Data
public class InstrumentConfig implements Message {

  private String symbol;
  private SecurityType type;
  private String base;
  private String quote;
}