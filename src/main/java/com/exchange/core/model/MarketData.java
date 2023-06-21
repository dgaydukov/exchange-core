package com.exchange.core.model;

import lombok.Data;

@Data
public class MarketData {
    private int[][] bids;
    private int[][] asks;
}
