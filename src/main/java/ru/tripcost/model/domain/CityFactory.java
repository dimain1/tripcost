package ru.tripcost.model.domain;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Locale;
import java.util.Map;

/**
 * Фабрика приспособленцев: пул городов.
 *
 * <p>{@link #get(String)} возвращает уже созданный объект {@link City}, если город
 * запрашивали раньше, и создаёт новый только при первом обращении. Поэтому
 * тысячи маршрутов ссылаются на два-три десятка объектов городов.</p>
 */
public class CityFactory {

    /** Справочник: город → {регион, федеральный округ}. */
    private static final Map<String, String[]> DIRECTORY = Map.ofEntries(
            Map.entry("Ставрополь", new String[] {"Ставропольский край", "СКФО"}),
            Map.entry("Кисловодск", new String[] {"Ставропольский край", "СКФО"}),
            Map.entry("Пятигорск", new String[] {"Ставропольский край", "СКФО"}),
            Map.entry("Ессентуки", new String[] {"Ставропольский край", "СКФО"}),
            Map.entry("Минеральные Воды", new String[] {"Ставропольский край", "СКФО"}),
            Map.entry("Нальчик", new String[] {"Кабардино-Балкарская Республика", "СКФО"}),
            Map.entry("Черкесск", new String[] {"Карачаево-Черкесская Республика", "СКФО"}),
            Map.entry("Владикавказ", new String[] {"Республика Северная Осетия - Алания", "СКФО"}),
            Map.entry("Махачкала", new String[] {"Республика Дагестан", "СКФО"}),
            Map.entry("Элиста", new String[] {"Республика Калмыкия", "ЮФО"}),
            Map.entry("Краснодар", new String[] {"Краснодарский край", "ЮФО"}),
            Map.entry("Сочи", new String[] {"Краснодарский край", "ЮФО"}),
            Map.entry("Анапа", new String[] {"Краснодарский край", "ЮФО"}),
            Map.entry("Геленджик", new String[] {"Краснодарский край", "ЮФО"}),
            Map.entry("Ростов-на-Дону", new String[] {"Ростовская область", "ЮФО"}),
            Map.entry("Волгоград", new String[] {"Волгоградская область", "ЮФО"}),
            Map.entry("Астрахань", new String[] {"Астраханская область", "ЮФО"}),
            Map.entry("Воронеж", new String[] {"Воронежская область", "ЦФО"}),
            Map.entry("Москва", new String[] {"Москва", "ЦФО"}),
            Map.entry("Санкт-Петербург", new String[] {"Санкт-Петербург", "СЗФО"}),
            Map.entry("Казань", new String[] {"Республика Татарстан", "ПФО"}),
            Map.entry("Нижний Новгород", new String[] {"Нижегородская область", "ПФО"}),
            Map.entry("Самара", new String[] {"Самарская область", "ПФО"}),
            Map.entry("Екатеринбург", new String[] {"Свердловская область", "УФО"}),
            Map.entry("Новосибирск", new String[] {"Новосибирская область", "СФО"})
    );

    /** Пул потокобезопасный: города запрашиваются и из пула потоков (расчёт направлений). */
    private final Map<String, City> pool = new ConcurrentHashMap<>();
    private final AtomicInteger requests = new AtomicInteger();

    /** Возвращает общий объект города (создаёт при первом запросе). */
    public City get(String name) {
        requests.incrementAndGet();
        String key = normalize(name);
        return pool.computeIfAbsent(key, k -> create(name.trim()));
    }

    private static City create(String name) {
        String[] info = DIRECTORY.entrySet().stream()
                .filter(e -> normalize(e.getKey()).equals(normalize(name)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(new String[] {"не указан", "не указан"});
        String canonical = DIRECTORY.keySet().stream()
                .filter(k -> normalize(k).equals(normalize(name))).findFirst().orElse(name);
        return new City(canonical, info[0], info[1]);
    }

    /** Сколько разных объектов City создано. */
    public int createdCount() {
        return pool.size();
    }

    /** Сколько раз к фабрике обращались за городом. */
    public int requestCount() {
        return requests.get();
    }

    private static String normalize(String name) {
        String s = name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        return s.startsWith("г. ") ? s.substring(3).trim() : s;
    }
}
