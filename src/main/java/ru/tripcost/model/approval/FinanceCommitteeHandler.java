package ru.tripcost.model.approval;

import java.util.List;

/**
 * Последнее звено: финансовый комитет рассматривает поездки любой стоимости.
 */
public class FinanceCommitteeHandler extends ApprovalHandler {

    @Override
    protected ApprovalDecision handle(ApprovalRequest request, List<String> trace) {
        return approve(trace, "Поездка дороже лимита директора, решение принимает финансовый комитет");
    }

    @Override
    public String name() {
        return "Финансовый комитет";
    }
}
