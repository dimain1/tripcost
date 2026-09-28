package ru.tripcost.viewmodel;

import javafx.beans.property.SimpleBooleanProperty;
import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewModelCommandTest {

    @Test
    void commandRunsOnlyWhenExecutable() {
        AtomicInteger runs = new AtomicInteger();
        SimpleBooleanProperty can = new SimpleBooleanProperty(false);
        ViewModelCommand command = new ViewModelCommand(runs::incrementAndGet, can);

        command.execute();
        assertEquals(0, runs.get());
        can.set(true);
        assertTrue(command.isExecutable());
        command.execute();
        assertEquals(1, runs.get());
    }

    @Test
    void journeyCommandsFollowViewModelState() {
        JourneyViewModel vm = new JourneyViewModel();
        assertTrue(vm.addLegCommand().isExecutable());
        assertFalse(vm.undoCommand().isExecutable());
        assertFalse(vm.addReturnCommand().isExecutable());
        assertFalse(vm.removeCommand().isExecutable());

        vm.fromProperty().set("Ставрополь");
        vm.toProperty().set("Москва");
        vm.transportProperty().set(TransportType.TRAIN);
        vm.addLegCommand().execute();

        assertEquals(1, vm.getRows().size());
        assertTrue(vm.undoCommand().isExecutable());
        assertTrue(vm.addReturnCommand().isExecutable());
        assertTrue(vm.saveVersionCommand().isExecutable());
        assertTrue(vm.removeCommand().isExecutable());     // новый участок выбран

        vm.undoCommand().execute();
        assertEquals(0, vm.getRows().size());
        assertTrue(vm.redoCommand().isExecutable());
        assertFalse(vm.restoreVersionCommand().isExecutable());
    }

    @Test
    void directionsAddCommandNeedsSelection() {
        DirectionsViewModel vm = new DirectionsViewModel();
        assertFalse(vm.addToJourneyCommand().isExecutable());
        vm.selectedIndexProperty().set(0);
        assertTrue(vm.addToJourneyCommand().isExecutable());
    }
}
