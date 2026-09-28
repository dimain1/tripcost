package ru.tripcost.model.transport;

import ru.tripcost.model.domain.TransportType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Соответствие «вид транспорта → фабрика». Клиенты получают здесь нужного
 * создателя и дальше работают с ним через {@link TransportFactory}.
 */
public final class TransportFactories {

    private static final Map<TransportType, TransportFactory> FACTORIES;

    static {
        Map<TransportType, TransportFactory> map = new EnumMap<>(TransportType.class);
        map.put(TransportType.CAR, new CarFactory());
        map.put(TransportType.TAXI, new TaxiFactory());
        map.put(TransportType.BUS, new BusFactory());
        map.put(TransportType.TRAIN, new TrainFactory());
        map.put(TransportType.PLANE, new PlaneFactory());
        FACTORIES = Collections.unmodifiableMap(map);
    }

    private TransportFactories() {
    }

    public static TransportFactory forType(TransportType type) {
        TransportFactory factory = FACTORIES.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Нет фабрики для вида транспорта " + type);
        }
        return factory;
    }
}
