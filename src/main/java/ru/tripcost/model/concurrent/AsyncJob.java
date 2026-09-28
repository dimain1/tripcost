package ru.tripcost.model.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Фоновое задание из многих независимых частей.
 *
 * <p><b>Паттерн «Future / Promise».</b> Каждая часть запускается в пуле потоков
 * и сразу даёт обещание ({@link CompletableFuture}) своего результата. Обещания
 * объединяются в одно: {@link #result()} выполняется, когда готовы все части.
 * Клиент не ждёт, а подписывается на результат ({@code thenAccept},
 * {@code whenComplete}) и может отменить задание.</p>
 *
 * @param <P> результат одной части
 * @param <R> итоговый результат
 */
public final class AsyncJob<P, R> {

    private final List<CompletableFuture<P>> parts = new ArrayList<>();
    private final CompletableFuture<R> result;

    public AsyncJob(List<Supplier<P>> tasks, QuotePool pool, Function<List<P>, R> combine,
                    ProgressListener listener) {
        AtomicInteger done = new AtomicInteger();
        int total = tasks.size();
        for (Supplier<P> task : tasks) {
            CompletableFuture<P> part = CompletableFuture.supplyAsync(task, pool.executor());
            part.thenRun(() -> listener.progress(done.incrementAndGet(), total));
            parts.add(part);
        }
        result = CompletableFuture.allOf(parts.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> combine.apply(parts.stream().map(CompletableFuture::join).toList()));
    }

    /** Обещание итогового результата. */
    public CompletableFuture<R> result() {
        return result;
    }

    /** Отменить задание: ещё не начатые части не выполняются, результат завершается отменой. */
    public void cancel() {
        result.cancel(false);                 // сначала общий результат - иначе он завершится
        parts.forEach(p -> p.cancel(false));  // исключением от отменённых частей
    }

    public boolean isDone() {
        return result.isDone();
    }

    public int partCount() {
        return parts.size();
    }
}
