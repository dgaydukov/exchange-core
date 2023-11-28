package com.exchange.core.futures.risk;

import com.exchange.core.futures.calculators.FundingRateCalculator;
import com.exchange.core.futures.calculators.MarkPriceCalculator;
import com.exchange.core.model.enums.SecurityType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.user.Account;

import com.exchange.core.user.Position;
import java.math.BigDecimal;
import java.util.List;


public class FundingEngine implements Engine {

  private final AccountRepository accountRepository;
  private final InstrumentRepository instrumentRepository;
  private final FundingRateCalculator fundingRateCalculator;
  private final MarkPriceCalculator markPriceCalculator;
  private final int fundingIntervalInMinutes;
  private long nextFundingTime;

  public FundingEngine(AccountRepository accountRepository, InstrumentRepository instrumentRepository,
      FundingRateCalculator fundingRateCalculator, MarkPriceCalculator markPriceCalculator,
      int fundingIntervalInMinutes) {
    this.accountRepository = accountRepository;
    this.instrumentRepository = instrumentRepository;
    this.fundingRateCalculator = fundingRateCalculator;
    this.markPriceCalculator = markPriceCalculator;
    this.fundingIntervalInMinutes = fundingIntervalInMinutes;
    nextFundingTime = calculateNextFundingTime();
  }

  public BigDecimal getIndexPrice() {
    return BigDecimal.ZERO;
  }

  @Override
  public void start() {
    System.out.println("Starting FundingEngine...");
    new Thread(this::run, "FundingEngine").start();
  }

  private void run() {
    while(true){
      if (System.currentTimeMillis() < nextFundingTime){
        sleep();
        continue;
      }
      nextFundingTime = calculateNextFundingTime();
      BigDecimal fundingRate = fundingRateCalculator.getFundingRate();
      List<InstrumentConfig> instruments = instrumentRepository.getInstruments()
          .stream()
          .filter(config -> SecurityType.PERPETUAL_FUTURES.equals(config.getType()))
          .toList();
      for (Account account : accountRepository.getAllAccounts()) {
        for (InstrumentConfig config: instruments){
          Position position = account.getPosition(config.getSymbol());
          if (position.getBalance().compareTo(BigDecimal.ZERO) == 0){
            continue;
          }
          // positive fundingFee longs pays to shorts, and otherwise
          BigDecimal notionalValue = position.getBalance().multiply(markPriceCalculator.getMarkPrice());

        }
      }
    }
  }

  private long calculateNextFundingTime(){
    return System.currentTimeMillis() + (long) fundingIntervalInMinutes * 60 * 1000;
  }

  private void sleep(){
    try{
      Thread.sleep(1000);
    } catch (InterruptedException ex){
      System.out.println("ERR => "+ ex);
    }
  }
}