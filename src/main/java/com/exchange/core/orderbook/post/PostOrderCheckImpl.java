package com.exchange.core.orderbook.post;

import com.exchange.core.account.AccountRepository;
import com.exchange.core.account.Position;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.*;
import com.exchange.core.orderbook.GlobalCounter;

import java.math.BigDecimal;
import java.util.Queue;

public class PostOrderCheckImpl implements PostOrderCheck{
    private final SymbolConfigMessage symbolConfig;
    private final GlobalCounter counter;
    private final AccountRepository accountRepository;
    private final Queue<Message> outbound;

    public PostOrderCheckImpl(SymbolConfigMessage symbolConfig, GlobalCounter counter, AccountRepository accountRepository, Queue<Message> outbound){
        this.symbolConfig = symbolConfig;
        this.counter = counter;
        this.accountRepository = accountRepository;
        this.outbound = outbound;
    }

    @Override
    public void sendExecReportNew(Order order) {
        ExecReport exec = orderToExecReport(order);
        exec.setStatus(OrderStatus.NEW);
        outbound.add(exec);
    }

    @Override
    public void sendExecReportCancel(Order order) {
        ExecReport exec = orderToExecReport(order);
        exec.setStatus(OrderStatus.CANCELLED);
        outbound.add(exec);
    }

    @Override
    public void sendExecReportTrade(Order taker, Order maker) {
        ExecReport execTaker = orderToExecReport(taker);
        execTaker.setExecId(counter.getNextExecutionId());
        execTaker.setIsTaker(true);
        execTaker.setCounterOrderId(maker.getOrderId());
        execTaker.setStatus(OrderStatus.PARTIALLY_FILLED);
        if (taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
            execTaker.setStatus(OrderStatus.FILLED);
        }
        ExecReport execMaker = orderToExecReport(maker);
        execMaker.setExecId(counter.getNextExecutionId());
        execMaker.setIsTaker(false);
        execMaker.setCounterOrderId(taker.getOrderId());
        execMaker.setStatus(OrderStatus.PARTIALLY_FILLED);
        if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
            execMaker.setStatus(OrderStatus.FILLED);
        }

        outbound.add(execTaker);
        outbound.add(execMaker);
    }

    @Override
    public void sendMarketData(MarketData marketData) {

    }

    @Override
    public void settleTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradeAmount) {
        Position takerBasePosition = accountRepository.getAccountPosition(taker.getAccount(), symbolConfig.getBase());
        Position makerBasePosition = accountRepository.getAccountPosition(maker.getAccount(), symbolConfig.getBase());
        Position takerQuotePosition = accountRepository.getAccountPosition(taker.getAccount(), symbolConfig.getQuote());
        Position makerQuotePosition = accountRepository.getAccountPosition(maker.getAccount(), symbolConfig.getQuote());

        if (taker.getSide() == OrderSide.BUY) {
            takerQuotePosition.freeLocked(tradeAmount);
            takerBasePosition.add(tradeQty);
            makerBasePosition.freeLocked(tradeQty);
            makerQuotePosition.add(tradeAmount);
        } else {
            takerBasePosition.freeLocked(tradeQty);
            takerQuotePosition.add(tradeAmount);
            makerBasePosition.freeLocked(tradeAmount);
            makerQuotePosition.add(tradeQty);
        }
    }

    private ExecReport orderToExecReport(Order order) {
        ExecReport exec = new ExecReport();
        exec.setOrderId(order.getOrderId());
        exec.setSymbol(order.getSymbol());
        exec.setPrice(order.getPrice());
        exec.setOrderQty(order.getOrderQty());
        exec.setLeavesQty(order.getLeavesQty());
        return exec;
    }
}