package com.exchange.core.user;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountTest {

  @Test
  public void notNullPositionTest() {
    Account account = new Account(1);
    Assertions.assertNotNull(account.getPosition("BTC"));
  }
}
