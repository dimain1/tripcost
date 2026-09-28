package ru.tripcost.model.request;

/**
 * Действия над заявкой. Какие из них доступны, решает текущее состояние.
 */
public enum RequestAction {
    SUBMIT("Отправить на согласование"),
    APPROVE("Согласовать"),
    REJECT("Отклонить"),
    PAY("Оплатить и забронировать"),
    COMPLETE("Завершить"),
    CANCEL("Отменить"),
    REVISE("Вернуть в черновик");

    private final String title;

    RequestAction(String title) {
        this.title = title;
    }

    public String title() {
        return title;
    }

    @Override
    public String toString() {
        return title;
    }
}
