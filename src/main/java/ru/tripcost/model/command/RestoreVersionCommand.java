package ru.tripcost.model.command;

import ru.tripcost.model.journey.Journey;

/**
 * Восстановить сохранённый вариант маршрута. Перед восстановлением команда делает
 * снимок текущего состояния, поэтому восстановление можно отменить.
 */
public class RestoreVersionCommand implements Command {

    private final Journey journey;
    private final Journey.Memento target;
    private Journey.Memento backup;

    public RestoreVersionCommand(Journey journey, Journey.Memento target) {
        this.journey = journey;
        this.target = target;
    }

    @Override
    public void execute() {
        backup = journey.save("до восстановления");
        journey.restore(target);
    }

    @Override
    public void undo() {
        journey.restore(backup);
    }

    @Override
    public String description() {
        return "Восстановлен вариант «" + target.name() + "»";
    }
}
