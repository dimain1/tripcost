package ru.tripcost.model.service;

import org.junit.jupiter.api.Test;
import ru.tripcost.external.geo.WorldGeoApi;
import ru.tripcost.model.domain.TransportType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CachingDistanceProxyTest {

    /** Реальный сервис, который считает обращения к себе. */
    private static final class CountingService implements DistanceService {
        private final DistanceService real = new GeoApiDistanceAdapter(new WorldGeoApi());
        int calls;

        @Override
        public RouteDistance distance(String from, String to) {
            calls++;
            return real.distance(from, to);
        }

        @Override
        public List<String> knownCities() {
            return real.knownCities();
        }
    }

    private final CountingService service = new CountingService();
    private final CachingDistanceProxy proxy = new CachingDistanceProxy(() -> service);

    @Test
    void proxyReturnsSameResultAsRealService() {
        DistanceService real = new GeoApiDistanceAdapter(new WorldGeoApi());
        assertEquals(real.distance("Ставрополь", "Москва"), proxy.distance("Ставрополь", "Москва"));
        assertEquals(real.knownCities(), proxy.knownCities());
    }

    @Test
    void realServiceIsCreatedOnFirstRequestOnly() {
        AtomicInteger created = new AtomicInteger();
        CachingDistanceProxy lazy = new CachingDistanceProxy(() -> {
            created.incrementAndGet();
            return service;
        });
        assertEquals(0, created.get());
        assertFalse(lazy.stats().realCreated());

        lazy.distance("Ставрополь", "Сочи");
        lazy.distance("Ставрополь", "Анапа");
        assertEquals(1, created.get());
        assertTrue(lazy.stats().realCreated());
    }

    @Test
    void repeatedRequestIsServedFromCache() {
        RouteDistance first = proxy.distance("Ставрополь", "Москва");
        RouteDistance second = proxy.distance("  ставрополь ", "МОСКВА");

        assertSame(first, second);
        assertEquals(1, service.calls);
        CachingDistanceProxy.Stats stats = proxy.stats();
        assertEquals(2, stats.requests());
        assertEquals(1, stats.cacheHits());
        assertEquals(1, stats.realCalls());
    }

    @Test
    void reverseDirectionUsesSameCacheEntry() {
        proxy.distance("Ставрополь", "Казань");
        proxy.distance("Казань", "Ставрополь");
        assertEquals(1, service.calls);
        assertEquals(1, proxy.stats().cachedRoutes());
    }

    @Test
    void errorsAreNotCached() {
        assertThrows(IllegalArgumentException.class, () -> proxy.distance("Ставрополь", "Атлантида"));
        assertThrows(IllegalArgumentException.class, () -> proxy.distance("Ставрополь", "Атлантида"));
        assertEquals(2, service.calls);
        assertEquals(0, proxy.stats().cachedRoutes());
    }

    @Test
    void clearCacheForcesNewRequest() {
        proxy.distance("Ставрополь", "Москва");
        proxy.clearCache();
        assertEquals(0, proxy.stats().requests());

        proxy.distance("Ставрополь", "Москва");
        assertEquals(2, service.calls);
    }

    @Test
    void directionsTwiceCallGeoServiceOnlyOnce() {
        TripPlanningFacade facade = new TripPlanningFacade(proxy);
        facade.directionsFrom("Ставрополь", TransportType.BUS, 1, true);
        int afterFirst = service.calls;
        facade.directionsFrom("Ставрополь", TransportType.TRAIN, 2, false);

        assertEquals(24, afterFirst);
        assertEquals(24, service.calls);
        assertEquals(24, facade.distanceStats().cacheHits());
    }
}
