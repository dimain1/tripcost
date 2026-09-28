package ru.tripcost.model.request;

import java.util.ArrayList;
import java.util.List;

/**
 * Наблюдатель: уведомления участникам процесса. По новому состоянию заявки решает,
 * кому и что сообщить (сотруднику, согласующему, бухгалтерии).
 */
public class NotificationCenter implements RequestListener {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void onStateChanged(StateChange change) {
        TripRequest r = change.request();
        String trip = "№" + r.id() + " (" + r.trip().getOrigin() + " → " + r.trip().getDestination() + ")";
        RequestState to = change.to();
        if (to == RequestStates.PENDING) {
            add(r.approver().replaceAll(" \\(.*\\)", ""), "согласуйте заявку " + trip);
        } else if (to == RequestStates.APPROVED && change.from() == RequestStates.APPROVED) {
            add("Сотруднику", "бронирование по заявке " + trip + " не выполнено, все шаги отменены: "
                    + change.comment());
        } else if (to == RequestStates.APPROVED) {
            add("Сотруднику", "заявка " + trip + " согласована");
            add("Бухгалтерии", "оплатите заявку " + trip);
        } else if (to == RequestStates.REJECTED) {
            add("Сотруднику", "заявка " + trip + " отклонена: " + change.comment());
        } else if (to == RequestStates.PAID) {
            add("Сотруднику", "билеты и проживание по заявке " + trip + " оплачены");
        } else if (to == RequestStates.CANCELLED && change.from() != RequestStates.DRAFT) {
            add("Бухгалтерии", "заявка " + trip + " отменена");
        }
    }

    private void add(String who, String text) {
        messages.add(who + ": " + text);
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }
}
