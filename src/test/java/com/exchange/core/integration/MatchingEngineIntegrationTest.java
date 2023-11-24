package com.exchange.core.integration;

import com.exchange.core.MockData;
import com.exchange.core.matching.MatchingEngine;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.Queue;

public class MatchingEngineIntegrationTest {
    @Test
    public void invalidSymbolTest() {
        Queue<Message> inbound = new LinkedList<>();
        Queue<Message> outbound = new LinkedList<>();
        MatchingEngine me = new MatchingEngine(inbound, outbound);
        Assertions.assertNull(outbound.poll());
        me.start();
        Assertions.assertNull(outbound.poll());

        Message msg;

        InstrumentConfig inst = MockData.getInstrument();
        Order buy = MockData.getLimitBuy();
        buy.setSymbol(null);

        inbound.add(buy);
        sleep();
        msg = outbound.poll();
        Assertions.assertInstanceOf(ErrorMessage.class, msg);
        ErrorMessage invalidSymbolError = (ErrorMessage) msg;
        Assertions.assertEquals("Symbol of new order can't be null", invalidSymbolError.getError(), "error text mismatch");

        buy.setSymbol("ABC");
        inbound.add(buy);
        sleep();
        msg = outbound.poll();
        Assertions.assertInstanceOf(ErrorMessage.class, msg);
        ErrorMessage orderBookNotFound = (ErrorMessage) msg;
        Assertions.assertEquals("OrderBook not found for symbol=ABC", orderBookNotFound.getError(), "error text mismatch");
    }

    @Test
    public void preOrderCheckFailTest(){
        Queue<Message> inbound = new LinkedList<>();
        Queue<Message> outbound = new LinkedList<>();
        MatchingEngine me = new MatchingEngine(inbound, outbound);
        me.start();

        InstrumentConfig inst = MockData.getInstrument();
        inbound.add(inst);
        Order order = MockData.getLimitBuy();
        inbound.add(order);
        sleep();
        Message msg;
        msg = outbound.poll();
        Assertions.assertInstanceOf(ErrorMessage.class, msg);
        ErrorMessage accountNotFoundError = (ErrorMessage) msg;
        Assertions.assertEquals("Account not found", accountNotFoundError.getError(), "error text mismatch");

        UserBalance user = MockData.getUser(inst.getQuote());
        user.setAmount(BigDecimal.ZERO);
        inbound.add(user);
        inbound.add(order);
        sleep();
        msg = outbound.poll();
        Assertions.assertInstanceOf(ErrorMessage.class, msg);
        ErrorMessage notEnoughBalance = (ErrorMessage) msg;
        Assertions.assertEquals("Balance insufficient", notEnoughBalance.getError(), "error text mismatch");
    }

    @Test
    public void placeOrderTest(){
        Queue<Message> inbound = new LinkedList<>();
        Queue<Message> outbound = new LinkedList<>();
        MatchingEngine me = new MatchingEngine(inbound, outbound);
        me.start();
        // add instrument config
        InstrumentConfig inst = MockData.getInstrument();
        inbound.add(inst);
        // add balance
        UserBalance user = MockData.getUser(inst.getQuote());
        inbound.add(user);
        // add order
        Order buy = MockData.getLimitBuy();
        inbound.add(buy);

        sleep();
        Message msg;
        msg = outbound.poll();
        Assertions.assertInstanceOf(ExecutionReport.class, msg);
        ExecutionReport exec = (ExecutionReport) msg;
        Assertions.assertEquals(buy.getSymbol(), exec.getSymbol(), "symbol mismatch");
        Assertions.assertEquals(buy.getOrderQty(), exec.getOrderQty(), "orderQty mismatch");
        Assertions.assertEquals(buy.getPrice(), exec.getPrice(), "price mismatch");
        Assertions.assertEquals(OrderStatus.NEW, exec.getStatus(), "status should be new");

        msg = outbound.poll();
        Assertions.assertInstanceOf(MarketData.class, msg);
        MarketData md = (MarketData) msg;
        Assertions.assertEquals(buy.getSymbol(), md.getSymbol(), "symbol mismatch");
        Assertions.assertEquals(1, md.getDepth(), "depth mismatch");
        Assertions.assertEquals(1, md.getBids().length, "should be 1 bids");
        BigDecimal[][] bids = new BigDecimal[][]{
                {new BigDecimal("100"), new BigDecimal("10")}
        };
        Assertions.assertArrayEquals(bids, md.getBids(), "bids mismatch");
        Assertions.assertEquals(0, md.getAsks().length, "should be 0 asks");
    }

    private void sleep(){
        try{
            Thread.sleep(50);
        } catch (InterruptedException ex){
        }
    }
}
