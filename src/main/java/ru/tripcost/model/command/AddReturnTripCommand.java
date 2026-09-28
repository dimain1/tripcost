package ru.tripcost.model.command;

import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.Leg;
import ru.tripcost.model.journey.LegIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * Добавить обратный путь: участки маршрута в обратном порядке и направлении.
 * Обратные участки перебирает итератор {@link Journey#returnIterator()}.
 */
public class AddReturnTripCommand implements Command {

    private final Journey journey;
    private int added;

    public AddReturnTripCommand(Journey journey) {
        this.journey = journey;
    }

    @Override
    public void execute() {
        if (journey.isEmpty()) {
            throw new IllegalStateException("Маршрут пуст: добавьте хотя бы один участок");
        }
        // сначала собираем участки: изменять маршрут во время обхода нельзя
        List<Leg> back = new ArrayList<>();
        LegIterator it = journey.returnIterator();
        while (it.hasNext()) {
            back.add(it.next());
        }
        back.forEach(journey::add);
        added = back.size();
    }

    @Override
    public void undo() {
        for (int i = 0; i < added; i++) {
            journey.remove(journey.size() - 1);
        }
    }

    @Override
    public String description() {
        return "Добавлен обратный путь (" + added + " уч.)";
    }
}
