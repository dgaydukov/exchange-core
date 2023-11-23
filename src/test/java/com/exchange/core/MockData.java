package com.exchange.core;

import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.InstrumentConfig;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;

public class MockData {
    public static InstrumentConfig getInstrument(){
        InstrumentConfig config = new InstrumentConfig();
        config.setSymbol("BTC/USDT");
        config.setBase("BTC");
        config.setQuote("USDT");
        return config;
    }

    public static Order getLimitBuy(){
        Order order = new Order();
        order.setSymbol("BTC-USDT");
        order.setType(OrderType.LIMIT);
        order.setSide(OrderSide.BUY);
        order.setPrice(new BigDecimal("100"));
        order.setOrderQty(new BigDecimal("10"));
        order.setAccount(1);
        return order;
    }
}
