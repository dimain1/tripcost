package ru.tripcost.model.report;

import java.util.List;

/**
 * Формат отчёта - реализация (Implementor) паттерна «Мост».
 *
 * <p>Умеет оформлять простые элементы: заголовок, раздел со строками,
 * примечание и документ целиком. Что именно попадёт в отчёт, решает
 * абстракция {@link TripReport}.</p>
 */
public interface ReportFormat {

    String displayName();

    /** Расширение файла без точки. */
    String extension();

    String title(String text);

    /** Раздел: заголовок (может быть {@code null}) и строки «название - значение». */
    String section(String heading, List<ReportLine> lines);

    String note(String text);

    /** Собирает готовый документ из заголовка и тела. */
    String document(String title, String body);
}
