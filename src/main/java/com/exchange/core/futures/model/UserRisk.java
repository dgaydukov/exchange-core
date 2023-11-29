package com.exchange.core.futures.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UserRisk {
  private BigDecimal IM;
  private BigDecimal ATB;
  private BigDecimal TAM;
}
