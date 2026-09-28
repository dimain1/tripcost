package ru.tripcost.model.command;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.Leg;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHistoryTest {

    private final Journey journey = new Journey();
    private final CommandHistory history = new CommandHistory();
    private final Leg a = new Leg("Ставрополь", "Москва", TransportType.TRAIN);
    private final Leg b = new Leg("Москва", "Казань", TransportType.PLANE);
    private final Leg c = new Leg("Казань", "Самара", TransportType.BUS);

    private void addAll() {
        history.execute(new AddLegCommand(journey, a));
        history.execute(new AddLegCommand(journey, b));
        history.execute(new AddLegCommand(journey, c));
    }

    @Test
    void addAndUndoAll() {
        addAll();
        assertEquals(List.of(a, b, c), journey.toList());
        history.undo();
        history.undo();
        history.undo();
        assertTrue(journey.isEmpty());
        assertFalse(history.canUndo());
        assertThrows(IllegalStateException.class, history::undo);
    }

    @Test
    void removeUndoReturnsLegToSamePlace() {
        addAll();
        history.execute(new RemoveLegCommand(journey, 1));
        assertEquals(List.of(a, c), journey.toList());
        history.undo();
        assertEquals(List.of(a, b, c), journey.toList());
    }

    @Test
    void moveAndUndo() {
        addAll();
        history.execute(new MoveLegCommand(journey, 2, -1));
        assertEquals(List.of(a, c, b), journey.toList());
        history.undo();
        assertEquals(List.of(a, b, c), journey.toList());
        assertThrows(IllegalArgumentException.class, () -> new MoveLegCommand(journey, 0, -1));
    }

    @Test
    void changeTransportAndUndo() {
        addAll();
        history.execute(new ChangeTransportCommand(journey, 0, TransportType.PLANE));
        assertEquals(TransportType.PLANE, journey.get(0).transport());
        history.undo();
        assertEquals(a, journey.get(0));
    }

    @Test
    void returnTripAddsReversedLegsAndUndoRemovesThem() {
        addAll();
        history.execute(new AddReturnTripCommand(journey));
        assertEquals(6, journey.size());
        assertEquals(c.reversed(), journey.get(3));
        assertEquals(a.reversed(), journey.get(5));
        history.undo();
        assertEquals(List.of(a, b, c), journey.toList());
    }

    @Test
    void redoRepeatsUndoneCommand() {
        addAll();
        history.execute(new RemoveLegCommand(journey, 0));
        history.undo();
        assertTrue(history.canRedo());
        history.redo();
        assertEquals(List.of(b, c), journey.toList());
        assertFalse(history.canRedo());
    }

    @Test
    void newCommandClearsRedo() {
        addAll();
        history.undo();
        assertTrue(history.canRedo());
        history.execute(new AddLegCommand(journey, a));
        assertFalse(history.canRedo());
        assertEquals(3, history.executed().size());
    }

    @Test
    void failedCommandIsNotStored() {
        assertThrows(IllegalStateException.class, () -> history.execute(new AddReturnTripCommand(journey)));
        assertFalse(history.canUndo());
    }

    @Test
    void historyListsDescriptions() {
        addAll();
        history.undo();
        assertEquals(2, history.executed().size());
        assertTrue(history.executed().get(0).contains("Ставрополь → Москва"));
        assertEquals(1, history.undone().size());
        assertTrue(history.undoDescription().contains("Москва → Казань"));
    }
}
