package ru.tripcost.model.report;

import java.util.List;

/** Markdown: удобно вставить в заметки, мессенджер или README. */
public class MarkdownFormat implements ReportFormat {

    @Override
    public String displayName() {
        return "Markdown";
    }

    @Override
    public String extension() {
        return "md";
    }

    @Override
    public String title(String text) {
        return "# " + text + "\n\n";
    }

    @Override
    public String section(String heading, List<ReportLine> lines) {
        StringBuilder sb = new StringBuilder();
        if (heading != null) {
            sb.append("## ").append(heading).append("\n\n");
        }
        sb.append("| Параметр | Значение |\n|---|---:|\n");
        for (ReportLine line : lines) {
            String value = line.emphasized() ? "**" + cell(line.value()) + "**" : cell(line.value());
            sb.append("| ").append(cell(line.label())).append(" | ").append(value).append(" |\n");
        }
        return sb.append("\n").toString();
    }

    @Override
    public String note(String text) {
        return "_" + text + "_\n";
    }

    @Override
    public String document(String title, String body) {
        return body;
    }

    private static String cell(String s) {
        return s.replace("|", "\\|");
    }
}
