package com.exchange.core.futures.calculators;

import java.math.BigDecimal;
import java.util.Random;

/**
 * This is fake version of funding fee calculation
 * The real one include some complex formulas
 * But this should be enough for our example, just to show that every 8 hours fundingFee should be exchanged between shorts & longs
 */
public class FundingRateCalculatorImpl implements FundingRateCalculator{
  private final Random random = new Random();
  private final int deviation;

  public FundingRateCalculatorImpl(int deviation){
    this.deviation = deviation;
  }

  @Override
  public BigDecimal getFundingRate() {
    double fundingRate = random.nextInt(deviation);
    if (random.nextBoolean()){
      fundingRate = -fundingRate;
    }
    while (fundingRate > 1){
      fundingRate /= 10;
    }
    return new BigDecimal(fundingRate);
  }
}
