package ru.tripcost.model.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Заместитель удалённого сервиса расстояний.
 *
 * <p><b>Паттерн «Заместитель» (Proxy).</b> Реализует тот же интерфейс
 * {@link DistanceService}, что и реальный сервис (адаптер геобиблиотеки),
 * поэтому клиент не отличает заместителя от настоящего объекта. Заместитель
 * контролирует доступ к реальному сервису:</p>
 * <ul>
 *     <li>виртуальный заместитель - реальный сервис создаётся только при первом
 *     обращении;</li>
 *     <li>кэширующий заместитель - расстояние между парой городов запрашивается
 *     у геосервиса один раз, повторные запросы (в том числе в обратном
 *     направлении) обслуживаются из кэша;</li>
 *     <li>учёт обращений - заместитель считает запросы, попадания в кэш
 *     и обращения к реальному сервису.</li>
 * </ul>
 */
public class CachingDistanceProxy implements DistanceService {

    private final Supplier<? extends DistanceService> realServiceFactory;
    private volatile DistanceService realService;

    /** Потокобезопасные кэш и счётчики: заместителя вызывают из пула потоков. */
    private final Map<String, RouteDistance> cache = new ConcurrentHashMap<>();
    private volatile List<String> knownCities;

    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger realCalls = new AtomicInteger();

    /**
     * @param realServiceFactory создаёт реальный сервис; вызывается не раньше
     *                           первого обращения к заместителю
     */
    public CachingDistanceProxy(Supplier<? extends DistanceService> realServiceFactory) {
        this.realServiceFactory = Objects.requireNonNull(realServiceFactory);
    }

    @Override
    public RouteDistance distance(String from, String to) {
        requests.incrementAndGet();
        String key = key(from, to);
        RouteDistance cached = cache.get(key);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }
        realCalls.incrementAndGet();
        // ошибки (неизвестный город) не кэшируются: исключение уходит клиенту
        RouteDistance result = real().distance(from, to);
        cache.put(key, result);
        return result;
    }

    @Override
    public List<String> knownCities() {
        if (knownCities == null) {
            knownCities = List.copyOf(real().knownCities());
        }
        return knownCities;
    }

    /** Сбрасывает кэш расстояний и счётчики; реальный сервис сохраняется. */
    public void clearCache() {
        cache.clear();
        requests.set(0);
        cacheHits.set(0);
        realCalls.set(0);
    }

    public Stats stats() {
        return new Stats(requests.get(), cacheHits.get(), realCalls.get(), cache.size(), realService != null);
    }

    /** Реальный сервис создаётся лениво (виртуальный заместитель). */
    private DistanceService real() {
        DistanceService service = realService;
        if (service == null) {
            synchronized (this) {                 // создать ровно один раз даже из нескольких потоков
                if (realService == null) {
                    realService = realServiceFactory.get();
                }
                service = realService;
            }
        }
        return service;
    }

    /** Расстояние симметрично, поэтому ключ не зависит от направления. */
    private static String key(String from, String to) {
        String a = GeoApiDistanceAdapter.normalize(from);
        String b = GeoApiDistanceAdapter.normalize(to);
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /**
     * Статистика заместителя.
     *
     * @param requests     всего запросов расстояния
     * @param cacheHits    ответов из кэша
     * @param realCalls    обращений к реальному сервису
     * @param cachedRoutes пар городов в кэше
     * @param realCreated  создан ли реальный сервис
     */
    public record Stats(int requests, int cacheHits, int realCalls, int cachedRoutes, boolean realCreated) {
    }
}
