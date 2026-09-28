package ru.tripcost.model.command;

import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.Leg;

/** Сменить вид транспорта на участке; старый участок запоминается для отмены. */
public class ChangeTransportCommand implements Command {

    private final Journey journey;
    private final int index;
    private final TransportType transport;
    private Leg previous;

    public ChangeTransportCommand(Journey journey, int index, TransportType transport) {
        this.journey = journey;
        this.index = index;
        this.transport = transport;
    }

    @Override
    public void execute() {
        previous = journey.set(index, journey.get(index).withTransport(transport));
    }

    @Override
    public void undo() {
        journey.set(index, previous);
    }

    @Override
    public String description() {
        return "Участок №" + (index + 1) + ": транспорт "
                + (previous != null ? previous.transport() + " → " : "") + transport;
    }
}
