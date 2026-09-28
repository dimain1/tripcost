package ru.tripcost.external.geo;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Имитация сторонней геобиблиотеки (Adaptee для паттерна «Адаптер»).
 *
 * <p>Интерфейс библиотеки несовместим с приложением: города ищутся по
 * англоязычным названиям, неизвестный город возвращает {@code null},
 * а расстояние между точками считается по дуге большого круга в милях.
 * Изменять этот класс нельзя - так ведёт себя «чужой» код.</p>
 */
public class WorldGeoApi {

    private static final double EARTH_RADIUS_MILES = 3958.8;

    private static final Map<String, GeoPoint> CITIES = Map.ofEntries(
            Map.entry("Stavropol", new GeoPoint(45.0428, 41.9734)),
            Map.entry("Kislovodsk", new GeoPoint(43.9133, 42.7208)),
            Map.entry("Pyatigorsk", new GeoPoint(44.0486, 43.0594)),
            Map.entry("Essentuki", new GeoPoint(44.0446, 42.8589)),
            Map.entry("Mineralnye Vody", new GeoPoint(44.2103, 43.1353)),
            Map.entry("Nalchik", new GeoPoint(43.4853, 43.6071)),
            Map.entry("Cherkessk", new GeoPoint(44.2269, 42.0467)),
            Map.entry("Vladikavkaz", new GeoPoint(43.0205, 44.6819)),
            Map.entry("Makhachkala", new GeoPoint(42.9849, 47.5047)),
            Map.entry("Elista", new GeoPoint(46.3078, 44.2558)),
            Map.entry("Krasnodar", new GeoPoint(45.0355, 38.9753)),
            Map.entry("Sochi", new GeoPoint(43.5855, 39.7231)),
            Map.entry("Anapa", new GeoPoint(44.895, 37.3163)),
            Map.entry("Gelendzhik", new GeoPoint(44.561, 38.0766)),
            Map.entry("Rostov-on-Don", new GeoPoint(47.2357, 39.7015)),
            Map.entry("Volgograd", new GeoPoint(48.708, 44.5133)),
            Map.entry("Astrakhan", new GeoPoint(46.3497, 48.0408)),
            Map.entry("Voronezh", new GeoPoint(51.6608, 39.2003)),
            Map.entry("Moscow", new GeoPoint(55.7558, 37.6173)),
            Map.entry("Saint Petersburg", new GeoPoint(59.9343, 30.3351)),
            Map.entry("Kazan", new GeoPoint(55.7887, 49.1221)),
            Map.entry("Nizhny Novgorod", new GeoPoint(56.3269, 44.0059)),
            Map.entry("Samara", new GeoPoint(53.1959, 50.1002)),
            Map.entry("Yekaterinburg", new GeoPoint(56.8389, 60.6057)),
            Map.entry("Novosibirsk", new GeoPoint(55.0084, 82.9357))
    );

    /** Координаты города по англоязычному названию или {@code null}, если город неизвестен. */
    public GeoPoint findCity(String englishName) {
        return CITIES.get(englishName);
    }

    /** Расстояние по дуге большого круга (формула гаверсинусов), мили. */
    public double greatCircleMiles(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_MILES * Math.asin(Math.sqrt(h));
    }

    public Set<String> supportedCities() {
        return new TreeSet<>(CITIES.keySet());
    }
}
