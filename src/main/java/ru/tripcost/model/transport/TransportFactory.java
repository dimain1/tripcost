package ru.tripcost.model.transport;

import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Создатель (Creator) паттерна «Фабричный метод».
 *
 * <p>Объявляет фабричный метод {@link #createTransport()}, который реализуют
 * подклассы, и операцию {@link #transportCost(Trip)}, которая работает
 * с созданным транспортом только через интерфейс {@link Transport}
 * и не знает его конкретного класса.</p>
 */
public abstract class TransportFactory {

    /** Фабричный метод: какой транспорт создать, решает подкласс. */
    public abstract Transport createTransport();

    /**
     * Транспорт для конкретной поездки: создаётся фабричным методом
     * и оборачивается декораторами выбранных услуг.
     */
    public Transport createFor(Trip trip) {
        Transport transport = createTransport();
        if (transport.getType() != trip.getTransport()) {
            throw new IllegalArgumentException("Фабрика создаёт «" + transport.getType()
                    + "», а поездка на «" + trip.getTransport() + "»");
        }
        return TransportDecorators.decorate(transport, trip.getOptions());
    }

    /** Стоимость проезда по поездке с учётом услуг. */
    public BigDecimal transportCost(Trip trip) {
        return createFor(trip).cost(trip);
    }
}
