package ru.tripcost.model.journey;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.command.AddLegCommand;
import ru.tripcost.model.command.CommandHistory;
import ru.tripcost.model.command.RestoreVersionCommand;
import ru.tripcost.model.domain.TransportType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JourneyMementoTest {

    private final Leg a = new Leg("Ставрополь", "Москва", TransportType.TRAIN);
    private final Leg b = new Leg("Москва", "Казань", TransportType.PLANE);
    private final Leg c = new Leg("Казань", "Самара", TransportType.BUS);

    @Test
    void restoreReturnsSavedState() {
        Journey journey = new Journey();
        journey.add(a);
        journey.add(b);
        Journey.Memento memento = journey.save("два участка");

        journey.remove(0);
        journey.add(c);
        journey.add(c);
        journey.restore(memento);

        assertEquals(List.of(a, b), journey.toList());
        assertEquals(2, memento.size());
        assertEquals("два участка", memento.name());
    }

    @Test
    void mementoIsNotAffectedByLaterChanges() {
        Journey journey = new Journey();
        journey.add(a);
        Journey.Memento memento = journey.save("один");
        for (int i = 0; i < 10; i++) {
            journey.add(c);           // внутренний массив растёт и копируется
        }
        journey.set(0, b);
        journey.restore(memento);
        assertEquals(List.of(a), journey.toList());
    }

    @Test
    void restoredJourneyCanBeChangedAgain() {
        Journey journey = new Journey();
        Journey.Memento empty = journey.save("пусто");
        journey.add(a);
        journey.restore(empty);
        assertTrue(journey.isEmpty());
        journey.add(b);
        assertEquals(List.of(b), journey.toList());
    }

    @Test
    void caretakerKeepsVersionsByName() {
        Journey journey = new Journey();
        JourneyVersions versions = new JourneyVersions();
        journey.add(a);
        versions.save(journey, "Поезд");
        journey.add(b);
        versions.save(journey, "Поезд + самолёт");
        versions.save(journey, "поезд");                // замена по названию

        assertEquals(2, versions.list().size());
        assertEquals(2, versions.find("Поезд").orElseThrow().size());
        assertThrows(IllegalArgumentException.class, () -> versions.save(journey, " "));
        assertThrows(IllegalArgumentException.class, () -> versions.save(new Journey(), "пустой"));
    }

    @Test
    void restoreCommandCanBeUndone() {
        Journey journey = new Journey();
        CommandHistory history = new CommandHistory();
        history.execute(new AddLegCommand(journey, a));
        Journey.Memento saved = journey.save("A");
        history.execute(new AddLegCommand(journey, b));
        history.execute(new AddLegCommand(journey, c));

        history.execute(new RestoreVersionCommand(journey, saved));
        assertEquals(List.of(a), journey.toList());
        history.undo();
        assertEquals(List.of(a, b, c), journey.toList());
        history.redo();
        assertEquals(List.of(a), journey.toList());
    }
}
