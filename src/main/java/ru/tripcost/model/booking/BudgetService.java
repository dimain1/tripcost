package ru.tripcost.model.booking;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Сервис бюджета отдела: списывает деньги на командировку, если их хватает.
 */
public class BudgetService extends RemoteService {

    private BigDecimal balance;
    private final Map<String, BigDecimal> charges = new HashMap<>();

    public BudgetService(BigDecimal balance) {
        super("П");
        this.balance = balance;
    }

    public String charge(BookingOrder order) {
        if (order.total().compareTo(balance) > 0) {
            throw new ServiceException("Недостаточно средств в бюджете отдела: нужно "
                    + order.total().toPlainString() + " ₽, осталось " + balance.toPlainString() + " ₽");
        }
        String id = register("списание " + order.total().toPlainString() + " ₽", "Сервис бюджета недоступен");
        balance = balance.subtract(order.total());
        charges.put(id, order.total());
        return id;
    }

    /** Компенсация: возврат денег в бюджет. */
    public void refund(String paymentId) {
        BigDecimal amount = charges.remove(paymentId);
        if (amount != null) {
            balance = balance.add(amount);
            unregister(paymentId);
        }
    }

    public BigDecimal balance() {
        return balance;
    }
}
