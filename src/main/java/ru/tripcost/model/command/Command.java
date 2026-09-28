package ru.tripcost.model.command;

/**
 * Команда редактирования маршрута.
 *
 * <p><b>Паттерн «Команда» (Command).</b> Действие пользователя оформлено объектом:
 * его можно выполнить, отменить и сохранить в истории.</p>
 */
public interface Command {

    void execute();

    /** Возвращает маршрут в состояние до {@link #execute()}. */
    void undo();

    /** Описание для истории команд. */
    String description();
}
