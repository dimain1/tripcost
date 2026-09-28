package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.service.TripPlanningFacade;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityFactoryTest {

    @Test
    void sameCityIsSharedObject() {
        CityFactory factory = new CityFactory();
        City a = factory.get("Москва");
        City b = factory.get("  москва ");
        City c = factory.get("Сочи");

        assertSame(a, b);
        assertNotSame(a, c);
        assertEquals(3, factory.requestCount());
        assertEquals(2, factory.createdCount());
    }

    @Test
    void intrinsicStateComesFromDirectory() {
        City city = new CityFactory().get("пятигорск");
        assertEquals("Пятигорск", city.getName());
        assertEquals("Ставропольский край", city.getRegion());
        assertEquals("СКФО", city.getFederalDistrict());
    }

    @Test
    void extrinsicStateIsPassedAsParameter() {
        CityFactory factory = new CityFactory();
        String text = factory.get("Ставрополь").routeTo(factory.get("Москва"), 1599.4);
        assertEquals("Ставрополь → Москва, 1599 км (другой федеральный округ)", text);
    }

    @Test
    void manyRoutesShareFewCities() {
        TripPlanningFacade facade = new TripPlanningFacade();
        int routes = 0;
        for (String origin : facade.knownCities()) {
            List<RouteQuote> quotes = facade.directionsFrom(origin, TransportType.BUS, 1, false);
            routes += quotes.size();
        }
        TripPlanningFacade.CityStats stats = facade.cityStats();

        assertEquals(25 * 24, routes);
        assertEquals(25, stats.created());                 // объектов City - по числу городов
        assertTrue(stats.requests() > routes);             // а запросов к фабрике - больше, чем маршрутов
    }
}
