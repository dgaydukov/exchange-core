package com.exchange.core.orderbook.pre;

import com.exchange.core.account.AccountRepository;
import com.exchange.core.account.Position;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.ErrorMessage;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.model.msg.SymbolConfigMessage;
import com.exchange.core.orderbook.GlobalCounter;

import java.math.BigDecimal;
import java.util.Queue;

public class PreOrderCheckImpl implements PreOrderCheck{
    private final SymbolConfigMessage symbolConfig;
    private final GlobalCounter counter;
    private final AccountRepository accountRepository;
    private final Queue<Message> outbound;

    public PreOrderCheckImpl(SymbolConfigMessage symbolConfig, GlobalCounter counter, AccountRepository accountRepository, Queue<Message> outbound){
        this.symbolConfig = symbolConfig;
        this.counter = counter;
        this.accountRepository = accountRepository;
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

    private boolean checkBalance(Order order) {
        String symbol = order.getSide() == OrderSide.BUY ? symbolConfig.getQuote() : symbolConfig.getBase();
        Position position = accountRepository.getAccPosition(order.getAccount(), symbol);
        BigDecimal amount;
        if (order.getType() == OrderType.LIMIT) {
            amount = order.getOrderQty().multiply(order.getPrice());
        } else if (order.getSide() == OrderSide.BUY) {
            amount = order.getQuoteOrderQty();
        } else {
            amount = order.getOrderQty();
        }
        return position.getBalance().compareTo(amount) > 0;
    }
}