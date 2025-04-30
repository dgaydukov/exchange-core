package com.exchange.core.matching.orderbook.book;

import com.exchange.core.config.AppConstants;
import com.exchange.core.matching.orderbook.OrderBook;
import com.exchange.core.matching.orderbook.level.OrderBookLevel;
import com.exchange.core.matching.orderbook.level.PriceLevel;
import com.exchange.core.model.Trade;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.MarketData;
import com.exchange.core.model.msg.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LinkedListOrderBook implements OrderBook {
    private OrderBookLevel bestBid;
    private OrderBookLevel bestAsk;

    private final String symbol;
    private final Map<Long, Order> orderIdMap;

    public LinkedListOrderBook(String symbol) {
        this.symbol = symbol;
        orderIdMap = new HashMap<>();
    }

    @Override
    public List<Trade> match(Order taker) {
        List<Trade> trades = new ArrayList<>();
        if (taker.getSide() == OrderSide.BUY) {
            OrderBookLevel level = bestAsk;
            while (level != null) {
                if (taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
                if (taker.getType() == OrderType.LIMIT) {
                    if (taker.getPrice().compareTo(level.getPrice()) < 0) {
                        break;
                    }
                    matchLimit(taker, level, trades);
                } else {
                    matchMarket(taker, level, trades);
                }
                // remove level if it fully matched
                if (!level.hasNext()) {
                    OrderBookLevel prev = level.prev;
                    OrderBookLevel next = level.next;
                    if (prev != null) {
                        prev.next = next;
                        if (next != null) {
                            next.prev = prev;
                        }
                    }
                }
                level = level.next;
            }
        } else {
            OrderBookLevel level = bestBid;
            while (level != null) {
                if (taker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
                if (taker.getType() == OrderType.LIMIT) {
                    if (taker.getPrice().compareTo(level.getPrice()) > 0) {
                        break;
                    }
                    matchLimit(taker, level, trades);
                } else {
                    matchMarket(taker, level, trades);
                }
                // remove level if it fully matched
                if (!level.hasNext()) {
                    OrderBookLevel prev = level.prev;
                    OrderBookLevel next = level.next;
                    if (prev != null) {
                        prev.next = next;
                        if (next != null) {
                            next.prev = prev;
                        }
                    }
                }
                level = level.next;
            }
        }
        return trades;
    }

    private void matchLimit(Order taker, PriceLevel level, List<Trade> trades) {
        final BigDecimal tradePrice = level.getPrice();
        level.resetIterator();
        while (level.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            Order maker = level.next();
            BigDecimal tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
            BigDecimal tradeAmount = tradeQty.multiply(tradePrice);
            taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
            maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

            trades.add(new Trade(taker, maker, tradeQty, tradePrice, tradeAmount));

            if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                level.remove();
            }
        }
    }

    private void matchMarket(Order taker, PriceLevel level, List<Trade> trades) {
        level.resetIterator();
        BigDecimal tradePrice = level.getPrice();
        while (level.hasNext() && taker.getLeavesQty().compareTo(BigDecimal.ZERO) > 0) {
            Order maker = level.next();
            BigDecimal tradeQty, tradeAmount;
            if (taker.getSide() == OrderSide.BUY) {
                BigDecimal takerTradeAmount = taker.getLeavesQty();
                BigDecimal takerTradeQty = takerTradeAmount.divide(tradePrice, AppConstants.ROUNDING_SCALE,
                        RoundingMode.DOWN);

                tradeQty = takerTradeQty.min(maker.getLeavesQty());
                tradeAmount = tradeQty.multiply(tradePrice);
                if (maker.getLeavesQty().compareTo(takerTradeQty) > 0) {
                    tradeAmount = takerTradeAmount;
                }

                taker.setLeavesQty(taker.getLeavesQty().subtract(tradeAmount));
                maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));

            } else {
                tradeQty = taker.getLeavesQty().min(maker.getLeavesQty());
                tradeAmount = tradeQty.multiply(tradePrice);
                taker.setLeavesQty(taker.getLeavesQty().subtract(tradeQty));
                maker.setLeavesQty(maker.getLeavesQty().subtract(tradeQty));
            }

            trades.add(new Trade(taker, maker, tradeQty, tradePrice, tradeAmount));
            if (maker.getLeavesQty().compareTo(BigDecimal.ZERO) == 0) {
                level.remove();
            }
        }
    }

    @Override
    public boolean add(Order order) {
        orderIdMap.put(order.getOrderId(), order);
        if (order.getSide() == OrderSide.BUY) {
            OrderBookLevel level = bestBid;
            OrderBookLevel last = null;
            boolean addToLast = true;
            while (level != null) {
                // append order to existing level
                if (order.getPrice().compareTo(level.getPrice()) == 0) {
                    addToLast = false;
                    level.add(order);
                    break;
                }
                // insert new PriceLevel before current level, because price is better (bigger for bids)
                if (order.getPrice().compareTo(level.getPrice()) > 0) {
                    addToLast = false;
                    OrderBookLevel newLvl = new OrderBookLevel(order);
                    if (level.prev == null){
                        newLvl.next = bestBid;
                        bestBid.prev = newLvl;
                        bestBid = newLvl;
                    } else {
                        OrderBookLevel next = level.next;
                        level.next = newLvl;
                        newLvl.prev = level;
                        newLvl.next = next;
                    }
                    break;
                }
                if (level.next == null) {
                    last = level;
                }
                level = level.next;
            }
            if (bestBid == null) {
                bestBid = new OrderBookLevel(order);
            }
            if (addToLast && last != null) {
                OrderBookLevel newLevel = new OrderBookLevel(order);
                last.next = newLevel;
                newLevel.prev = last;
            }
        } else {
            OrderBookLevel level = bestAsk;
            OrderBookLevel last = null;
            boolean addToLast = true;
            while (level != null) {
                // append order to existing level
                if (order.getPrice().compareTo(level.getPrice()) == 0) {
                    addToLast = false;
                    level.add(order);
                    break;
                }
                // insert new level
                if (order.getPrice().compareTo(level.getPrice()) < 0) {
                    addToLast = false;
                    OrderBookLevel newLvl = new OrderBookLevel(order);
                    if (level.prev == null){
                        newLvl.next = bestAsk;
                        bestAsk.prev = newLvl;
                        bestAsk = newLvl;
                    } else {
                        OrderBookLevel next = level.next;
                        level.next = newLvl;
                        newLvl.prev = level;
                        newLvl.next = next;
                    }
                    break;
                }
                if (level.next == null) {
                    last = level;
                }
                level = level.next;
            }
            if (bestAsk == null) {
                bestAsk = new OrderBookLevel(order);
            }
            if (addToLast && last != null) {
                OrderBookLevel newLevel = new OrderBookLevel(order);
                last.next = newLevel;
                newLevel.prev = last;
            }
        }
        return true;
    }

    @Override
    public boolean update(Order order) {
        final long orderId = order.getOrderId();
        Order o = orderIdMap.get(orderId);
        if (o == null) {
            return false;
        }
        // if we change price we need to move order into new price level
        if (order.getPrice().compareTo(o.getPrice()) != 0) {
            // remove and add
            remove(orderId);
            add(order);
        } else {
            // if we change quantity, just change on order
            order.setQuoteOrderQty(order.getQuoteOrderQty());
        }
        return true;
    }

    @Override
    public boolean remove(long orderId) {
        Order order = orderIdMap.remove(orderId);
        if (order == null) {
            return false;
        }
        PriceLevel level = order.level;
        level.remove(order);
        return true;
    }

    @Override
    public Order getOrder(long orderId) {
        return orderIdMap.get(orderId);
    }

    @Override
    public MarketData buildMarketData() {
        // find number & bids & asks
        int bidSize = 0, askSize = 0;

        OrderBookLevel bidLvl = bestBid;
        while (bidLvl != null) {
            bidSize++;
            bidLvl = bidLvl.next;
        }
        OrderBookLevel askLvl = bestAsk;
        while (askLvl != null) {
            askSize++;
            askLvl = askLvl.next;
        }
        bidSize = Math.min(bidSize, AppConstants.DEFAULT_DEPTH);
        askSize = Math.min(askSize, AppConstants.DEFAULT_DEPTH);
        int depth = Math.max(bidSize, askSize);
        MarketData md = new MarketData();
        md.setDepth(depth);
        md.setSymbol(symbol);
        md.setTransactTime(System.currentTimeMillis());

        BigDecimal[][] bids = new BigDecimal[bidSize][];
        BigDecimal[][] asks = new BigDecimal[askSize][];
        int bidIndex = 0, askIndex = 0;
        bidLvl = bestBid;
        while (bidLvl != null && bidIndex < bidSize) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            bidLvl.resetIterator();
            while (bidLvl.hasNext()) {
                cumulativeQuantity = cumulativeQuantity.add(bidLvl.next().getLeavesQty());
            }
            bids[bidIndex++] = new BigDecimal[]{bidLvl.getPrice(), cumulativeQuantity};
            bidLvl = bidLvl.next;
        }
        askLvl = bestAsk;
        while (askLvl != null && askIndex < askSize) {
            BigDecimal cumulativeQuantity = BigDecimal.ZERO;
            askLvl.resetIterator();
            while (askLvl.hasNext()) {
                cumulativeQuantity = cumulativeQuantity.add(askLvl.next().getLeavesQty());
            }
            asks[askIndex++] = new BigDecimal[]{askLvl.getPrice(), cumulativeQuantity};
            askLvl = askLvl.next;
        }

        md.setBids(bids);
        md.setAsks(asks);
        return md;
    }
}