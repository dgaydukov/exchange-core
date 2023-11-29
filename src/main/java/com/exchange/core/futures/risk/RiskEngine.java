package com.exchange.core.futures.risk;

import com.exchange.core.config.AppConstants;
import com.exchange.core.futures.calculators.MarkPriceCalculator;
import com.exchange.core.futures.model.UserRisk;
import com.exchange.core.futures.msg.UserLiquidationMessage;
import com.exchange.core.futures.msg.UserRiskMessage;
import com.exchange.core.model.enums.SecurityType;
import com.exchange.core.model.msg.Message;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class RiskEngine implements Engine {

  private final Queue<Message> inbound;
  private final Queue<Message> outbound;
  private final AccountRepository accountRepository;
  private final MarkPriceCalculator markPriceCalculator;

  public RiskEngine(Queue<Message> inbound, Queue<Message> outbound,
      AccountRepository accountRepository, MarkPriceCalculator markPriceCalculator) {
    this.inbound = inbound;
    this.outbound = outbound;
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
      Position usdtPosition = account.getPosition(AppConstants.FUTURES_SETTLE_ASSET);
      BigDecimal initialMargin = BigDecimal.ZERO;
      BigDecimal unrealizedPnL = BigDecimal.ZERO;
      List<Position> positions = new ArrayList<>();
      for (Position position : account.getPositions().values()) {
        if (position.getType() == SecurityType.PERPETUAL_FUTURES) {
          BigDecimal posUnrealizedPnL = position.getAmount().multiply(markPrice.subtract(position.getPrice()));
          position.setUnrealizedPnL(posUnrealizedPnL);
          positions.add(position);
          unrealizedPnL = unrealizedPnL.add(posUnrealizedPnL);
          initialMargin = initialMargin.add(
              position.getAmount().multiply(position.getPrice())
              .divide(account.getLeverage(), AppConstants.ROUNDING_SCALE, RoundingMode.DOWN)
          );
        }
      }
      UserRisk risk = account.getRisk();
      BigDecimal availableBalance = usdtPosition.getBalance();
      BigDecimal totalAccountMargin = availableBalance.add(initialMargin).add(unrealizedPnL);
      risk.setAvailableTransferBalance(availableBalance.min(availableBalance.add(unrealizedPnL)));
      risk.setInitialMargin(initialMargin);
      risk.setTotalAccountMargin(totalAccountMargin);
      // send user risk & position update
      UserRiskMessage riskMessage = new UserRiskMessage();
      riskMessage.setRisk(risk);
      riskMessage.setPositions(positions);
      outbound.add(riskMessage);
      if (totalAccountMargin.compareTo(initialMargin.multiply(AppConstants.MARGIN_LIQUIDATION_TRIGGER)) <= 0){
        // start liquidation
        UserLiquidationMessage liquidationMessage = new UserLiquidationMessage();
        liquidationMessage.setAccount(account.getAccountId());
        inbound.add(liquidationMessage);
      }
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