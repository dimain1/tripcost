package ru.tripcost.model.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Пул потоков для расчётов: фиксированное число рабочих потоков, которые берут
 * задачи из общей очереди. Потоки фоновые (daemon) и не мешают закрыть программу.
 */
public final class QuotePool {

    private final ExecutorService executor;
    private final int size;

    public QuotePool(int size) {
        this.size = Math.max(1, size);
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "quote-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(this.size, factory);
    }

    public ExecutorService executor() {
        return executor;
    }

    public int size() {
        return size;
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
