package com.exchange.core.orderbook;

import com.exchange.core.account.AccountRepository;
import com.exchange.core.account.Position;
import com.exchange.core.config.AppConstants;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.*;

import java.math.BigDecimal;
import java.util.*;

public class MapOrderBook implements OrderBook {
    private final NavigableMap<BigDecimal, List<Order>> bidsMap;
    private final NavigableMap<BigDecimal, List<Order>> asksMap;
    private final SymbolConfigMessage symbolConfig;
    private final GlobalCounter counter;
    private final Queue<Message> outbound;
    private final AccountRepository accountRepository;


    public MapOrderBook(SymbolConfigMessage symbol, GlobalCounter counter, Queue<Message> outbound, AccountRepository accountRepository) {
        bidsMap = new TreeMap<>();
        asksMap = new TreeMap<>(Comparator.reverseOrder());
        this.symbolConfig = symbol;
        this.counter = counter;
        this.outbound = outbound;
        this.accountRepository = accountRepository;
    }

    @Override
    public void addOrder(Order order) {
        if (!validateBalance(order)) {
            outbound.add(new ErrorMessage("Balance insufficient: order=" + order));
            return;
        }
        order.setOrderId(counter.getNextOrderId());
        order.setLeavesQty(order.getOrderQty());
        sendNewExecReport(order);
        match(order);
        if (order.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            Map<BigDecimal, List<Order>> book = order.getSide() == OrderSide.BUY ? bidsMap : asksMap;
            book.merge(order.getPrice(), new ArrayList<>(List.of(order)), (o, v) -> {
                o.addAll(v);
                return o;
            });
        }
        outbound.add(buildMarketData());
    }

    private boolean validateBalance(Order order) {
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

    private void sendNewExecReport(Order order) {
        ExecReport exec = orderToExecReport(order);
        exec.setStatus(OrderStatus.NEW);
        outbound.add(exec);
    }

    /**
     * LIMIT BUY => we match all sells with price equals or below
     */
    private void match(Order taker) {
        Map<BigDecimal, List<Order>> counterMap;
        if (taker.getSide() == OrderSide.BUY) {
            counterMap = asksMap.headMap(taker.getPrice());
        } else {
            counterMap = bidsMap.headMap(taker.getPrice());
        }
        Iterator<BigDecimal> mapIterator = counterMap.keySet().iterator();
        while (mapIterator.hasNext()) {
            final BigDecimal tradePrice = mapIterator.next();
            List<Order> orders = counterMap.get(tradePrice);
            if (orders != null) {
                Iterator<Order> iterator = orders.iterator();
                while (iterator.hasNext()) {
                    // match order & decrease quantity
                    Order maker = iterator.next();
                    BigDecimal tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
                    BigDecimal tradeAmount = tradeQty.multiply(tradePrice);
                    taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
                    maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

                    settleTrade(taker, maker, tradeQty, tradeAmount);
                    sendExecReport(taker, maker);

                    if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                        // remove maker order from orderbook
                        iterator.remove();
                    }
                }
                if (orders.size() == 0) {
                    mapIterator.remove();
                }
            }
        }
    }

    private void settleTrade(Order taker, Order maker, BigDecimal tradeQty, BigDecimal tradeAmount) {
        Position takerBasePosition = accountRepository.getAccPosition(taker.getAccount(), symbolConfig.getBase());
        Position makerBasePosition = accountRepository.getAccPosition(maker.getAccount(), symbolConfig.getBase());
        Position takerQuotePosition = accountRepository.getAccPosition(taker.getAccount(), symbolConfig.getQuote());
        Position makerQuotePosition = accountRepository.getAccPosition(maker.getAccount(), symbolConfig.getQuote());

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

    private void sendExecReport(Order taker, Order maker) {
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

    private ExecReport orderToExecReport(Order order) {
        ExecReport exec = new ExecReport();
        exec.setOrderId(order.getOrderId());
        exec.setSymbol(order.getSymbol());
        exec.setPrice(order.getPrice());
        exec.setOrderQty(order.getOrderQty());
        exec.setLeavesQty(order.getLeavesQty());
        return exec;
    }

    private MarketData buildMarketData() {
        int depth = Math.max(bidsMap.size(), asksMap.size());
        if (depth > AppConstants.DEFAULT_DEPTH) {
            depth = AppConstants.DEFAULT_DEPTH;
        }
        MarketData md = new MarketData();
        md.setDepth(depth);
        md.setSymbol(symbolConfig.getSymbol());
        md.setTransactTime(System.currentTimeMillis());

        BigDecimal[][] bids = new BigDecimal[depth][];
        BigDecimal[][] asks = new BigDecimal[depth][];
        int bidIndex = 0, asksIndex = 0;
        for (Map.Entry<BigDecimal, List<Order>> e : bidsMap.entrySet()) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            for (Order order : e.getValue()) {
                cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
            }
            bids[bidIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
        }
        for (Map.Entry<BigDecimal, List<Order>> e : asksMap.entrySet()) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            for (Order order : e.getValue()) {
                cumulativeQuantity = cumulativeQuantity.add(order.getLeavesQty());
            }
            asks[asksIndex++] = new BigDecimal[]{e.getKey(), cumulativeQuantity};
        }
        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }
}