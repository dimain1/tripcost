package ru.tripcost.model.service;

import ru.tripcost.external.geo.GeoPoint;
import ru.tripcost.external.geo.WorldGeoApi;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Адаптер сторонней геобиблиотеки {@link WorldGeoApi} к интерфейсу {@link DistanceService}.
 *
 * <p><b>Паттерн «Адаптер» (Adapter).</b> Приложение работает с русскими названиями
 * и километрами, а библиотека - с англоязычными названиями, милями и {@code null}
 * вместо ошибки. Адаптер переводит названия, вызывает два метода библиотеки,
 * переводит мили в километры и оценивает расстояние по дорогам.</p>
 */
public class GeoApiDistanceAdapter implements DistanceService {

    static final double KM_PER_MILE = 1.609344;
    /** Во сколько раз путь по дорогам в среднем длиннее прямой. */
    static final double ROAD_FACTOR = 1.3;

    /** Русское название города → название в библиотеке. */
    private static final Map<String, String> RUSSIAN_NAMES = Map.ofEntries(
            Map.entry("Ставрополь", "Stavropol"),
            Map.entry("Кисловодск", "Kislovodsk"),
            Map.entry("Пятигорск", "Pyatigorsk"),
            Map.entry("Ессентуки", "Essentuki"),
            Map.entry("Минеральные Воды", "Mineralnye Vody"),
            Map.entry("Нальчик", "Nalchik"),
            Map.entry("Черкесск", "Cherkessk"),
            Map.entry("Владикавказ", "Vladikavkaz"),
            Map.entry("Махачкала", "Makhachkala"),
            Map.entry("Элиста", "Elista"),
            Map.entry("Краснодар", "Krasnodar"),
            Map.entry("Сочи", "Sochi"),
            Map.entry("Анапа", "Anapa"),
            Map.entry("Геленджик", "Gelendzhik"),
            Map.entry("Ростов-на-Дону", "Rostov-on-Don"),
            Map.entry("Волгоград", "Volgograd"),
            Map.entry("Астрахань", "Astrakhan"),
            Map.entry("Воронеж", "Voronezh"),
            Map.entry("Москва", "Moscow"),
            Map.entry("Санкт-Петербург", "Saint Petersburg"),
            Map.entry("Казань", "Kazan"),
            Map.entry("Нижний Новгород", "Nizhny Novgorod"),
            Map.entry("Самара", "Samara"),
            Map.entry("Екатеринбург", "Yekaterinburg"),
            Map.entry("Новосибирск", "Novosibirsk")
    );

    /** То же, но ключи нормализованы: нижний регистр, «ё» → «е». */
    private static final Map<String, String> LOOKUP = buildLookup();

    private final WorldGeoApi api;
    /** Имитация времени сетевого запроса к геосервису, мс. */
    private final long latencyMs;

    public GeoApiDistanceAdapter(WorldGeoApi api) {
        this(api, 0);
    }

    public GeoApiDistanceAdapter(WorldGeoApi api, long latencyMs) {
        this.api = Objects.requireNonNull(api);
        this.latencyMs = Math.max(0, latencyMs);
    }

    @Override
    public RouteDistance distance(String from, String to) {
        GeoPoint a = locate(from);
        GeoPoint b = locate(to);
        if (a.equals(b)) {
            throw new IllegalArgumentException("Пункты отправления и назначения совпадают");
        }
        simulateNetwork();
        double straightKm = api.greatCircleMiles(a, b) * KM_PER_MILE;
        return new RouteDistance(Math.round(straightKm), Math.round(straightKm * ROAD_FACTOR));
    }

    @Override
    public List<String> knownCities() {
        return RUSSIAN_NAMES.keySet().stream().sorted().toList();
    }

    /** Русское название → точка библиотеки; {@code null} библиотеки превращается в исключение. */
    private GeoPoint locate(String russianName) {
        String english = LOOKUP.get(normalize(russianName));
        GeoPoint point = english == null ? null : api.findCity(english);
        if (point == null) {
            throw new IllegalArgumentException("Город «" + (russianName == null ? "" : russianName.trim())
                    + "» не найден в геосервисе. Доступны: " + String.join(", ", knownCities()));
        }
        return point;
    }

    /** Геосервис удалённый: каждый запрос занимает время. */
    private void simulateNetwork() {
        if (latencyMs == 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String s = name.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        return s.startsWith("г. ") ? s.substring(3).trim() : s;
    }

    private static Map<String, String> buildLookup() {
        Map<String, String> map = new HashMap<>();
        RUSSIAN_NAMES.forEach((ru, en) -> map.put(normalize(ru), en));
        return Map.copyOf(map);
    }
}
