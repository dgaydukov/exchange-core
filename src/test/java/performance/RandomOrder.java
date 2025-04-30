package performance;

import com.exchange.core.MockData;
import com.exchange.core.model.enums.OrderSide;
import com.exchange.core.model.enums.OrderType;
import com.exchange.core.model.msg.Order;
import java.math.BigDecimal;
import java.util.Random;

public class RandomOrder {

  private final static Random random = new Random();
  private static long orderId;

  public static Order buyLimitUser1() {
    Order order = new Order();
    order.setOrderId(orderId++);
    order.setSymbol(MockData.SYMBOL);
    order.setType(OrderType.LIMIT);
    order.setSide(OrderSide.BUY);
    order.setAccount(1);
    order.setLeavesQty(getQuantity());
    order.setPrice(getPrice());
    return order;
  }

  public static Order buyMarketUser1() {
    Order order = buyLimitUser1();
    order.setType(OrderType.MARKET);
    order.setPrice(null);
    return order;
  }

  public static Order sellLimitUser2() {
    Order order = buyLimitUser1();
    order.setAccount(2);
    order.setSide(OrderSide.SELL);
    return order;
  }

  public static Order sellMarketUser1() {
    Order order = sellLimitUser2();
    order.setType(OrderType.MARKET);
    order.setPrice(null);
    return order;
  }

  private static BigDecimal getPrice() {
    int next = random.nextInt(100, 100_000);
    return new BigDecimal(next);
  }

  private static BigDecimal getQuantity() {
    int next = random.nextInt(1, 1_000);
    return new BigDecimal(next);
  }
}
