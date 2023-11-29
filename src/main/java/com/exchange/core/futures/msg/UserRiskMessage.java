package com.exchange.core.futures.msg;

import com.exchange.core.futures.model.UserRisk;
import com.exchange.core.model.msg.Message;
import com.exchange.core.user.Position;
import java.util.List;
import lombok.Data;

@Data
public class UserRiskMessage implements Message {
  private UserRisk risk;
  private List<Position> positions;
}
