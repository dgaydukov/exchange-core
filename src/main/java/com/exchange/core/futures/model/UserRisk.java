package com.exchange.core.futures.model;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UserRisk {
  private BigDecimal initialMargin;
  private BigDecimal availableTransferBalance;
  private BigDecimal totalAccountMargin;
}
