package ru.tripcost.model.booking;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Оркестратор саги бронирования.
 *
 * <p><b>Паттерн «Сага» (Saga), оркестрация.</b> Шаги выполняются по очереди, каждый -
 * локальная операция своего сервиса. Выполненные шаги запоминаются в стеке. Если шаг
 * завершился отказом, оркестратор вызывает компенсации выполненных шагов в обратном
 * порядке, и система возвращается в согласованное состояние: билеты сданы, бронь
 * снята, деньги возвращены.</p>
 */
public class BookingSaga {

    private final List<SagaStep> steps;

    public BookingSaga(List<SagaStep> steps) {
        this.steps = List.copyOf(steps);
    }

    /** Стандартная сага: билеты → гостиница → страховка → списание с бюджета. */
    public static BookingSaga standard(TravelServices s) {
        return new BookingSaga(List.of(
                BookingSteps.buyTickets(s.tickets()),
                BookingSteps.bookHotel(s.hotels()),
                BookingSteps.issueInsurance(s.insurance()),
                BookingSteps.chargeBudget(s.budget())));
    }

    public List<String> stepNames() {
        return steps.stream().map(SagaStep::name).toList();
    }

    public SagaResult run(BookingOrder order) {
        Objects.requireNonNull(order);
        List<SagaLogEntry> log = new ArrayList<>();
        Deque<Done> done = new ArrayDeque<>();
        for (SagaStep step : steps) {
            if (!step.appliesTo(order)) {
                log.add(new SagaLogEntry(SagaLogEntry.Kind.SKIPPED, step.name(), "не требуется"));
                continue;
            }
            try {
                String ref = step.execute(order);
                done.push(new Done(step, ref));
                log.add(new SagaLogEntry(SagaLogEntry.Kind.DONE, step.name(), "№" + ref));
            } catch (ServiceException e) {
                log.add(new SagaLogEntry(SagaLogEntry.Kind.FAILED, step.name(), e.getMessage()));
                compensate(order, done, log);
                return new SagaResult(false, log, Map.of(), step.name() + ": " + e.getMessage());
            }
        }
        Map<String, String> refs = new LinkedHashMap<>();
        List<Done> ordered = new ArrayList<>(done);
        java.util.Collections.reverse(ordered);
        for (Done d : ordered) {
            refs.put(d.step().name(), d.reference());
        }
        return new SagaResult(true, log, refs, "");
    }

    /** Компенсации в порядке, обратном выполнению. */
    private static void compensate(BookingOrder order, Deque<Done> done, List<SagaLogEntry> log) {
        while (!done.isEmpty()) {
            Done d = done.pop();
            d.step().compensate(order, d.reference());
            log.add(new SagaLogEntry(SagaLogEntry.Kind.COMPENSATED, d.step().name(),
                    "отменено №" + d.reference()));
        }
    }

    private record Done(SagaStep step, String reference) {
    }
}
