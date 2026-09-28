package ru.tripcost.viewmodel;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.request.RequestAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripPlannerMediatorTest {

    private final MainViewModel main = new MainViewModel();
    private final TripCalculatorViewModel calculator = new TripCalculatorViewModel();
    private final JourneyViewModel journey = new JourneyViewModel();
    private final DirectionsViewModel directions = new DirectionsViewModel();
    private final RequestsViewModel requests = new RequestsViewModel();

    TripPlannerMediatorTest() {
        main.connect(calculator, journey, directions, requests);
    }

    @Test
    void calculatorSendsTripToJourney() {
        calculator.openTrip("Ставрополь", "Москва", TransportType.TRAIN, 1);
        calculator.addToJourney();

        assertEquals(1, journey.getRows().size());
        assertEquals("Москва", journey.getRows().get(0).to());
        assertEquals(AppTab.JOURNEY, main.selectedTabProperty().get());
        assertTrue(main.statusProperty().get().contains("Расчёт"));
    }

    @Test
    void directionSendsLegToJourney() {
        directions.originProperty().set("Ставрополь");
        directions.transportProperty().set(TransportType.BUS);
        directions.show().join();
        directions.selectedIndexProperty().set(0);
        directions.addSelectedToJourney();

        assertEquals(1, journey.getRows().size());
        assertEquals("Ставрополь", journey.getRows().get(0).from());
        assertEquals(directions.getRows().get(0).city(), journey.getRows().get(0).to());
        assertTrue(main.statusProperty().get().contains("Направления"));
    }

    @Test
    void journeyLegOpensInCalculator() {
        journey.addLegFrom("Ставрополь", "Сочи", TransportType.BUS);
        journey.selectedIndexProperty().set(0);
        journey.openSelectedInCalculator();

        assertEquals(AppTab.CALCULATOR, main.selectedTabProperty().get());
        assertEquals("Ставрополь", calculator.originProperty().get());
        assertEquals("Сочи", calculator.destinationProperty().get());
        assertTrue(calculator.hasResultProperty().get());
    }

    @Test
    void calculatorWithoutResultSendsNothing() {
        calculator.addToJourney();
        assertEquals(0, journey.getRows().size());
        assertFalse(calculator.errorMessageProperty().get().isEmpty());
    }

    @Test
    void journeyChangesAreShownInStatusBar() {
        journey.addLegFrom("Ставрополь", "Москва", TransportType.TRAIN);
        assertTrue(main.statusProperty().get().startsWith("Маршрут: участков 1"));
        journey.undo();
        assertEquals("Маршрут пуст", main.statusProperty().get());
    }

    @Test
    void savedVersionCanBeRestoredAndUndone() {
        journey.addLegFrom("Ставрополь", "Москва", TransportType.TRAIN);
        journey.versionNameProperty().set("Только поезд");
        journey.saveVersion();
        journey.addLegFrom("Москва", "Казань", TransportType.PLANE);

        journey.restoreVersion();
        assertEquals(1, journey.getRows().size());
        journey.undo();
        assertEquals(2, journey.getRows().size());
        assertEquals(1, journey.getVersions().size());
    }

    @Test
    void calculatorCreatesRequestAndRequestsTabObservesIt() {
        calculator.selectedTemplateProperty().set("Командировка в Москву на поезде");
        calculator.applyTemplate();
        calculator.createRequest();

        assertEquals(AppTab.REQUESTS, main.selectedTabProperty().get());
        assertEquals(1, requests.getRows().size());
        assertEquals("Черновик", requests.getRows().get(0).state());
        assertTrue(requests.availableProperty(RequestAction.SUBMIT).get());

        requests.perform(RequestAction.SUBMIT);
        assertEquals("На согласовании", requests.getRows().get(0).state());
        assertEquals(1, requests.getJournal().size());
        assertTrue(main.statusProperty().get().startsWith("Заявка №1: На согласовании"));
        assertFalse(requests.availableProperty(RequestAction.SUBMIT).get());
        assertTrue(requests.availableProperty(RequestAction.APPROVE).get());
    }
}
