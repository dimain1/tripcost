package ru.tripcost.model.concurrent;

import org.junit.jupiter.api.Test;
import ru.tripcost.external.geo.WorldGeoApi;
import ru.tripcost.model.domain.City;
import ru.tripcost.model.domain.CityFactory;
import ru.tripcost.model.service.CachingDistanceProxy;
import ru.tripcost.model.service.GeoApiDistanceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ThreadSafetyTest {

    @Test
    void proxyCountersAreExactUnderConcurrency() {
        AtomicInteger created = new AtomicInteger();
        CachingDistanceProxy proxy = new CachingDistanceProxy(() -> {
            created.incrementAndGet();
            return new GeoApiDistanceAdapter(new WorldGeoApi());
        });
        QuotePool pool = new QuotePool(8);
        List<Supplier<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            tasks.add(() -> { proxy.distance("Ставрополь", "Москва"); return 1; });
        }
        int total = new AsyncJob<>(tasks, pool, list -> list.size(), ProgressListener.NONE).result().join();
        pool.shutdown();

        assertEquals(400, total);
        assertEquals(1, created.get());                   // реальный сервис создан один раз
        CachingDistanceProxy.Stats stats = proxy.stats();
        assertEquals(400, stats.requests());               // ни один запрос не потерян
        assertEquals(400, stats.cacheHits() + stats.realCalls());
        assertEquals(1, stats.cachedRoutes());
    }

    @Test
    void flyweightReturnsSameCityFromDifferentThreads() {
        QuotePool pool = new QuotePool(8);
        List<CompletableFuture<City>> futures = new ArrayList<>();
        CityFactory factory = new CityFactory();
        for (int i = 0; i < 200; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> factory.get("Пятигорск"),
                    pool.executor()));
        }
        City first = futures.get(0).join();
        futures.forEach(f -> assertSame(first, f.join()));
        pool.shutdown();
    }
}
