package com.exchange.core.repository;

import com.exchange.core.model.msg.UserBalance;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AccountRepositoryTest {

  @Test
  public void addBalanceTest() {
    UserBalance user = new UserBalance();
    user.setAccount(1);
    user.setAsset("BTC");
    user.setAmount(new BigDecimal("100"));

    AccountRepository repository = new AccountRepositoryImpl();
    Assertions.assertNull(repository.getAccount(user.getAccount()));
    Assertions.assertNull(repository.getAccountPosition(user.getAccount(), user.getAsset()));
    Assertions.assertNotNull(repository.getAllAccounts());
    Assertions.assertEquals(0, repository.getAllAccounts().size());

    repository.addBalance(user);

    Account account = repository.getAccount(user.getAccount());
    Assertions.assertNotNull(account);
    Assertions.assertEquals(user.getAccount(), account.getAccountId());

    Position position = repository.getAccountPosition(user.getAccount(), user.getAsset());
    Assertions.assertNotNull(position);
    Assertions.assertEquals(user.getAmount(), position.getBalance());

    Assertions.assertEquals(1, repository.getAllAccounts().size());
  }
}
