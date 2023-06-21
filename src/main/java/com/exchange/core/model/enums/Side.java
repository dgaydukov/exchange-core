package com.exchange.core.model.enums;

public enum Side {
    BUY(1),
    SELL(2);

    private int side;

    Side(int side){
        this.side = side;
    }

    public static Side fromValue(int side) {
        for (var e: Side.values()) {
            if (side == e.side) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown side=" + side);
    }
}
