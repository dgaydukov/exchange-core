package com.exchange.core.matching.orderchecks;

import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.*;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import com.exchange.core.user.Position;

import java.math.BigDecimal;
import java.util.Queue;

public class PostOrderCheckImpl implements PostOrderCheck{
    private final GlobalCounter counter;
    private final AccountRepository accountRepository;
    private final InstrumentRepository instrumentRepository;
    private final Queue<Message> outbound;

    public PostOrderCheckImpl(GlobalCounter counter, AccountRepository accountRepository, InstrumentRepository instrumentRepository, Queue<Message> outbound){
        this.counter = counter;
        this.accountRepository = accountRepository;
        this.instrumentRepository = instrumentRepository;
        this.outbound = outbound;
    }

    @Override
    public void sendExecReportNew(Order order) {
        ExecutionReport exec = orderToExecReport(order);
        exec.setStatus(OrderStatus.NEW);
        outbound.add(exec);
    }

    @Override
    public void sendExecReportTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradePrice) {
        ExecutionReport execTaker = orderToExecReport(taker);
        execTaker.setIsTaker(true);
        execTaker.setCounterOrderId(maker.getOrderId());
        execTaker.setStatus(OrderStatus.PARTIALLY_FILLED);
        if (taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
            execTaker.setStatus(OrderStatus.FILLED);
        }
        execTaker.setLastQty(tradeQty);
        execTaker.setLastPx(tradePrice);
        ExecutionReport execMaker = orderToExecReport(maker);
        execMaker.setIsTaker(false);
        execMaker.setCounterOrderId(taker.getOrderId());
        execMaker.setStatus(OrderStatus.PARTIALLY_FILLED);
        if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
            execMaker.setStatus(OrderStatus.FILLED);
        }
        execMaker.setLastQty(tradeQty);
        execMaker.setLastPx(tradePrice);

        outbound.add(execTaker);
        outbound.add(execMaker);
    }

    @Override
    public void sendMarketData(MarketData marketData) {
        outbound.add(marketData);
    }

    @Override
    public void settleTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradeAmount) {
        InstrumentConfig inst = instrumentRepository.getInstrument(taker.getSymbol());
        Position takerBasePosition = accountRepository.getAccountPosition(taker.getAccount(), inst.getBase());
        Position makerBasePosition = accountRepository.getAccountPosition(maker.getAccount(), inst.getBase());
        Position takerQuotePosition = accountRepository.getAccountPosition(taker.getAccount(), inst.getQuote());
        Position makerQuotePosition = accountRepository.getAccountPosition(maker.getAccount(), inst.getQuote());

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

    @Override
    public void cancelOrder(Order order) {
        // free locked balance
        InstrumentConfig inst = instrumentRepository.getInstrument(order.getSymbol());
        String asset = order.getSide() == OrderSide.BUY ? inst.getBase() : inst.getQuote();
        Position position = accountRepository.getAccountPosition(order.getAccount(), asset);
        position.freeLocked(order.getLeavesQty());
        // send cancellation execution report
        ExecutionReport exec = orderToExecReport(order);
        exec.setStatus(OrderStatus.CANCELLED);
        outbound.add(exec);
    }

    private ExecutionReport orderToExecReport(Order order) {
        ExecutionReport exec = new ExecutionReport();
        exec.setExecId(counter.getNextExecutionId());
        exec.setOrderId(order.getOrderId());
        exec.setSymbol(order.getSymbol());
        exec.setPrice(order.getPrice());
        exec.setOrderQty(order.getOrderQty());
        exec.setLeavesQty(order.getLeavesQty());
        return exec;
    }
}