package ru.tripcost.model.config;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    void getInstanceAlwaysReturnsSameObject() {
        assertSame(AppConfig.getInstance(), AppConfig.getInstance());
    }

    @Test
    void sameInstanceFromManyThreads() throws InterruptedException {
        Set<AppConfig> instances = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> instances.add(AppConfig.getInstance()));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, instances.size());
    }

    @Test
    void constructorIsPrivate() {
        for (Constructor<?> c : AppConfig.class.getDeclaredConstructors()) {
            assertTrue(Modifier.isPrivate(c.getModifiers()));
        }
    }

    @Test
    void valuesAreLoadedFromProperties() {
        AppConfig config = AppConfig.getInstance();
        assertEquals(0, new BigDecimal("62.50").compareTo(config.getFuelPrice()));
        assertEquals(8.0, config.getDefaultFuelConsumption());
        assertEquals(0, new BigDecimal("28").compareTo(config.getTariff(TransportType.TAXI).perKm()));
    }

    @Test
    void carHasNoTariff() {
        assertThrows(IllegalArgumentException.class, () -> AppConfig.getInstance().getTariff(TransportType.CAR));
    }
}
