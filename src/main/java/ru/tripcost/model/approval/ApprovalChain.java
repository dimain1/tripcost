package ru.tripcost.model.approval;

import ru.tripcost.model.config.AppConfig;

/**
 * Сборка стандартной цепочки согласования по настройкам из app.properties.
 */
public final class ApprovalChain {

    private ApprovalChain() {
    }

    public static ApprovalHandler standard() {
        AppConfig c = AppConfig.getInstance();
        ApprovalHandler head = new TravelPolicyHandler(
                c.getPolicyTaxiMaxKm(), c.getPolicyPlaneMinKm(), c.getPolicyHotelMaxPerNight());
        head.linkWith(new LimitApprovalHandler("Автосогласование", c.getAutoApprovalLimit()))
                .linkWith(new LimitApprovalHandler("Руководитель отдела", c.getManagerApprovalLimit()))
                .linkWith(new LimitApprovalHandler("Директор", c.getDirectorApprovalLimit()))
                .linkWith(new FinanceCommitteeHandler());
        return head;
    }
}
