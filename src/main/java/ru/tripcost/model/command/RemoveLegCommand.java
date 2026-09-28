package ru.tripcost.model.command;

import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.Leg;

/** Удалить участок; при отмене участок возвращается на прежнее место. */
public class RemoveLegCommand implements Command {

    private final Journey journey;
    private final int index;
    private Leg removed;

    public RemoveLegCommand(Journey journey, int index) {
        this.journey = journey;
        this.index = index;
    }

    @Override
    public void execute() {
        removed = journey.remove(index);
    }

    @Override
    public void undo() {
        journey.insert(index, removed);
    }

    @Override
    public String description() {
        return "Удалён участок " + (removed != null ? removed : "№" + (index + 1));
    }
}
