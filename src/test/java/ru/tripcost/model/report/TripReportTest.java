package ru.tripcost.model.report;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;
import ru.tripcost.model.pricing.TripCostCalculator;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripReportTest {

    private final Trip trip = Trip.builder().from("Ставрополь").to("Кисловодск").distance(200)
            .by(TransportType.CAR).passengers(2).fuelConsumption(8).roundTrip(true)
            .tolls(new BigDecimal("100"))
            .accommodation(1, new BigDecimal("3000")).build();
    private final CostBreakdown cost = new TripCostCalculator().calculate(trip);

    @Test
    void everyReportWorksWithEveryFormat() {
        for (ReportType type : ReportType.values()) {
            for (ReportFormatType format : ReportFormatType.values()) {
                String text = type.create(format.create(), trip, cost).render();
                String digits = text.replaceAll("[\\s\\u00a0\\u202f]", "");
                assertTrue(digits.contains("5200,00"), type + " / " + format + ": нет итога");
            }
        }
    }

    @Test
    void formatDecidesMarkup() {
        String html = ReportType.SHORT.create(new HtmlFormat(), trip, cost).render();
        String md = ReportType.SHORT.create(new MarkdownFormat(), trip, cost).render();
        String txt = ReportType.SHORT.create(new PlainTextFormat(), trip, cost).render();

        assertTrue(html.startsWith("<!DOCTYPE html>") && html.contains("<h1>Стоимость поездки</h1>"));
        assertTrue(md.startsWith("# Стоимость поездки") && md.contains("| Итого |"));
        assertTrue(txt.startsWith("СТОИМОСТЬ ПОЕЗДКИ"));
    }

    @Test
    void reportDecidesContent() {
        String shortReport = ReportType.SHORT.create(new PlainTextFormat(), trip, cost).render();
        String detailed = ReportType.DETAILED.create(new PlainTextFormat(), trip, cost).render();

        assertFalse(shortReport.contains("Платные дороги"));
        assertTrue(detailed.contains("Платные дороги"));
        assertTrue(detailed.contains("Расход топлива"));
    }

    @Test
    void htmlEscapesUserText() {
        Trip odd = Trip.builder().from("<А>").to("Б & В").distance(10).by(TransportType.BUS).build();
        String html = ReportType.DETAILED.create(new HtmlFormat(), odd,
                new TripCostCalculator().calculate(odd)).render();

        assertTrue(html.contains("&lt;А&gt;") && html.contains("Б &amp; В"));
    }
}
