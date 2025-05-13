package com.exchange.core.matching.orderbook.ipq;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IndexedPriorityQueueTest {
    private IndexedPriorityQueue<Integer, String> queue;

    @BeforeEach
    public void beforeEach(){
        queue = new MapIndexedPriorityQueue<>(SortOrder.ASC, 10, 5);
    }

    @Test
    public void unlimitedGrowthTest(){
        Assertions.assertEquals(0, queue.size(), "size should be 0");
        for (int i = 0; i < 10 ; i++){
            Assertions.assertTrue(queue.offer(i, "a"), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");
        for (int i = 100; i < 200 ; i++){
            Assertions.assertTrue(queue.offer(i, "a"), "should add successfully");
        }
        Assertions.assertEquals(110, queue.size(), "size should be 110");
        for (int i = 1000; i < 2000 ; i++){
            Assertions.assertTrue(queue.offer(i, "a"), "should add successfully");
        }
        Assertions.assertEquals(1110, queue.size(), "size should be 1110");
    }

    @Test
    public void offerAndPollTest(){
        queue = new MapIndexedPriorityQueue<>(SortOrder.DESC, 10, 5);
        Assertions.assertEquals(0, queue.size(), "size should be 0");
        for (int i = 1; i <= 10 ; i++){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");
        for (int i = 10; i >0 ; i--){
            Assertions.assertEquals("msg_"+i, queue.poll(), "should add successfully");
        }
        Assertions.assertEquals(0, queue.size(), "size should be 0");

        // test reverse insert
        for (int i = 10; i > 0 ; i--){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");
        for (int i = 10; i >0 ; i--){
            Assertions.assertEquals("msg_"+i, queue.poll(), "should add successfully");
        }
        Assertions.assertEquals(0, queue.size(), "size should be 0");
    }

    @Test
    public void reverseOfferAndPollTest(){
        Assertions.assertEquals(0, queue.size(), "size should be 0");
        for (int i = 1; i <= 10 ; i++){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");
        for (int i = 1; i <= 10 ; i++){
            Assertions.assertEquals("msg_"+i, queue.poll(), "should add successfully");
        }
        Assertions.assertEquals(0, queue.size(), "size should be 0");

        // test reverse insert
        for (int i = 10; i > 0 ; i--){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");
        for (int i = 1; i <= 10 ; i++){
            Assertions.assertEquals("msg_"+i, queue.poll(), "should add successfully");
        }
        Assertions.assertEquals(0, queue.size(), "size should be 0");
    }

    @Test
    public void peekTest(){
        Assertions.assertEquals(0, queue.size(), "size should be 0");
        for (int i = 1; i <= 10 ; i++){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");

        Assertions.assertEquals("msg_1", queue.peek(), "peek mismatch");
        Assertions.assertEquals("msg_1", queue.peek(), "peek mismatch");
        Assertions.assertEquals(10, queue.size(), "size should be 10");
        Assertions.assertEquals("msg_1", queue.poll(), "poll mismatch");
        Assertions.assertEquals("msg_2", queue.peek(), "peek mismatch");
        Assertions.assertEquals("msg_2", queue.peek(), "peek mismatch");
        Assertions.assertEquals(9, queue.size(), "size should be 10");
    }

    @Test
    public void getExactTest(){
        Assertions.assertEquals(0, queue.size(), "size should be 0");
        for (int i = 1; i <= 20 ; i++){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(20, queue.size(), "size should be 20");
        Assertions.assertEquals("msg_1", queue.getExact(1), "getExact mismatch");
        Assertions.assertEquals("msg_10", queue.getExact(10), "getExact mismatch");
        Assertions.assertEquals("msg_20", queue.getExact(20), "getExact mismatch");
        Assertions.assertNull(queue.getExact(0), "getExact mismatch");
        Assertions.assertNull(queue.getExact(21), "getExact mismatch");
        Assertions.assertNull(queue.getExact(100), "getExact mismatch");

        Assertions.assertEquals("msg_1", queue.getExact(1), "getExact mismatch");
        Assertions.assertEquals("msg_1", queue.poll(), "poll mismatch");
        Assertions.assertNull(queue.getExact(1), "getExact mismatch");
        Assertions.assertEquals(19, queue.size(), "size should be 19");

        Assertions.assertEquals("msg_2", queue.getExact(2), "getExact mismatch");
        Assertions.assertEquals("msg_2", queue.poll(), "poll mismatch");
        Assertions.assertNull(queue.getExact(2), "getExact mismatch");
        Assertions.assertEquals(18, queue.size(), "size should be 18");
    }

    @Test
    public void iterationTest(){
        Assertions.assertEquals(0, queue.size(), "size should be 0");
        for (int i = 1; i <= 10 ; i++){
            Assertions.assertTrue(queue.offer(i, "msg_"+i), "should add successfully");
        }
        Assertions.assertEquals(10, queue.size(), "size should be 10");

        queue.resetIterator();
        for (int i = 1; i <= 10; i++){
            Assertions.assertTrue(queue.hasNext(), "hasNext should return true");
            Assertions.assertEquals("msg_"+i, queue.next(), "next mismatch");
        }
        Assertions.assertFalse(queue.hasNext(), "hasNext should return false");
        Assertions.assertNull(queue.next(), "next should return null");

    }
}
