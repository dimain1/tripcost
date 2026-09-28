package ru.tripcost.viewmodel;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.tripcost.model.command.AddLegCommand;
import ru.tripcost.model.command.AddReturnTripCommand;
import ru.tripcost.model.command.ChangeTransportCommand;
import ru.tripcost.model.command.Command;
import ru.tripcost.model.command.CommandHistory;
import ru.tripcost.model.command.MoveLegCommand;
import ru.tripcost.model.command.RemoveLegCommand;
import ru.tripcost.model.command.RestoreVersionCommand;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.JourneyQuote;
import ru.tripcost.model.journey.JourneyVersions;
import ru.tripcost.model.journey.Leg;
import ru.tripcost.model.journey.LegQuote;
import ru.tripcost.model.service.TripPlanningFacade;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel вкладки «Маршрут»: составной маршрут из нескольких участков.
 *
 * <p>Каждое изменение маршрута - команда (паттерн «Команда»), которую выполняет
 * {@link CommandHistory}; поэтому любое действие можно отменить и повторить.
 * Таблица участков и сводка строятся обходом маршрута итераторами
 * (паттерн «Итератор»).</p>
 */
public class JourneyViewModel implements PlannerColleague {

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));

    private final TripPlanningFacade facade = new TripPlanningFacade();
    private final Journey journey = new Journey();
    private final CommandHistory history = new CommandHistory();
    /** Хранитель сохранённых вариантов маршрута (паттерн «Снимок»). */
    private final JourneyVersions versions = new JourneyVersions();
    private PlannerMediator mediator;

    private final ObservableList<String> cities = FXCollections.observableArrayList(facade.knownCities());
    private final ObservableList<TransportType> transportTypes =
            FXCollections.observableArrayList(TransportType.values());

    private final StringProperty from = new SimpleStringProperty("Ставрополь");
    private final StringProperty to = new SimpleStringProperty("Москва");
    private final ObjectProperty<TransportType> transport = new SimpleObjectProperty<>(TransportType.TRAIN);
    private final ObjectProperty<Integer> passengers = new SimpleObjectProperty<>(1);
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(-1);
    private final StringProperty versionName = new SimpleStringProperty("Вариант 1");
    private final ObservableList<Journey.Memento> versionList = FXCollections.observableArrayList();
    private final ObjectProperty<Journey.Memento> selectedVersion = new SimpleObjectProperty<>();

    private final ObservableList<LegRow> rows = FXCollections.observableArrayList();
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper total = new ReadOnlyStringWrapper("—");
    private final ReadOnlyStringWrapper perPassenger = new ReadOnlyStringWrapper("—");
    private final ReadOnlyStringWrapper summary = new ReadOnlyStringWrapper("Маршрут пуст");
    private final ReadOnlyStringWrapper errorMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper undoText = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper redoText = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper canUndo = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper canRedo = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper hasSelection = new ReadOnlyBooleanWrapper(false);

    // --- команды MVVM: View привязывает к ним кнопки
    private final ViewModelCommand addLegCommand;
    private final ViewModelCommand removeCommand;
    private final ViewModelCommand moveUpCommand;
    private final ViewModelCommand moveDownCommand;
    private final ViewModelCommand changeTransportCommand;
    private final ViewModelCommand addReturnCommand;
    private final ViewModelCommand undoCommand;
    private final ViewModelCommand redoCommand;
    private final ViewModelCommand openInCalculatorCommand;
    private final ViewModelCommand saveVersionCommand;
    private final ViewModelCommand restoreVersionCommand;

    public JourneyViewModel() {
        selectedIndex.addListener((obs, o, n) -> hasSelection.set(n.intValue() >= 0));
        var notEmpty = Bindings.isNotEmpty(rows);
        addLegCommand = new ViewModelCommand(this::addLeg);
        removeCommand = new ViewModelCommand(this::removeSelected, hasSelection);
        moveUpCommand = new ViewModelCommand(this::moveUp, hasSelection);
        moveDownCommand = new ViewModelCommand(this::moveDown, hasSelection);
        changeTransportCommand = new ViewModelCommand(this::changeTransport, hasSelection);
        addReturnCommand = new ViewModelCommand(this::addReturnTrip, notEmpty);
        undoCommand = new ViewModelCommand(this::undo, canUndo);
        redoCommand = new ViewModelCommand(this::redo, canRedo);
        openInCalculatorCommand = new ViewModelCommand(this::openSelectedInCalculator, hasSelection);
        saveVersionCommand = new ViewModelCommand(this::saveVersion, notEmpty);
        restoreVersionCommand = new ViewModelCommand(this::restoreVersion, selectedVersion.isNotNull());
    }

    // ------------------------------------------------------------ действия (каждое - команда)

    public void addLeg() {
        if (isBlank(from.get()) || isBlank(to.get()) || transport.get() == null) {
            errorMessage.set("Укажите пункты отправления и назначения и вид транспорта");
            return;
        }
        String arrival = to.get().trim();
        if (run(new AddLegCommand(journey, new Leg(from.get(), arrival, transport.get())), journey.size())) {
            from.set(arrival);   // следующий участок начинается там, где закончился этот
            to.set("");
        }
    }

    public void removeSelected() {
        int i = selectedIndex.get();
        if (i >= 0) {
            run(new RemoveLegCommand(journey, i), Math.min(i, journey.size() - 2));
        }
    }

    public void moveUp() {
        move(-1);
    }

    public void moveDown() {
        move(1);
    }

    public void changeTransport() {
        int i = selectedIndex.get();
        if (i >= 0 && transport.get() != null && journey.get(i).transport() != transport.get()) {
            run(new ChangeTransportCommand(journey, i, transport.get()), i);
        }
    }

    public void addReturnTrip() {
        run(new AddReturnTripCommand(journey), -1);
    }

    /** Вызывается посредником: добавить участок, пришедший с другой вкладки. */
    public boolean addLegFrom(String legFrom, String legTo, TransportType type) {
        if (run(new AddLegCommand(journey, new Leg(legFrom, legTo, type)), journey.size())) {
            from.set(legTo);
            to.set("");
            return true;
        }
        return false;
    }

    /** Открыть выбранный участок на вкладке «Расчёт» - через посредника. */
    public void openSelectedInCalculator() {
        int i = selectedIndex.get();
        if (i < 0) {
            errorMessage.set("Выберите участок в таблице");
            return;
        }
        Leg leg = journey.get(i);
        if (mediator != null) {
            mediator.notify(this, new PlannerEvent.OpenInCalculatorRequested(leg.from(), leg.to(),
                    leg.transport(), passengers.get() == null ? 1 : passengers.get()));
        }
    }

    // ------------------------------------------------------------ варианты (снимки)

    /** Сохраняет снимок текущего маршрута под введённым названием. */
    public void saveVersion() {
        try {
            Journey.Memento memento = versions.save(journey, versionName.get());
            versionList.setAll(versions.list());
            selectedVersion.set(memento);
            versionName.set("Вариант " + (versionList.size() + 1));
            errorMessage.set("");
        } catch (IllegalArgumentException e) {
            errorMessage.set(e.getMessage());
        }
    }

    /** Восстанавливает выбранный вариант; восстановление - команда, его можно отменить. */
    public void restoreVersion() {
        if (selectedVersion.get() == null) {
            errorMessage.set("Выберите сохранённый вариант");
            return;
        }
        run(new RestoreVersionCommand(journey, selectedVersion.get()), -1);
    }

    @Override
    public void setMediator(PlannerMediator mediator) {
        this.mediator = mediator;
    }

    public void undo() {
        if (history.canUndo()) {
            history.undo();
            errorMessage.set("");
            refresh(-1);
        }
    }

    public void redo() {
        if (history.canRedo()) {
            history.redo();
            errorMessage.set("");
            refresh(-1);
        }
    }

    private void move(int delta) {
        int i = selectedIndex.get();
        if (i >= 0) {
            run(new MoveLegCommand(journey, i, delta), i + delta);
        }
    }

    /** Выполняет команду через историю и обновляет экран. */
    private boolean run(Command command, int selectAfter) {
        try {
            history.execute(command);
            errorMessage.set("");
            refresh(selectAfter);
            return true;
        } catch (IllegalArgumentException | IllegalStateException e) {
            errorMessage.set(e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------ обновление экрана

    private void refresh(int selectAfter) {
        int pax = passengers.get() == null ? 1 : passengers.get();
        JourneyQuote quote = facade.quoteJourney(journey, pax);
        List<LegRow> newRows = new ArrayList<>();
        for (LegQuote q : quote.legs()) {
            newRows.add(new LegRow(String.valueOf(q.number()), q.leg().from(), q.leg().to(),
                    q.leg().transport().toString(),
                    q.isPriced() ? String.valueOf(Math.round(q.distanceKm())) : "—",
                    q.isPriced() ? money(q.total()) : "—", q.note()));
        }
        rows.setAll(newRows);
        selectedIndex.set(selectAfter < journey.size() ? selectAfter : -1);

        if (journey.isEmpty()) {
            total.set("—");
            perPassenger.set("—");
            summary.set("Маршрут пуст");
        } else {
            total.set(money(quote.total()) + (quote.allPriced() ? "" : " (без участков с ошибками)"));
            perPassenger.set(money(quote.perPassenger()));
            StringBuilder sb = new StringBuilder("Участков: " + journey.size() + ". По видам транспорта: ");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<TransportType, Double> e : quote.kmByTransport().entrySet()) {
                parts.add(e.getKey() + " - " + Math.round(e.getValue()) + " км");
            }
            summary.set(sb.append(String.join(", ", parts)).append('.').toString());
        }

        List<String> items = new ArrayList<>();
        List<String> executed = history.executed();
        for (int i = 0; i < executed.size(); i++) {
            items.add((i + 1) + ". " + executed.get(i));
        }
        for (String undone : history.undone()) {
            items.add("↶ отменено: " + undone);
        }
        historyItems.setAll(items);
        canUndo.set(history.canUndo());
        canRedo.set(history.canRedo());
        undoText.set(history.undoDescription());
        redoText.set(history.redoDescription());
        if (mediator != null) {
            mediator.notify(this, new PlannerEvent.JourneyChanged(journey.size(), total.get()));
        }
    }

    /** Пересчитать после изменения числа пассажиров (не команда: маршрут не меняется). */
    public void recalculate() {
        refresh(selectedIndex.get());
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value) + " ₽";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public ObservableList<String> getCities() { return cities; }
    public ObservableList<TransportType> getTransportTypes() { return transportTypes; }
    public StringProperty fromProperty() { return from; }
    public StringProperty toProperty() { return to; }
    public ObjectProperty<TransportType> transportProperty() { return transport; }
    public ObjectProperty<Integer> passengersProperty() { return passengers; }
    public IntegerProperty selectedIndexProperty() { return selectedIndex; }
    public ObservableList<LegRow> getRows() { return rows; }
    public StringProperty versionNameProperty() { return versionName; }
    public ObservableList<Journey.Memento> getVersions() { return versionList; }
    public ObjectProperty<Journey.Memento> selectedVersionProperty() { return selectedVersion; }
    public ObservableList<String> getHistoryItems() { return historyItems; }
    public ReadOnlyStringProperty totalProperty() { return total.getReadOnlyProperty(); }
    public ReadOnlyStringProperty perPassengerProperty() { return perPassenger.getReadOnlyProperty(); }
    public ReadOnlyStringProperty summaryProperty() { return summary.getReadOnlyProperty(); }
    public ReadOnlyStringProperty errorMessageProperty() { return errorMessage.getReadOnlyProperty(); }
    public ReadOnlyStringProperty undoTextProperty() { return undoText.getReadOnlyProperty(); }
    public ReadOnlyStringProperty redoTextProperty() { return redoText.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty canUndoProperty() { return canUndo.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty canRedoProperty() { return canRedo.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty hasSelectionProperty() { return hasSelection.getReadOnlyProperty(); }

    public ViewModelCommand addLegCommand() { return addLegCommand; }
    public ViewModelCommand removeCommand() { return removeCommand; }
    public ViewModelCommand moveUpCommand() { return moveUpCommand; }
    public ViewModelCommand moveDownCommand() { return moveDownCommand; }
    public ViewModelCommand changeTransportCommand() { return changeTransportCommand; }
    public ViewModelCommand addReturnCommand() { return addReturnCommand; }
    public ViewModelCommand undoCommand() { return undoCommand; }
    public ViewModelCommand redoCommand() { return redoCommand; }
    public ViewModelCommand openInCalculatorCommand() { return openInCalculatorCommand; }
    public ViewModelCommand saveVersionCommand() { return saveVersionCommand; }
    public ViewModelCommand restoreVersionCommand() { return restoreVersionCommand; }

    /** Строка таблицы участков. */
    public record LegRow(String number, String from, String to, String transport,
                         String km, String cost, String note) {
    }
}
