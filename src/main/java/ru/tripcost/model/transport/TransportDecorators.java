package ru.tripcost.model.transport;

import ru.tripcost.model.domain.TripOption;

import java.util.Set;

/**
 * Оборачивает транспорт декораторами выбранных услуг.
 * Порядок - как в перечислении {@link TripOption}: сезон, льгота, багаж, страховка.
 */
public final class TransportDecorators {

    private TransportDecorators() {
    }

    public static Transport decorate(Transport transport, Set<TripOption> options) {
        Transport result = transport;
        for (TripOption option : TripOption.values()) {
            if (options.contains(option)) {
                result = wrap(result, option);
            }
        }
        return result;
    }

    private static Transport wrap(Transport transport, TripOption option) {
        return switch (option) {
            case PEAK_SEASON -> new PeakSeasonDecorator(transport);
            case DISCOUNT -> new DiscountDecorator(transport);
            case BAGGAGE -> new BaggageDecorator(transport);
            case INSURANCE -> new InsuranceDecorator(transport);
        };
    }
}
