package com.exchange.core.user;

import com.exchange.core.exceptions.AppException;
import lombok.Data;

import java.math.BigDecimal;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Position{

  private String symbol;
  private BigDecimal balance;
  private BigDecimal locked;

  public Position(String asset) {
    this(asset, BigDecimal.ZERO);
  }

  public Position(String asset, BigDecimal balance) {
    this.symbol = asset;
    this.balance = balance;
    this.locked = BigDecimal.ZERO;
  }

  public BigDecimal getTotalBalance() {
    return balance.add(locked);
  }

  public void add(BigDecimal amount) {
    this.balance = balance.add(amount);
  }

  public void lock(BigDecimal amount) {
    if (amount.compareTo(balance) > 0) {
      throw new AppException(
          "Failed to lock more than available: amount=" + amount + ", balance=" + balance);
    }
    locked = locked.add(amount);
    balance = balance.subtract(amount);
  }

  public void unlock(BigDecimal amount) {
    if (amount.compareTo(locked) > 0) {
      throw new AppException(
          "Failed to unlock more than locked: amount=" + amount + ", locked=" + locked);
    }
    locked = locked.subtract(amount);
    balance = balance.add(amount);
  }

  public void freeLocked(BigDecimal amount) {
    if (amount.compareTo(locked) > 0) {
      throw new AppException(
          "Failed to free more than locked: amount=" + amount + ", locked=" + locked);
    }
    locked = locked.subtract(amount);
  }
}