package ru.tripcost.model.report;

import java.util.List;

/** Обычный текст с выравниванием по колонкам. */
public class PlainTextFormat implements ReportFormat {

    private static final int LABEL_WIDTH = 28;

    @Override
    public String displayName() {
        return "Текст";
    }

    @Override
    public String extension() {
        return "txt";
    }

    @Override
    public String title(String text) {
        return text.toUpperCase() + "\n" + "=".repeat(text.length()) + "\n\n";
    }

    @Override
    public String section(String heading, List<ReportLine> lines) {
        StringBuilder sb = new StringBuilder();
        if (heading != null) {
            sb.append(heading).append("\n").append("-".repeat(heading.length())).append("\n");
        }
        for (ReportLine line : lines) {
            String value = line.emphasized() ? line.value().toUpperCase() : line.value();
            sb.append(String.format("%-" + LABEL_WIDTH + "s %s%n", line.label() + ":", value));
        }
        return sb.append("\n").toString();
    }

    @Override
    public String note(String text) {
        return text + "\n";
    }

    @Override
    public String document(String title, String body) {
        return body;
    }
}
