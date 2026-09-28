package ru.tripcost.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.tripcost.model.booking.SagaResult;
import ru.tripcost.model.booking.TravelServices;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;
import ru.tripcost.model.request.RequestAction;
import ru.tripcost.model.request.RequestListener;
import ru.tripcost.model.request.StateChange;
import ru.tripcost.model.request.TripRequest;
import ru.tripcost.model.service.TripPlanningFacade;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel вкладки «Заявки»: заявки на командировку и их жизненный цикл.
 *
 * <p>Модель представления - один из наблюдателей заявок (паттерн «Наблюдатель»):
 * при каждом переходе заявки она обновляет таблицу, журнал, уведомления и доступность
 * кнопок. Какие действия доступны, определяет состояние заявки (паттерн «Состояние»).</p>
 */
public class RequestsViewModel implements PlannerColleague, RequestListener {

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));

    private final TripPlanningFacade facade = new TripPlanningFacade();
    private final List<TripRequest> requests = new ArrayList<>();
    private PlannerMediator mediator;

    private final ObservableList<RequestRow> rows = FXCollections.observableArrayList();
    private final ObservableList<String> journal = FXCollections.observableArrayList();
    private final ObservableList<String> notifications = FXCollections.observableArrayList();
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(-1);
    private final StringProperty comment = new SimpleStringProperty("");
    private final ReadOnlyStringWrapper errorMessage = new ReadOnlyStringWrapper("");
    private final Map<RequestAction, ReadOnlyBooleanWrapper> available = new EnumMap<>(RequestAction.class);
    /** Команда MVVM на каждое действие; доступность задаёт состояние выбранной заявки. */
    private final Map<RequestAction, ViewModelCommand> commands = new EnumMap<>(RequestAction.class);

    // --- бронирование (сага): имитация сбоев сервисов, остаток бюджета, журнал шагов
    private final BooleanProperty ticketsFailing = new SimpleBooleanProperty(false);
    private final BooleanProperty hotelsFailing = new SimpleBooleanProperty(false);
    private final BooleanProperty insuranceFailing = new SimpleBooleanProperty(false);
    private final ReadOnlyStringWrapper budget = new ReadOnlyStringWrapper("");
    private final ObservableList<String> sagaLog = FXCollections.observableArrayList();

    public RequestsViewModel() {
        for (RequestAction action : RequestAction.values()) {
            available.put(action, new ReadOnlyBooleanWrapper(false));
            commands.put(action, new ViewModelCommand(() -> perform(action), available.get(action)));
        }
        selectedIndex.addListener((obs, o, n) -> {
            updateActions();
            updateSagaLog();
        });
        TravelServices services = facade.travelServices();
        ticketsFailing.addListener((obs, o, n) -> services.tickets().setFailing(n));
        hotelsFailing.addListener((obs, o, n) -> services.hotels().setFailing(n));
        insuranceFailing.addListener((obs, o, n) -> services.insurance().setFailing(n));
        updateBudget();
    }

    /** Вызывается посредником: создать заявку по рассчитанной поездке. */
    public TripRequest createRequest(Trip trip, CostBreakdown cost) {
        TripRequest request = facade.createRequest(trip, cost);
        request.addListener(this);          // ViewModel - ещё один наблюдатель
        requests.add(request);
        refresh();
        selectedIndex.set(requests.size() - 1);
        return request;
    }

    /** Выполнить действие над выбранной заявкой; допустимость проверяет её состояние. */
    public void perform(RequestAction action) {
        TripRequest request = selected();
        if (request == null) {
            errorMessage.set("Выберите заявку в таблице");
            return;
        }
        try {
            request.perform(action, comment.get());
            comment.set("");
            errorMessage.set("");
        } catch (IllegalStateException e) {
            errorMessage.set(e.getMessage());
        }
    }

    /** Наблюдатель: заявка сменила состояние. */
    @Override
    public void onStateChanged(StateChange change) {
        int keep = selectedIndex.get();
        refresh();
        selectedIndex.set(-1);
        selectedIndex.set(keep);
        if (mediator != null) {
            mediator.notify(this, new PlannerEvent.RequestChanged("Заявка №" + change.request().id() + ": "
                    + change.to().name() + (change.comment().isEmpty() ? "" : " - " + change.comment())));
        }
    }

    private void refresh() {
        List<RequestRow> list = new ArrayList<>();
        for (TripRequest r : requests) {
            list.add(new RequestRow(String.valueOf(r.id()),
                    r.trip().getOrigin() + " → " + r.trip().getDestination() + ", " + r.trip().getTransport(),
                    money(r.cost().total()), r.approver(), r.state().name()));
        }
        rows.setAll(list);
        List<String> log = new ArrayList<>(facade.requestJournal().entries().stream()
                .map(StateChange::toString).toList());
        Collections.reverse(log);                    // новые записи сверху
        journal.setAll(log);
        List<String> messages = new ArrayList<>(facade.notifications().messages());
        Collections.reverse(messages);
        notifications.setAll(messages);
        updateActions();
        updateSagaLog();
        updateBudget();
    }

    /** Журнал шагов саги для выбранной заявки. */
    private void updateSagaLog() {
        TripRequest request = selected();
        SagaResult result = request == null ? null : request.lastBooking();
        if (result == null) {
            sagaLog.setAll(request == null ? List.of() : List.of("Бронирование ещё не выполнялось"));
        } else {
            List<String> lines = new ArrayList<>();
            result.log().forEach(e -> lines.add(e.toString()));
            lines.add(result.success() ? "Итог: всё забронировано и оплачено"
                    : "Итог: бронирование отменено, выполненные шаги компенсированы");
            sagaLog.setAll(lines);
        }
    }

    private void updateBudget() {
        budget.set("Остаток бюджета отдела: " + money(facade.travelServices().budget().balance()));
    }

    private void updateActions() {
        TripRequest request = selected();
        for (RequestAction action : RequestAction.values()) {
            available.get(action).set(request != null && request.can(action));
        }
    }

    private TripRequest selected() {
        int i = selectedIndex.get();
        return i >= 0 && i < requests.size() ? requests.get(i) : null;
    }

    @Override
    public void setMediator(PlannerMediator mediator) {
        this.mediator = mediator;
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value) + " ₽";
    }

    public ObservableList<RequestRow> getRows() { return rows; }
    public ObservableList<String> getJournal() { return journal; }
    public ObservableList<String> getNotifications() { return notifications; }
    public IntegerProperty selectedIndexProperty() { return selectedIndex; }
    public StringProperty commentProperty() { return comment; }
    public ReadOnlyStringProperty errorMessageProperty() { return errorMessage.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty availableProperty(RequestAction action) {
        return available.get(action).getReadOnlyProperty();
    }
    public BooleanProperty ticketsFailingProperty() { return ticketsFailing; }
    public BooleanProperty hotelsFailingProperty() { return hotelsFailing; }
    public BooleanProperty insuranceFailingProperty() { return insuranceFailing; }
    public ReadOnlyStringProperty budgetProperty() { return budget.getReadOnlyProperty(); }
    public ObservableList<String> getSagaLog() { return sagaLog; }

    public ViewModelCommand command(RequestAction action) {
        return commands.get(action);
    }

    /** Заявки (для тестов и посредника). */
    public List<TripRequest> getRequests() { return List.copyOf(requests); }

    /** Строка таблицы заявок. */
    public record RequestRow(String id, String route, String total, String approver, String state) {
    }
}
