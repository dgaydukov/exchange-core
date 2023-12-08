package com.exchange.core.model.msg;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UserBalance implements Message {

  private int account;
  private String asset;
  private BigDecimal amount;
}