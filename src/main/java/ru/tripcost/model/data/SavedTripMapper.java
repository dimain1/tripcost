package ru.tripcost.model.data;

import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripBuilder;
import ru.tripcost.model.domain.TripOption;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Преобразование шаблона в записи файла настроек и обратно.
 *
 * <p>Каждый шаблон занимает группу строк с общим префиксом {@code trip.N.}:
 * {@code trip.1.name=Командировка}, {@code trip.1.from=Ставрополь} и т.д.
 * Экранирование символов и кодировку берёт на себя {@link Properties}.</p>
 */
final class SavedTripMapper {

    private SavedTripMapper() {
    }

    static void write(Properties props, int index, SavedTrip saved) {
        String p = "trip." + index + ".";
        Trip t = saved.trip();
        props.setProperty(p + "name", saved.name());
        props.setProperty(p + "savedAt", saved.savedAt().toString());
        props.setProperty(p + "from", t.getOrigin());
        props.setProperty(p + "to", t.getDestination());
        props.setProperty(p + "distanceKm", Double.toString(t.getDistanceKm()));
        props.setProperty(p + "transport", t.getTransport().name());
        props.setProperty(p + "passengers", Integer.toString(t.getPassengers()));
        props.setProperty(p + "fuelConsumption", Double.toString(t.getFuelConsumption()));
        props.setProperty(p + "roundTrip", Boolean.toString(t.isRoundTrip()));
        props.setProperty(p + "tolls", t.getTollCost().toPlainString());
        props.setProperty(p + "nights", Integer.toString(t.getNights()));
        props.setProperty(p + "pricePerNight", t.getPricePerNight().toPlainString());
        props.setProperty(p + "options", t.getOptions().stream().map(Enum::name).collect(Collectors.joining(",")));
    }

    static SavedTrip read(Properties props, int index) {
        String p = "trip." + index + ".";
        Map<String, String> v = new java.util.HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(p)) {
                v.put(key.substring(p.length()), props.getProperty(key));
            }
        }
        TripBuilder builder = Trip.builder()
                .from(v.get("from")).to(v.get("to"))
                .distance(Double.parseDouble(v.get("distanceKm")))
                .by(TransportType.valueOf(v.get("transport")))
                .passengers(Integer.parseInt(v.get("passengers")))
                .fuelConsumption(Double.parseDouble(v.get("fuelConsumption")))
                .roundTrip(Boolean.parseBoolean(v.get("roundTrip")))
                .tolls(new BigDecimal(v.get("tolls")))
                .accommodation(Integer.parseInt(v.get("nights")), new BigDecimal(v.get("pricePerNight")))
                .options(options(v.getOrDefault("options", "")));
        return new SavedTrip(v.get("name"), builder.build(), LocalDateTime.parse(v.get("savedAt")));
    }

    private static Set<TripOption> options(String text) {
        Set<TripOption> set = EnumSet.noneOf(TripOption.class);
        for (String part : text.split(",")) {
            if (!part.isBlank()) {
                set.add(TripOption.valueOf(part.trim()));
            }
        }
        return set;
    }
}
