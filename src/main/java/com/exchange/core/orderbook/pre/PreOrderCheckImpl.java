package com.exchange.core.orderbook.pre;

import com.exchange.core.repository.AccountRepository;
import com.exchange.core.user.Position;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.ErrorMessage;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.orderbook.GlobalCounter;
import com.exchange.core.repository.InstrumentRepository;

import java.math.BigDecimal;
import java.util.Queue;

public class PreOrderCheckImpl implements PreOrderCheck{
    private final GlobalCounter counter;
    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final Queue<Message> outbound;

    public PreOrderCheckImpl(GlobalCounter counter, AccountRepository accountRepository, InstrumentRepository instrumentRepository, Queue<Message> outbound){
        this.counter = counter;
        this.accountRepository = accountRepository;
        this.instrumentRepository = instrumentRepository;
        this.outbound = outbound;
    }

    @Override
    public boolean validateOrder(Order order) {
        if (!checkBalance(order)){
            outbound.add(new ErrorMessage("Balance insufficient: order=" + order));
            return false;
        }
        return true;
    }

    @Override
    public void updateNewOrder(Order order) {
        order.setOrderId(counter.getNextOrderId());
        order.setLeavesQty(order.getOrderQty());
    }

    @Override
    public void lockBalance(Order order) {
        Position position = getUserPosition(order);
        BigDecimal amount = getTradeAmount(order);
        position.lock(amount);
    }

    private boolean checkBalance(Order order) {
        Position position = getUserPosition(order);
        BigDecimal amount = getTradeAmount(order);
        return position.getBalance().compareTo(amount) > 0;
    }

    private Position getUserPosition(Order order){
        InstrumentConfig inst = instrumentRepository.getInstrument(order.getSymbol());
        String asset = order.getSide() == OrderSide.BUY ? inst.getQuote() : inst.getBase();
        return accountRepository.getAccountPosition(order.getAccount(), asset);
    }
    private BigDecimal getTradeAmount(Order order){
        BigDecimal amount;
        if (order.getType() == OrderType.LIMIT) {
            amount = order.getOrderQty().multiply(order.getPrice());
        } else if (order.getSide() == OrderSide.BUY) {
            amount = order.getQuoteOrderQty();
        } else {
            amount = order.getOrderQty();
        }
        return amount;
    }
}