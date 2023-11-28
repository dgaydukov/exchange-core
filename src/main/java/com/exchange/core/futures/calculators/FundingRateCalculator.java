package com.exchange.core.futures.calculators;

import java.math.BigDecimal;

public interface FundingRateCalculator {

  BigDecimal getFundingRate();
}