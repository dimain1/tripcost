package ru.tripcost.model.report;

/**
 * Строка отчёта «название - значение». Выделенные строки (итог) форматы показывают заметнее.
 */
public record ReportLine(String label, String value, boolean emphasized) {

    public static ReportLine of(String label, String value) {
        return new ReportLine(label, value, false);
    }

    public static ReportLine strong(String label, String value) {
        return new ReportLine(label, value, true);
    }
}
