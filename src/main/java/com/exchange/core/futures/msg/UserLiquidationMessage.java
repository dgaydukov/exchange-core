package com.exchange.core.futures.msg;

import com.exchange.core.model.msg.Message;
import lombok.Data;

@Data
public class UserLiquidationMessage implements Message {
  private int account;
}
