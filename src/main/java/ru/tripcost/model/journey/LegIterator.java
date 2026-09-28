package ru.tripcost.model.journey;

import java.util.Iterator;

/**
 * Итератор участков маршрута.
 *
 * <p><b>Паттерн «Итератор» (Iterator).</b> Кроме стандартных {@code hasNext()}
 * и {@code next()} сообщает номер текущего участка в маршруте, чтобы клиент мог
 * подписать строку, не зная, как хранятся участки.</p>
 */
public interface LegIterator extends Iterator<Leg> {

    /** Номер (с 1) участка, который вернул последний вызов {@code next()}. */
    int number();
}
