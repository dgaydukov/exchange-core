package com.exchange.core.matching.waitstrategy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WaitStrategyTest {
    @Test
    public void sleepWaitStrategyTest(){
        WaitStrategy waitStrategy = new SleepWaitStrategy();
        long time = System.currentTimeMillis();
        for(int i = 0; i < 1000; i++){
            waitStrategy.idle();
        }
        long diff = System.currentTimeMillis() - time;
        Assertions.assertTrue(diff > 1000, "waiting should be more then 1000 ms: diff="+diff);
        // ideally since idle is 1ms, 1000-loop shouldn't be than 2000 combined, yet if we hit this problem we can increase it to 3000
        Assertions.assertTrue(diff < 2000, "waiting should be more then 2000 ms: diff="+diff);
    }
}
