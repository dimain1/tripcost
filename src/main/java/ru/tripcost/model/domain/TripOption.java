package ru.tripcost.model.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Дополнительная услуга к проезду. Каждая услуга реализована декоратором транспорта.
 * Порядок констант задаёт порядок применения декораторов.
 */
public enum TripOption {

    PEAK_SEASON("Высокий сезон", EnumSet.of(TransportType.TAXI, TransportType.BUS,
            TransportType.TRAIN, TransportType.PLANE)),
    DISCOUNT("Льготный тариф", EnumSet.of(TransportType.BUS, TransportType.TRAIN, TransportType.PLANE)),
    BAGGAGE("Багаж", EnumSet.of(TransportType.BUS, TransportType.TRAIN, TransportType.PLANE)),
    INSURANCE("Страховка", EnumSet.allOf(TransportType.class));

    private final String displayName;
    private final Set<TransportType> applicable;

    TripOption(String displayName, Set<TransportType> applicable) {
        this.displayName = displayName;
        this.applicable = applicable;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Можно ли подключить услугу к этому виду транспорта. */
    public boolean appliesTo(TransportType type) {
        return type != null && applicable.contains(type);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
