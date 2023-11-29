package com.exchange.core.futures.orderchecks;

import com.exchange.core.config.AppConstants;
import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.matching.orderchecks.PostOrderCheckImpl;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.PositionDirection;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.user.Account;
import com.exchange.core.user.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Queue;

public class FuturesPostOrderCheck extends PostOrderCheckImpl {
  private final GlobalCounter counter;
  private final AccountRepository accountRepository;
  private final InstrumentRepository instrumentRepository;
  private final Queue<Message> outbound;

  public FuturesPostOrderCheck(GlobalCounter counter, AccountRepository accountRepository,
      InstrumentRepository instrumentRepository, Queue<Message> outbound) {
    super(counter, accountRepository, instrumentRepository, outbound);
    this.counter = counter;
    this.accountRepository = accountRepository;
    this.instrumentRepository = instrumentRepository;
    this.outbound = outbound;
  }

  @Override
  public void settleTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradeAmount) {
    InstrumentConfig inst = instrumentRepository.getInstrument(taker.getSymbol());
    Account takerAccount = accountRepository.getAccount(taker.getAccount());
    Account makerAccount = accountRepository.getAccount(maker.getAccount());
    Position takerPosition = accountRepository.getAccountPosition(taker.getAccount(),
        inst.getSymbol());
    Position makerPosition = accountRepository.getAccountPosition(maker.getAccount(),
        inst.getSymbol());
    Position takerQuotePosition = accountRepository.getAccountPosition(taker.getAccount(),
        inst.getQuote());
    Position makerQuotePosition = accountRepository.getAccountPosition(maker.getAccount(),
        inst.getQuote());


    makerQuotePosition.freeLocked(tradeAmount);

    if (taker.getSide() == OrderSide.BUY){
      takerQuotePosition.freeLocked(tradeAmount);
      BigDecimal price = tradeAmount.divide(tradeQty, AppConstants.ROUNDING_SCALE, RoundingMode.DOWN);
      if (takerPosition.getAmount().compareTo(BigDecimal.ZERO) == 0){
        // new taker position
        takerPosition.setAmount(tradeQty);
        takerPosition.setDirection(PositionDirection.LONG);
        takerPosition.setPrice(price);
      } else if (takerPosition.getDirection() == PositionDirection.LONG){
        // increase position & re-calculate new price
        BigDecimal oldQty = takerPosition.getAmount();
        BigDecimal oldPrice = takerPosition.getPrice();
        BigDecimal newQty = oldQty.add(tradeQty);
        BigDecimal newPrice = oldPrice.add(price).divide(new BigDecimal(2), AppConstants.ROUNDING_SCALE, RoundingMode.DOWN);
        takerPosition.setAmount(newQty);
        takerPosition.setPrice(newPrice);
      } else {
        // taker has short position, so closing his short position
      }
    }
  }
}