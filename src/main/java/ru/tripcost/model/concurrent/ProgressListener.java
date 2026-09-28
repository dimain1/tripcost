package ru.tripcost.model.concurrent;

/**
 * Уведомление о ходе фоновой работы. Вызывается из рабочего потока.
 */
@FunctionalInterface
public interface ProgressListener {

    void progress(int done, int total);

    ProgressListener NONE = (done, total) -> { };
}
