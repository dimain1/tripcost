package ru.tripcost.model.booking;

import java.util.List;
import java.util.Map;

/**
 * Итог саги.
 *
 * @param success    все шаги выполнены
 * @param log        журнал: выполненные, пропущенные, неудачный и компенсированные шаги
 * @param references номера операций выполненных шагов (при успехе)
 * @param failure    причина неудачи или пустая строка
 */
public record SagaResult(boolean success, List<SagaLogEntry> log, Map<String, String> references,
                         String failure) {

    public SagaResult {
        log = List.copyOf(log);
        references = Map.copyOf(references);
    }

    /** «билеты Б-1, гостиница Г-1, ...» */
    public String summary() {
        return String.join(", ", references.values());
    }
}
