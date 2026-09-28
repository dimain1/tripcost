package ru.tripcost.model.command;

import ru.tripcost.model.journey.Journey;

/** Переместить участок на позицию выше или ниже. */
public class MoveLegCommand implements Command {

    private final Journey journey;
    private final int index;
    private final int target;

    /** @param delta -1 - выше, +1 - ниже */
    public MoveLegCommand(Journey journey, int index, int delta) {
        this.journey = journey;
        this.index = index;
        this.target = index + delta;
        if (target < 0 || target >= journey.size()) {
            throw new IllegalArgumentException("Участок нельзя переместить дальше края маршрута");
        }
    }

    @Override
    public void execute() {
        journey.swap(index, target);
    }

    @Override
    public void undo() {
        journey.swap(index, target);
    }

    @Override
    public String description() {
        return "Участок №" + (index + 1) + " перемещён " + (target < index ? "выше" : "ниже");
    }
}
