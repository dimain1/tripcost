package ru.tripcost.model.transport;

import ru.tripcost.model.domain.CostGroup;
import ru.tripcost.model.domain.CostLeaf;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Базовый декоратор транспорта.
 *
 * <p><b>Паттерн «Декоратор» (Decorator).</b> Декоратор реализует тот же интерфейс
 * {@link Transport}, что и оборачиваемый объект, и передаёт ему все вызовы,
 * добавляя к стоимости свою часть. Декораторы можно вкладывать друг в друга
 * в любом количестве, поэтому набор дополнительных услуг выбирается во время
 * работы программы, без подклассов на каждое сочетание.</p>
 */
public abstract class TransportDecorator implements Transport {

    protected final Transport inner;

    protected TransportDecorator(Transport inner) {
        this.inner = Objects.requireNonNull(inner);
    }

    /** Надбавка (или скидка со знаком минус) к стоимости обёрнутого транспорта. */
    protected abstract BigDecimal extra(Trip trip, BigDecimal innerCost);

    /** Название статьи расходов для этой услуги. */
    protected abstract String label();

    @Override
    public BigDecimal cost(Trip trip) {
        return breakdown(trip).amount();
    }

    /** Статьи обёрнутого транспорта плюс статья этой услуги. */
    @Override
    public CostGroup breakdown(Trip trip) {
        CostGroup inside = inner.breakdown(trip);
        BigDecimal add = extra(trip, inside.amount()).setScale(2, RoundingMode.HALF_UP);
        return inside.with(new CostLeaf(label(), add));
    }

    @Override
    public TransportType getType() {
        return inner.getType();
    }

    @Override
    public String costLabel() {
        return inner.costLabel();
    }

    @Override
    public String describeBaseFee() {
        return inner.describeBaseFee();
    }

    @Override
    public String describePerKm() {
        return inner.describePerKm();
    }

    /** Процент от суммы. */
    protected static BigDecimal percent(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
