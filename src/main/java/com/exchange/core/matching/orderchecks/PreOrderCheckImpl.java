package com.exchange.core.matching.orderchecks;

import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.ErrorMessage;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.user.Position;
import java.math.BigDecimal;
import java.util.Queue;

public class PreOrderCheckImpl implements PreOrderCheck {

  private final GlobalCounter counter;
  private final AccountRepository accountRepository;
  private final InstrumentRepository instrumentRepository;
  private final Queue<Message> outbound;

  public PreOrderCheckImpl(GlobalCounter counter, AccountRepository accountRepository,
      InstrumentRepository instrumentRepository, Queue<Message> outbound) {
    this.counter = counter;
    this.accountRepository = accountRepository;
    this.instrumentRepository = instrumentRepository;
    this.outbound = outbound;
  }

  @Override
  public boolean validateOrder(Order order) {
    if (!validateAccount(order)) {
      outbound.add(new ErrorMessage("Account not found", order));
      return false;
    }
    if (!validateMarketOrder(order)) {
      outbound.add(new ErrorMessage("Invalid market order", order));
      return false;
    }
    if (!validateBalance(order)) {
      outbound.add(new ErrorMessage("Balance insufficient", order));
      return false;
    }
    return true;
  }

  @Override
  public void updateNewOrder(Order order) {
    order.setOrderId(counter.getNextOrderId());
    order.setLeavesQty(order.getOrderQty());
    if (order.getType() == OrderType.MARKET && order.getSide() == OrderSide.BUY) {
      order.setLeavesQty(order.getQuoteOrderQty());
    }
  }

  @Override
  public void lockBalance(Order order) {
    Position position = getUserPosition(order);
    BigDecimal amount = getTradeAmount(order);
    position.lock(amount);
  }


  private boolean validateAccount(Order order) {
    return accountRepository.getAccount(order.getAccount()) != null;
  }

  private boolean validateBalance(Order order) {
    Position position = getUserPosition(order);
    BigDecimal amount = getTradeAmount(order);
    return position.getBalance().compareTo(amount) >= 0;
  }

  private boolean validateMarketOrder(Order order) {
    if (order.getType() == OrderType.MARKET) {
      if (order.getPrice() != null) {
        return false;
      }
      if (order.getSide() == OrderSide.BUY) {
        return order.getQuoteOrderQty() != null;
      } else {
        return order.getOrderQty() != null;
      }
    }
    return true;
  }

  private Position getUserPosition(Order order) {
    InstrumentConfig inst = instrumentRepository.getInstrument(order.getSymbol());
    String asset = order.getSide() == OrderSide.BUY ? inst.getQuote() : inst.getBase();
    return accountRepository.getAccountPosition(order.getAccount(), asset);
  }

  private BigDecimal getTradeAmount(Order order) {
    if (order.getSide() == OrderSide.BUY) {
      if (order.getType() == OrderType.LIMIT) {
        return order.getOrderQty().multiply(order.getPrice());
      } else {
        return order.getQuoteOrderQty();
      }
    } else {
      return order.getOrderQty();
    }
  }
}