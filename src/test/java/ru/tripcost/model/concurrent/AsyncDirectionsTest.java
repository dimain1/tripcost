package ru.tripcost.model.concurrent;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.RouteQuote;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.service.TripPlanningFacade;
import ru.tripcost.viewmodel.DirectionsViewModel;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncDirectionsTest {

    @Test
    void asyncResultEqualsSequentialResult() {
        List<RouteQuote> expected = new TripPlanningFacade()
                .directionsFrom("Ставрополь", TransportType.TRAIN, 2, true);
        List<RouteQuote> actual = new TripPlanningFacade()
                .directionsFromAsync("Ставрополь", TransportType.TRAIN, 2, true, ProgressListener.NONE)
                .result().join();

        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).to().getName(), actual.get(i).to().getName());
            assertEquals(expected.get(i).total(), actual.get(i).total());
        }
    }

    @Test
    void methodReturnsPromiseImmediatelyAndReportsProgress() {
        AtomicInteger calls = new AtomicInteger();
        AtomicInteger last = new AtomicInteger();
        AsyncJob<RouteQuote, List<RouteQuote>> job = new TripPlanningFacade()
                .directionsFromAsync("Ставрополь", TransportType.BUS, 1, false, (done, total) -> {
                    calls.incrementAndGet();
                    last.accumulateAndGet(done, Math::max);
                });

        assertFalse(job.isDone());                        // геосервис отвечает с задержкой
        List<RouteQuote> quotes = job.result().join();
        assertEquals(24, quotes.size());
        assertEquals(24, job.partCount());
        waitUntil(() -> calls.get() == 24);
        assertEquals(24, last.get());
    }

    @Test
    void poolIsFasterThanSequentialCalculation() {
        long t0 = System.nanoTime();
        new TripPlanningFacade().directionsFrom("Москва", TransportType.TRAIN, 1, false);
        long sequential = System.nanoTime() - t0;

        long t1 = System.nanoTime();
        new TripPlanningFacade().directionsFromAsync("Москва", TransportType.TRAIN, 1, false,
                ProgressListener.NONE).result().join();
        long parallel = System.nanoTime() - t1;

        assertTrue(parallel < sequential * 0.7, "параллельно " + parallel / 1_000_000
                + " мс, последовательно " + sequential / 1_000_000 + " мс");
    }

    @Test
    void cancelledJobCompletesWithCancellation() {
        AsyncJob<RouteQuote, List<RouteQuote>> job = new TripPlanningFacade()
                .directionsFromAsync("Сочи", TransportType.PLANE, 1, false, ProgressListener.NONE);
        job.cancel();
        assertTrue(job.result().isCancelled());
        assertThrows(CancellationException.class, () -> job.result().join());
    }

    @Test
    void sharedCacheIsConsistentAfterParallelRun() {
        TripPlanningFacade facade = new TripPlanningFacade();
        facade.directionsFromAsync("Казань", TransportType.BUS, 1, false, ProgressListener.NONE).result().join();
        assertEquals(24, facade.distanceStats().realCalls());
        assertEquals(24, facade.distanceStats().cachedRoutes());

        facade.directionsFromAsync("Казань", TransportType.TRAIN, 1, false, ProgressListener.NONE).result().join();
        assertEquals(24, facade.distanceStats().cacheHits());      // второй раз - всё из кэша
    }

    @Test
    void failingTaskFailsWholePromise() {
        QuotePool pool = new QuotePool(2);
        List<Supplier<Integer>> tasks = List.of(() -> 1, () -> { throw new IllegalStateException("сбой"); });
        AsyncJob<Integer, Integer> job = new AsyncJob<>(tasks, pool, list -> list.size(), ProgressListener.NONE);
        CompletionException e = assertThrows(CompletionException.class, () -> job.result().join());
        assertTrue(e.getCause() instanceof IllegalStateException);
        pool.shutdown();
    }

    @Test
    void viewModelFillsTableWhenPromiseCompletes() {
        DirectionsViewModel vm = new DirectionsViewModel();
        vm.originProperty().set("Ставрополь");
        vm.transportProperty().set(TransportType.BUS);
        vm.show().join();
        assertEquals(24, vm.getRows().size());
        assertFalse(vm.busyProperty().get());
        assertEquals(1.0, vm.progressProperty().get(), 1e-9);
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + 2000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.onSpinWait();
        }
    }
}
