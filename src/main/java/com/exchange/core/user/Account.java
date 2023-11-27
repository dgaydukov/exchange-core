package com.exchange.core.user;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Account {

  private int accountId;
  private Map<String, Position> positions;

  public Account(int accountId) {
    this.accountId = accountId;
    positions = new HashMap<>();
  }

  public Position getPosition(String asset) {
    return positions.compute(asset, (k, v) -> v == null ? new Position(asset) : v);
  }
}