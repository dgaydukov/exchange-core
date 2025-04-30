package performance;

import com.exchange.core.MockData;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.Random;

public class RandomOrder {

  private final static Random random = new Random();

  public static Order buyLimitUser1() {
    Order order = new Order();
    order.setSymbol(MockData.SYMBOL);
    order.setType(OrderType.LIMIT);
    order.setSide(OrderSide.BUY);
    order.setAccount(1);
    order.setOrderQty(getQuantity());
    order.setLeavesQty(order.getOrderQty());
    order.setPrice(getPrice());
    return order;
  }

  public static Order sellLimitUser2() {
    Order order = buyLimitUser1();
    order.setAccount(2);
    order.setSide(OrderSide.SELL);
    return order;
  }

  private static BigDecimal getPrice() {
    int next = random.nextInt(100, 10_000);
    return new BigDecimal(next);
  }

  private static BigDecimal getQuantity() {
    int next = random.nextInt(100, 10_000);
    return new BigDecimal(next);
  }
}
