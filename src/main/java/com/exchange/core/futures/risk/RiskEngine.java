package com.exchange.core.futures.risk;

import com.exchange.core.futures.calculators.MarkPriceCalculator;
import com.exchange.core.futures.model.UserRisk;
import com.exchange.core.model.enums.SecurityType;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.math.BigDecimal;

public class RiskEngine implements Engine {

  private final AccountRepository accountRepository;
  private final MarkPriceCalculator markPriceCalculator;

  public RiskEngine(AccountRepository accountRepository, MarkPriceCalculator markPriceCalculator) {
    this.accountRepository = accountRepository;
    this.markPriceCalculator = markPriceCalculator;
  }

  @Override
  public void start() {
    System.out.println("Starting FundingEngine...");
    new Thread(this::run, "FundingEngine").start();
  }

  private void run() {
    while (true) {
      sleep(1);
      updateRisk();
    }
  }

  private void updateRisk() {
    BigDecimal markPrice = markPriceCalculator.getMarkPrice();
    for (Account account : accountRepository.getAllAccounts()) {
      for (Position position : account.getPositions().values()) {
        if (position.getType() == SecurityType.PERPETUAL_FUTURES) {
          BigDecimal unrealizedPnL = position.getAmount().multiply(markPrice.subtract(position.getPrice()));
          position.setUnrealizedPnL(unrealizedPnL);
          // TODO: send PnL update to user
        }
      }
      UserRisk risk = account.getRisk();
    }
  }

  private void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ex) {
      System.out.println("ERR => " + ex);
    }
  }
}