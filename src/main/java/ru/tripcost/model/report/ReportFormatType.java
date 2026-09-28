package ru.tripcost.model.report;

import java.util.function.Supplier;

/** Формат файла отчёта, который выбирает пользователь. */
public enum ReportFormatType {

    TEXT(PlainTextFormat::new),
    HTML(HtmlFormat::new),
    MARKDOWN(MarkdownFormat::new);

    private final Supplier<ReportFormat> factory;

    ReportFormatType(Supplier<ReportFormat> factory) {
        this.factory = factory;
    }

    public ReportFormat create() {
        return factory.get();
    }

    @Override
    public String toString() {
        return create().displayName();
    }
}
