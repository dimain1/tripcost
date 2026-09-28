package ru.tripcost.model.report;

import java.util.List;

/** HTML-страница, которую можно открыть в браузере или распечатать. */
public class HtmlFormat implements ReportFormat {

    @Override
    public String displayName() {
        return "HTML";
    }

    @Override
    public String extension() {
        return "html";
    }

    @Override
    public String title(String text) {
        return "<h1>" + esc(text) + "</h1>\n";
    }

    @Override
    public String section(String heading, List<ReportLine> lines) {
        StringBuilder sb = new StringBuilder();
        if (heading != null) {
            sb.append("<h2>").append(esc(heading)).append("</h2>\n");
        }
        sb.append("<table>\n");
        for (ReportLine line : lines) {
            String value = line.emphasized() ? "<b>" + esc(line.value()) + "</b>" : esc(line.value());
            sb.append("  <tr><td>").append(esc(line.label())).append("</td><td>")
                    .append(value).append("</td></tr>\n");
        }
        return sb.append("</table>\n").toString();
    }

    @Override
    public String note(String text) {
        return "<p class=\"note\">" + esc(text) + "</p>\n";
    }

    @Override
    public String document(String title, String body) {
        return "<!DOCTYPE html>\n<html lang=\"ru\">\n<head>\n<meta charset=\"UTF-8\">\n"
                + "<title>" + esc(title) + "</title>\n"
                + "<style>body{font-family:sans-serif;max-width:640px;margin:24px auto}"
                + "td{padding:4px 12px 4px 0}td:last-child{text-align:right}.note{color:#666}</style>\n"
                + "</head>\n<body>\n" + body + "</body>\n</html>\n";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
