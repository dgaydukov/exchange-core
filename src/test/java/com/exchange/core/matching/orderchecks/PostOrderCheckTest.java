package com.exchange.core.matching.orderchecks;

import com.exchange.core.MockData;
import com.exchange.core.matching.counter.GlobalCounter;
import com.exchange.core.model.enums.OrderStatus;
import com.exchange.core.model.msg.ExecutionReport;
import com.exchange.core.model.msg.Message;
import com.exchange.core.model.msg.Order;
import com.exchange.core.repository.AccountRepository;
import com.exchange.core.repository.InstrumentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostOrderCheckTest {
    @Test
    public void sendExecReportNewTest(){
        GlobalCounter counter = mock(GlobalCounter.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        InstrumentRepository instrumentRepository = mock(InstrumentRepository.class);
        Queue<Message> outbound = mock(Queue.class);
        PostOrderCheck postCheck = new PostOrderCheckImpl(counter, accountRepository, instrumentRepository, outbound);

        Order order = MockData.getLimitBuy();
        final long execId = 100;
        when(counter.getNextOrderId()).thenReturn(execId);
        when(counter.getNextExecutionId()).thenReturn(execId);
        postCheck.sendExecReportNew(order);
        ArgumentCaptor<ExecutionReport> argument = ArgumentCaptor.forClass(ExecutionReport.class);
        verify(outbound).add(argument.capture());
        ExecutionReport exec = argument.getValue();
        Assertions.assertEquals(order.getSymbol(), exec.getSymbol(), "symbol mismatch");
        Assertions.assertEquals(execId, exec.getExecId(), "execId mismatch");
        Assertions.assertEquals(execId, exec.getOrderId(), "orderId mismatch");
        Assertions.assertEquals(order.getOrderQty(), exec.getOrderQty(), "orderQty mismatch");
        Assertions.assertEquals(order.getPrice(), exec.getPrice(), "price mismatch");
        Assertions.assertEquals(OrderStatus.NEW, exec.getStatus(), "status should be new");
    }
}
