package com.exchange.core.futures.msg;

import com.exchange.core.model.msg.Message;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FundingRateMessage implements Message {
  private BigDecimal markPrice;
  private BigDecimal fundingRate;
}