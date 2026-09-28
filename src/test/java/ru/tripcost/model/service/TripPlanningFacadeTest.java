package ru.tripcost.model.service;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.RouteQuote;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripPreset;
import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.JourneyQuote;
import ru.tripcost.model.journey.Leg;
import ru.tripcost.model.pricing.CostBreakdown;
import ru.tripcost.model.pricing.TripCostCalculator;
import ru.tripcost.model.report.ReportFormatType;
import ru.tripcost.model.report.ReportType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripPlanningFacadeTest {

    private final TripPlanningFacade facade = new TripPlanningFacade();

    @Test
    void quickQuoteCombinesDistanceBuilderAndCalculator() {
        TripPlanningFacade.Quote quote = facade.quickQuote("Ставрополь", "Москва", TransportType.TRAIN, 2, true);

        double km = facade.distance("Ставрополь", "Москва").roadKm();
        assertEquals(km, quote.trip().getDistanceKm());
        CostBreakdown direct = new TripCostCalculator().calculate(quote.trip());
        assertEquals(0, direct.total().compareTo(quote.cost().total()));
    }

    @Test
    void planeUsesStraightDistance() {
        TripPlanningFacade.Quote quote = facade.quickQuote("Ставрополь", "Москва", TransportType.PLANE, 1, false);
        assertEquals(facade.distance("Ставрополь", "Москва").straightKm(), quote.trip().getDistanceKm());
    }

    @Test
    void templatesAndReportsGoThroughFacade() {
        assertTrue(facade.templateNames().contains(TripPreset.CITY_TAXI.getDisplayName()));
        Trip trip = facade.template(TripPreset.CITY_TAXI.getDisplayName()).build();
        CostBreakdown cost = facade.calculate(trip);

        String md = facade.report(ReportType.SHORT, ReportFormatType.MARKDOWN, trip, cost);
        assertTrue(md.startsWith("# Стоимость поездки"));
        assertEquals("md", facade.reportExtension(ReportFormatType.MARKDOWN));
    }

    @Test
    void directionsCoverAllOtherCitiesSortedByPrice() {
        List<RouteQuote> quotes = facade.directionsFrom("Ставрополь", TransportType.BUS, 1, false);

        assertEquals(facade.knownCities().size() - 1, quotes.size());
        for (int i = 1; i < quotes.size(); i++) {
            assertTrue(quotes.get(i - 1).total().compareTo(quotes.get(i).total()) <= 0);
        }
        // во всех строках - один и тот же объект города отправления
        for (RouteQuote q : quotes) {
            assertSame(quotes.get(0).from(), q.from());
        }
    }

    @Test
    void unavailableVariantsAreMarked() {
        List<RouteQuote> quotes = facade.directionsFrom("Ставрополь", TransportType.TAXI, 6, false);
        assertTrue(quotes.stream().noneMatch(RouteQuote::isAvailable));
    }

    @Test
    void unknownCityIsReported() {
        assertThrows(IllegalArgumentException.class,
                () -> facade.quickQuote("Ставрополь", "Атлантида", TransportType.BUS, 1, false));
    }

    @Test
    void journeyQuoteSumsLegsAndGroupsKmByTransport() {
        Journey journey = new Journey();
        journey.add(new Leg("Ставрополь", "Москва", TransportType.TRAIN));
        journey.add(new Leg("Москва", "Казань", TransportType.PLANE));
        journey.add(new Leg("Самара", "Волгоград", TransportType.TRAIN));

        JourneyQuote q = facade.quoteJourney(journey, 2);

        assertEquals(3, q.legs().size());
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
        for (Leg leg : journey) {
            sum = sum.add(facade.quickQuote(leg.from(), leg.to(), leg.transport(), 2, false).cost().total());
        }
        assertEquals(0, sum.compareTo(q.total()));
        assertEquals(2, q.kmByTransport().size());
        assertTrue(q.legs().get(2).note().startsWith("Разрыв"));
        double trainKm = q.legs().get(0).distanceKm() + q.legs().get(2).distanceKm();
        assertEquals(trainKm, q.kmByTransport().get(TransportType.TRAIN).doubleValue());
    }

    @Test
    void journeyQuoteMarksBadLeg() {
        Journey journey = new Journey();
        journey.add(new Leg("Ставрополь", "Атлантида", TransportType.BUS));
        JourneyQuote q = facade.quoteJourney(journey, 1);
        assertTrue(!q.allPriced());
        assertEquals(0, q.total().signum());
    }
}
