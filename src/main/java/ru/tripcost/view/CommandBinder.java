package ru.tripcost.view;

import javafx.scene.control.ButtonBase;
import ru.tripcost.viewmodel.ViewModelCommand;

/**
 * Привязка кнопок View к командам ViewModel (MVVM).
 */
public final class CommandBinder {

    private CommandBinder() {
    }

    /** Нажатие выполняет команду, кнопка недоступна, пока команду выполнить нельзя. */
    public static void bind(ButtonBase button, ViewModelCommand command) {
        button.setOnAction(e -> command.execute());
        button.disableProperty().bind(command.executableProperty().not());
    }
}
