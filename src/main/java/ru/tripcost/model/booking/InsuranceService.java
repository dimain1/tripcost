package ru.tripcost.model.booking;

/** Сервис страховой компании. */
public class InsuranceService extends RemoteService {

    public InsuranceService() {
        super("С");
    }

    public String issue(BookingOrder order) {
        return register("полис на " + order.passengers() + " пасс.", "Страховая компания недоступна");
    }

    /** Компенсация: аннулирование полиса. */
    public void annul(String policyId) {
        unregister(policyId);
    }
}
