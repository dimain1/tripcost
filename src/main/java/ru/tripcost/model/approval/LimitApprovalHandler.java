package ru.tripcost.model.approval;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

/**
 * Звено, которое согласует поездки стоимостью до своего лимита, а более дорогие
 * передаёт дальше. Используется для автосогласования, руководителя отдела и директора.
 */
public class LimitApprovalHandler extends ApprovalHandler {

    /** Лимит в виде «60 000»; строка готовится один раз - объект используется из разных потоков. */
    private final String limitText;

    private final String role;
    private final BigDecimal limit;

    public LimitApprovalHandler(String role, BigDecimal limit) {
        this.role = role;
        this.limit = limit;
        this.limitText = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("ru", "RU")))
                .format(limit);
    }

    @Override
    protected ApprovalDecision handle(ApprovalRequest request, List<String> trace) {
        if (request.total().compareTo(limit) <= 0) {
            return approve(trace, "Стоимость в пределах лимита " + limitText + " ₽");
        }
        return passToNext(request, trace, "превышен лимит " + limitText + " ₽");
    }

    @Override
    public String name() {
        return role + " (до " + limitText + " ₽)";
    }
}
