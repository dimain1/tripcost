package ru.tripcost.viewmodel;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableBooleanValue;

import java.util.Objects;

/**
 * Команда модели представления (MVVM Command).
 *
 * <p>В архитектуре MVVM действие пользователя - не метод-обработчик во View, а объект
 * ViewModel: что сделать ({@code execute()}) и можно ли это сделать сейчас
 * ({@link #executableProperty()}). View только привязывает кнопку к команде:
 * нажатие вызывает {@code execute()}, доступность кнопки следует за
 * {@code executable}. Так вся логика доступности действий остаётся в ViewModel
 * и проверяется тестами без интерфейса.</p>
 */
public class ViewModelCommand {

    private final Runnable action;
    private final ReadOnlyBooleanWrapper executable = new ReadOnlyBooleanWrapper(true);

    /** Команда, доступная всегда. */
    public ViewModelCommand(Runnable action) {
        this.action = Objects.requireNonNull(action);
    }

    /** Команда, доступная, пока {@code canExecute} истинно. */
    public ViewModelCommand(Runnable action, ObservableBooleanValue canExecute) {
        this(action);
        executable.bind(canExecute);
    }

    /** Выполнить, если команда сейчас доступна. */
    public void execute() {
        if (executable.get()) {
            action.run();
        }
    }

    public boolean isExecutable() {
        return executable.get();
    }

    public ReadOnlyBooleanProperty executableProperty() {
        return executable.getReadOnlyProperty();
    }
}
