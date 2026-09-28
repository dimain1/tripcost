package ru.tripcost.model.domain;

/**
 * Прототип: объект, который умеет создавать свою копию.
 *
 * @param <T> тип копии
 */
public interface Prototype<T> {

    /** Создаёт независимую копию объекта. */
    T copy();
}
