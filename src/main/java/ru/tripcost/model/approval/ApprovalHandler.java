package ru.tripcost.model.approval;

import java.util.ArrayList;
import java.util.List;

/**
 * Обработчик запроса на согласование.
 *
 * <p><b>Паттерн «Цепочка обязанностей» (Chain of Responsibility), роль Handler.</b>
 * Каждый обработчик знает только следующее звено. Он либо принимает решение сам,
 * либо передаёт запрос дальше методом {@link #passToNext}. Клиент отправляет запрос
 * в первое звено и не знает, кто именно его обработает.</p>
 */
public abstract class ApprovalHandler {

    private ApprovalHandler next;

    /**
     * Назначает следующее звено и возвращает его, чтобы цепочку можно было
     * собрать одной строкой: {@code a.linkWith(b).linkWith(c)}.
     */
    public ApprovalHandler linkWith(ApprovalHandler next) {
        this.next = next;
        return next;
    }

    /** Точка входа для клиента. */
    public final ApprovalDecision handle(ApprovalRequest request) {
        return handle(request, new ArrayList<>());
    }

    /** Обработать запрос или передать его дальше; {@code trace} копит путь запроса. */
    protected abstract ApprovalDecision handle(ApprovalRequest request, List<String> trace);

    /** Название звена для пути запроса. */
    public abstract String name();

    protected ApprovalDecision passToNext(ApprovalRequest request, List<String> trace, String why) {
        trace.add(name() + ": " + why + ", передано дальше");
        if (next == null) {
            return new ApprovalDecision(false, name(), "В цепочке нет звена, которое может принять решение", trace);
        }
        return next.handle(request, trace);
    }

    protected ApprovalDecision approve(List<String> trace, String reason) {
        trace.add(name() + ": согласовано");
        return new ApprovalDecision(true, name(), reason, trace);
    }

    protected ApprovalDecision reject(List<String> trace, String reason) {
        trace.add(name() + ": отклонено");
        return new ApprovalDecision(false, name(), reason, trace);
    }

    /** Звенья цепочки, начиная с этого. */
    public List<String> chainNames() {
        List<String> names = new ArrayList<>();
        for (ApprovalHandler h = this; h != null; h = h.next) {
            names.add(h.name());
        }
        return names;
    }
}
