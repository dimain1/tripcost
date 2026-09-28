package ru.tripcost.model.command;

import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.Leg;

/** Добавить участок в конец маршрута. */
public class AddLegCommand implements Command {

    private final Journey journey;
    private final Leg leg;
    private int index;

    public AddLegCommand(Journey journey, Leg leg) {
        this.journey = journey;
        this.leg = leg;
    }

    @Override
    public void execute() {
        index = journey.size();
        journey.add(leg);
    }

    @Override
    public void undo() {
        journey.remove(index);
    }

    @Override
    public String description() {
        return "Добавлен участок " + leg;
    }
}
