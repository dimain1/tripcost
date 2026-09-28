package ru.tripcost.model.approval;

import java.util.List;

/**
 * Решение по запросу.
 *
 * @param approved  согласовано или отклонено
 * @param handler   кто принял решение
 * @param reason    пояснение
 * @param trace     путь запроса по цепочке: что сделал каждый обработчик
 */
public record ApprovalDecision(boolean approved, String handler, String reason, List<String> trace) {

    public ApprovalDecision {
        trace = List.copyOf(trace);
    }

    /** Краткая подпись для таблиц и строки результата. */
    public String summary() {
        return (approved ? "Согласует: " : "Отклонено: ") + handler;
    }
}
