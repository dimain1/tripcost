package ru.tripcost.model.request;

/**
 * Подписчик на изменения заявки.
 *
 * <p><b>Паттерн «Наблюдатель» (Observer), роль Observer.</b> Заявка вызывает
 * {@link #onStateChanged} при каждом переходе и не знает, кто и зачем подписан.</p>
 */
@FunctionalInterface
public interface RequestListener {

    void onStateChanged(StateChange change);
}
