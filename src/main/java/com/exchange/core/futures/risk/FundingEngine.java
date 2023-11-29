package com.exchange.core.futures.risk;

import com.exchange.core.futures.calculators.FundingRateCalculator;
import com.exchange.core.futures.calculators.MarkPriceCalculator;
import com.exchange.core.futures.msg.FundingRateMessage;
import com.exchange.core.model.msg.Message;

import java.util.Queue;


public class FundingEngine implements Engine {

  private final Queue<Message> inbound;
  private final FundingRateCalculator fundingRateCalculator;
  private final MarkPriceCalculator markPriceCalculator;
  private final int fundingIntervalInMinutes;
  private long nextFundingTime;

  public FundingEngine(Queue<Message> inbound, FundingRateCalculator fundingRateCalculator,
      MarkPriceCalculator markPriceCalculator, int fundingIntervalInMinutes) {
    this.inbound = inbound;
    this.fundingRateCalculator = fundingRateCalculator;
    this.markPriceCalculator = markPriceCalculator;
    this.fundingIntervalInMinutes = fundingIntervalInMinutes;
    nextFundingTime = calculateNextFundingTime();
  }

  @Override
  public void start() {
    System.out.println("Starting FundingEngine...");
    new Thread(this::run, "FundingEngine").start();
  }

  private void run() {
    while (true) {
      if (System.currentTimeMillis() < nextFundingTime) {
        sleep();
        continue;
      }
      FundingRateMessage msg = new FundingRateMessage();
      msg.setFundingRate(fundingRateCalculator.getFundingRate());
      msg.setMarkPrice(markPriceCalculator.getMarkPrice());
      inbound.add(msg);
      nextFundingTime = calculateNextFundingTime();
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