package com.exchange.core.futures.calculators;

import java.math.BigDecimal;
import java.util.Random;

/**
 * This is fake markPrice generator, specifically designed by me to show how perpetual futures work
 * The real markPrice calculation look differently, you first collect indexPrice from top N exchanges,
 * then you use InterestRate and using formulas calculate markPrice
 * But for our example it's ideal solution, you can manage your markPrice to any value you want
 */
public class MarkPriceCalculatorImpl implements MarkPriceCalculator{
  private final Random random = new Random();
  private BigDecimal markPrice;
  private final int deviation;
  public MarkPriceCalculatorImpl(BigDecimal markPrice, int deviation){
    this.markPrice = markPrice;
    this.deviation = deviation;
  }

  @Override
  public BigDecimal getMarkPrice(){
    BigDecimal movement = new BigDecimal(random.nextInt(deviation));
    markPrice = random.nextBoolean() ? markPrice.add(movement) : markPrice.subtract(movement);
    return markPrice;
  }
}