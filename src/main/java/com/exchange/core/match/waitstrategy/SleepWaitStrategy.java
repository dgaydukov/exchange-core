package com.exchange.core.match.waitstrategy;

public class SleepWaitStrategy implements WaitStrategy{
    @Override
    public void idle() {
        try{
            Thread.sleep(1);
        } catch (InterruptedException ex){
            throw new RuntimeException(ex);
        }
    }
}
