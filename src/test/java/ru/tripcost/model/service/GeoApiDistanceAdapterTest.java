package ru.tripcost.model.service;

import org.junit.jupiter.api.Test;
import ru.tripcost.external.geo.WorldGeoApi;
import ru.tripcost.model.domain.TransportType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoApiDistanceAdapterTest {

    private final WorldGeoApi api = new WorldGeoApi();
    private final DistanceService service = new GeoApiDistanceAdapter(api);

    @Test
    void convertsMilesToKilometers() {
        double miles = api.greatCircleMiles(api.findCity("Stavropol"), api.findCity("Moscow"));

        RouteDistance route = service.distance("Ставрополь", "Москва");

        assertEquals(Math.round(miles * 1.609344), route.straightKm());
        assertTrue(route.straightKm() > 1150 && route.straightKm() < 1300, "по прямой около 1230 км");
    }

    @Test
    void roadDistanceIsLongerThanStraight() {
        RouteDistance route = service.distance("Ставрополь", "Кисловодск");

        assertEquals(Math.round(route.straightKm() * 1.3), route.roadKm(), 1.0);
        assertEquals(route.straightKm(), route.forTransport(TransportType.PLANE));
        assertEquals(route.roadKm(), route.forTransport(TransportType.BUS));
    }

    @Test
    void acceptsDifferentSpelling() {
        RouteDistance a = service.distance("Минеральные Воды", "Санкт-Петербург");
        RouteDistance b = service.distance("  минеральные воды ", "г. санкт-петербург");
        assertEquals(a, b);
    }

    @Test
    void unknownCityBecomesException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.distance("Ставрополь", "Атлантида"));
        assertTrue(e.getMessage().contains("Атлантида"));
    }

    @Test
    void everyKnownCityIsFoundInLibrary() {
        for (String city : service.knownCities()) {
            if (!city.equals("Москва")) {
                assertTrue(service.distance(city, "Москва").straightKm() > 0, city);
            }
        }
    }
}
