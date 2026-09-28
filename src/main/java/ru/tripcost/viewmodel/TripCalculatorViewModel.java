package ru.tripcost.viewmodel;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.tripcost.model.approval.ApprovalDecision;
import ru.tripcost.model.domain.CostItem;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripBuilder;
import ru.tripcost.model.domain.TripOption;
import ru.tripcost.model.domain.TripValidationException;
import ru.tripcost.model.pricing.ComparisonRow;
import ru.tripcost.model.pricing.TransportChoiceStrategy;
import ru.tripcost.model.transport.TravelTime;
import ru.tripcost.model.pricing.CostBreakdown;
import ru.tripcost.model.report.ReportFormatType;
import ru.tripcost.model.report.ReportType;
import ru.tripcost.model.service.RouteDistance;
import ru.tripcost.model.service.TripPlanningFacade;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel экрана расчёта поездки.
 *
 * <p>Поля формы хранятся в JavaFX-свойствах, к которым View привязывается
 * двусторонне. По команде {@link #calculate()} ViewModel собирает поездку
 * через {@link TripBuilder} (паттерн «Строитель») и передаёт её в расчёт.</p>
 *
 * <p>Все обращения к модели идут через фасад {@link TripPlanningFacade}
 * (паттерн «Фасад»): шаблоны-прототипы, расстояния через адаптер геобиблиотеки,
 * расчёт с фабриками и декораторами транспорта, сравнение и отчёты. Модель
 * представления не знает, из каких подсистем состоит модель.</p>
 */
public class TripCalculatorViewModel implements PlannerColleague {

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));

    // --- поля ввода ---
    private final StringProperty origin = new SimpleStringProperty("");
    private final StringProperty destination = new SimpleStringProperty("");
    private final StringProperty distance = new SimpleStringProperty("");
    private final ObjectProperty<TransportType> transport = new SimpleObjectProperty<>(TransportType.CAR);
    private final ObjectProperty<Integer> passengers = new SimpleObjectProperty<>(1);
    private final StringProperty fuelConsumption = new SimpleStringProperty("");
    private final BooleanProperty roundTrip = new SimpleBooleanProperty(false);
    private final StringProperty tolls = new SimpleStringProperty("");
    private final StringProperty nights = new SimpleStringProperty("");
    private final StringProperty pricePerNight = new SimpleStringProperty("");
    private final StringProperty selectedTemplate = new SimpleStringProperty();
    private final StringProperty newTemplateName = new SimpleStringProperty("");
    private final ObjectProperty<ReportType> reportType = new SimpleObjectProperty<>(ReportType.DETAILED);
    private final ObjectProperty<ReportFormatType> reportFormat = new SimpleObjectProperty<>(ReportFormatType.TEXT);

    // --- доступность полей зависит от вида транспорта ---
    private final BooleanBinding fuelEditable = transport.isEqualTo(TransportType.CAR);
    private final BooleanBinding tollsEditable = Bindings.createBooleanBinding(
            () -> transport.get() != null && transport.get().allowsTolls(), transport);

    // --- дополнительные услуги (декораторы транспорта) ---
    private final Map<TripOption, BooleanProperty> options = new EnumMap<>(TripOption.class);
    private final Map<TripOption, BooleanBinding> optionAvailable = new EnumMap<>(TripOption.class);

    {
        for (TripOption option : TripOption.values()) {
            options.put(option, new SimpleBooleanProperty(false));
            optionAvailable.put(option, Bindings.createBooleanBinding(
                    () -> option.appliesTo(transport.get()), transport));
        }
    }

    // --- результат ---
    private final ReadOnlyStringWrapper errorMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper infoMessage = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper tripSummary = new ReadOnlyStringWrapper("");
    /** Дерево статей расходов для таблицы-дерева во View. */
    private final ReadOnlyObjectWrapper<CostRow> costTree = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyStringWrapper totalCost = new ReadOnlyStringWrapper("—");
    private final ReadOnlyStringWrapper perPassengerCost = new ReadOnlyStringWrapper("—");
    private final ReadOnlyBooleanWrapper hasResult = new ReadOnlyBooleanWrapper(false);
    /** Результат цепочки согласования: кто согласует и путь запроса по цепочке. */
    private final ReadOnlyStringWrapper approval = new ReadOnlyStringWrapper("—");
    private final ReadOnlyStringWrapper approvalDetails = new ReadOnlyStringWrapper("");
    private final ReadOnlyBooleanWrapper approvalRejected = new ReadOnlyBooleanWrapper(false);

    private final ObservableList<TransportType> transportTypes =
            FXCollections.observableArrayList(TransportType.values());
    /** Единственная точка входа в модель. */
    private final TripPlanningFacade facade = new TripPlanningFacade();
    private final ObservableList<String> templateNames =
            FXCollections.observableArrayList(facade.templateNames());
    private final ObservableList<VariantRow> variants = FXCollections.observableArrayList();
    /** Критерии выбора лучшего транспорта (паттерн «Стратегия»). */
    private final ObservableList<TransportChoiceStrategy> choiceStrategies =
            FXCollections.observableArrayList(facade.choiceStrategies());
    private final ObjectProperty<TransportChoiceStrategy> choiceStrategy =
            new SimpleObjectProperty<>(choiceStrategies.get(0));
    private final ReadOnlyStringWrapper travelTime = new ReadOnlyStringWrapper("—");
    private final ReadOnlyStringWrapper travelTimeDetails = new ReadOnlyStringWrapper("");
    /** Выбран ли шаблон пользователя (его можно удалить); где хранятся шаблоны. */
    private final ReadOnlyBooleanWrapper userTemplateSelected = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper templatesHint = new ReadOnlyStringWrapper("");
    private ViewModelCommand deleteTemplateCommand;

    {
        selectedTemplate.addListener((obs, o, n) -> userTemplateSelected.set(facade.isUserTemplate(n)));
        templatesHint.set("Свои шаблоны сохраняются в файл: " + facade.templatesLocation());
        deleteTemplateCommand = new ViewModelCommand(this::deleteTemplate, userTemplateSelected);
        // смена стратегии сразу пересортировывает уже показанное сравнение
        choiceStrategy.addListener((obs, o, n) -> {
            if (n != null && !variants.isEmpty()) {
                compareTransport();
            }
        });
    }
    private final ObservableList<ReportType> reportTypes = FXCollections.observableArrayList(ReportType.values());
    private final ObservableList<ReportFormatType> reportFormats =
            FXCollections.observableArrayList(ReportFormatType.values());

    /** Посредник между вкладками (паттерн «Посредник»). */
    private PlannerMediator mediator;

    /** Последний успешный расчёт - по нему строится отчёт. */
    private Trip lastTrip;
    private CostBreakdown lastCost;


    // ------------------------------------------------------------ команды

    /** Собирает поездку из полей формы и считает её стоимость. */
    public void calculate() {
        try {
            Trip trip = builderFromForm().build();
            showResult(trip, facade.calculate(trip));
        } catch (TripValidationException e) {
            showError(String.join("\n", e.getErrors()));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /** Заполняет форму копией выбранного шаблона и сразу считает стоимость. */
    public void applyTemplate() {
        String name = selectedTemplate.get();
        if (name == null) {
            showError("Выберите шаблон поездки");
            return;
        }
        Trip trip = facade.template(name).build();   // копия прототипа
        fillForm(trip);
        showResult(trip, facade.calculate(trip));
    }

    /** Удаляет выбранный шаблон пользователя из реестра и из файла. */
    public void deleteTemplate() {
        String name = selectedTemplate.get();
        if (name != null && facade.deleteTemplate(name)) {
            templateNames.remove(name);
            selectedTemplate.set(null);
            errorMessage.set("");
            infoMessage.set("Шаблон «" + name + "» удалён");
        }
    }

    /** Сохраняет текущую форму как шаблон (новый прототип в реестре). */
    public void saveTemplate() {
        String name = newTemplateName.get() == null ? "" : newTemplateName.get().trim();
        if (name.isEmpty()) {
            showError("Введите название шаблона");
            return;
        }
        try {
            TripBuilder builder = builderFromForm();
            builder.copy().build();                      // проверка данных, прототип не меняется
            boolean replaced = facade.hasTemplate(name);
            facade.saveTemplate(name, builder);
            if (!replaced) {
                templateNames.add(name);
            }
            selectedTemplate.set(name);
            userTemplateSelected.set(true);
            newTemplateName.set("");
            errorMessage.set("");
            infoMessage.set("Шаблон «" + name + "» " + (replaced ? "обновлён" : "сохранён"));
        } catch (TripValidationException e) {
            showError(String.join("\n", e.getErrors()));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /** Считает ту же поездку на всех видах транспорта и упорядочивает по выбранному критерию. */
    public void compareTransport() {
        try {
            TransportChoiceStrategy strategy = choiceStrategy.get();
            List<ComparisonRow> rows = facade.compare(builderFromForm(true), strategy);
            if (rows.stream().noneMatch(ComparisonRow::isAvailable)) {
                showError(rows.get(0).note());
                return;
            }
            variants.clear();
            boolean first = true;
            for (ComparisonRow row : rows) {
                if (row.isAvailable()) {
                    CostBreakdown c = row.cost();
                    String explain = strategy.explain(row);
                    String note = first ? "★ лучший" + (explain.isEmpty() ? "" : ", " + explain) : explain;
                    variants.add(new VariantRow(row.transport().getDisplayName(),
                            money(c.total(), c.currency()), TravelTime.format(row.totalHours()), note));
                    first = false;
                } else {
                    variants.add(new VariantRow(row.transport().getDisplayName(), "—", "—", row.note()));
                }
            }
            errorMessage.set("");
            infoMessage.set("");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    /** Определяет расстояние между городами через геосервис и подставляет его в форму. */
    public void lookupDistance() {
        if (isBlank(origin.get()) || isBlank(destination.get())) {
            reportError("Укажите города отправления и назначения");
            return;
        }
        try {
            int hitsBefore = facade.distanceStats().cacheHits();
            RouteDistance route = facade.distance(origin.get(), destination.get());
            boolean fromCache = facade.distanceStats().cacheHits() > hitsBefore;
            TransportType type = transport.get() == null ? TransportType.CAR : transport.get();
            distance.set(formatNumber(route.forTransport(type)));
            errorMessage.set("");
            infoMessage.set((type == TransportType.PLANE
                    ? "Расстояние по прямой для самолёта: " + formatNumber(route.straightKm()) + " км"
                    : "Расстояние по дорогам (оценка): " + formatNumber(route.roadKm())
                            + " км, по прямой " + formatNumber(route.straightKm()) + " км")
                    + (fromCache ? " (из кэша)" : " (запрос к геосервису)"));
        } catch (IllegalArgumentException e) {
            reportError(e.getMessage());
        }
    }

    /**
     * Строит отчёт по последнему расчёту: вид отчёта и формат выбираются независимо.
     *
     * @return текст отчёта или {@code null}, если расчёта ещё нет (сообщение показывается в форме)
     */
    public String buildReport() {
        if (lastTrip == null) {
            reportError("Сначала рассчитайте поездку");
            return null;
        }
        return facade.report(reportType.get(), reportFormat.get(), lastTrip, lastCost);
    }

    /** Имя файла отчёта по умолчанию. */
    public String reportFileName() {
        String base = lastTrip == null ? "поездка"
                : "поездка_" + lastTrip.getOrigin() + "_" + lastTrip.getDestination();
        return base.replaceAll("[\\\\/:*?\"<>|,]", "").replace(' ', '_') + "." + reportExtension();
    }

    /** Расширение файла для выбранного формата. */
    public String reportExtension() {
        return facade.reportExtension(reportFormat.get());
    }

    /** Сохраняет отчёт в файл. */
    public void saveReport(File file) {
        String text = buildReport();
        if (text == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
            errorMessage.set("");
            infoMessage.set("Отчёт сохранён: " + file.getName());
        } catch (IOException e) {
            reportError("Не удалось сохранить файл: " + e.getMessage());
        }
    }

    /** Отправляет рассчитанную поездку в маршрут - через посредника. */
    public void addToJourney() {
        if (lastTrip == null) {
            reportError("Сначала рассчитайте поездку");
            return;
        }
        if (mediator != null) {
            mediator.notify(this, new PlannerEvent.AddLegRequested(lastTrip.getOrigin(),
                    lastTrip.getDestination(), lastTrip.getTransport(), "Расчёт"));
        }
    }

    /** Оформляет заявку на командировку по рассчитанной поездке - через посредника. */
    public void createRequest() {
        if (lastTrip == null) {
            reportError("Сначала рассчитайте поездку");
            return;
        }
        if (mediator != null) {
            mediator.notify(this, new PlannerEvent.CreateRequestRequested(lastTrip, lastCost));
        }
    }

    /** Вызывается посредником: заполнить форму участком маршрута и рассчитать его. */
    public void openTrip(String from, String to, TransportType type, int pax) {
        clear();
        origin.set(from);
        destination.set(to);
        transport.set(type);
        passengers.set(pax);
        lookupDistance();
        if (errorMessage.get().isEmpty()) {
            calculate();
        }
    }

    @Override
    public void setMediator(PlannerMediator mediator) {
        this.mediator = mediator;
    }

    /** Очищает форму и результат. */
    public void clear() {
        origin.set("");
        destination.set("");
        distance.set("");
        transport.set(TransportType.CAR);
        passengers.set(1);
        fuelConsumption.set("");
        roundTrip.set(false);
        options.values().forEach(selected -> selected.set(false));
        tolls.set("");
        nights.set("");
        pricePerNight.set("");
        selectedTemplate.set(null);
        newTemplateName.set("");
        clearResult();
        variants.clear();
        errorMessage.set("");
        infoMessage.set("");
    }

    // ------------------------------------------------------------ логика

    /** Переносит поля формы в строитель (без создания поездки). */
    private TripBuilder builderFromForm() {
        return builderFromForm(false);
    }

    /**
     * @param allCheckedOptions {@code true} - взять все отмеченные услуги (для сравнения транспорта,
     *                          где лишние отбрасываются для каждого варианта отдельно)
     */
    private TripBuilder builderFromForm(boolean allCheckedOptions) {
        TransportType type = transport.get();
        TripBuilder builder = Trip.builder()
                .from(origin.get())
                .to(destination.get())
                .distance(parseNumber(distance.get(), "Расстояние", true))
                .by(type)
                .passengers(passengers.get() == null ? 0 : passengers.get())
                .roundTrip(roundTrip.get())
                .options(selectedOptions(allCheckedOptions ? null : type))
                .accommodation(
                        (int) parseNumber(nights.get(), "Ночей", false),
                        BigDecimal.valueOf(parseNumber(pricePerNight.get(), "Цена за ночь", false)));

        if (type != null && type.usesFuel() && !isBlank(fuelConsumption.get())) {
            builder.fuelConsumption(parseNumber(fuelConsumption.get(), "Расход топлива", true));
        }
        if (type != null && type.allowsTolls()) {
            builder.tolls(BigDecimal.valueOf(parseNumber(tolls.get(), "Платные дороги", false)));
        }
        return builder;
    }

    private void fillForm(Trip trip) {
        origin.set(trip.getOrigin());
        destination.set(trip.getDestination());
        distance.set(formatNumber(trip.getDistanceKm()));
        transport.set(trip.getTransport());
        passengers.set(trip.getPassengers());
        fuelConsumption.set(trip.getTransport().usesFuel() ? formatNumber(trip.getFuelConsumption()) : "");
        roundTrip.set(trip.isRoundTrip());
        options.forEach((option, selected) -> selected.set(trip.getOptions().contains(option)));
        tolls.set(trip.getTollCost().signum() > 0 ? trip.getTollCost().toPlainString() : "");
        nights.set(trip.getNights() > 0 ? String.valueOf(trip.getNights()) : "");
        pricePerNight.set(trip.getPricePerNight().signum() > 0 ? trip.getPricePerNight().toPlainString() : "");
    }

    private void showResult(Trip trip, CostBreakdown cost) {
        lastTrip = trip;
        lastCost = cost;
        errorMessage.set("");
        infoMessage.set("");
        tripSummary.set(trip.toString());
        costTree.set(toRow(cost.items(), cost.currency()));
        totalCost.set(money(cost.total(), cost.currency()));
        perPassengerCost.set(money(cost.perPassenger(), cost.currency()));
        TravelTime time = facade.travelTime(trip);
        travelTime.set(time + (trip.isRoundTrip() ? " в одну сторону" : ""));
        travelTimeDetails.set(time.details());
        ApprovalDecision decision = facade.approve(trip, cost);
        approval.set(decision.approved() ? decision.handler() : "Отклонено");
        approvalRejected.set(!decision.approved());
        approvalDetails.set(decision.reason() + ".\nПуть по цепочке: " + String.join(" → ", decision.trace()));
        hasResult.set(true);
    }

    private void showError(String message) {
        clearResult();
        variants.clear();
        infoMessage.set("");
        errorMessage.set(message);
    }

    /** Сообщение об ошибке без сброса результата. */
    private void reportError(String message) {
        infoMessage.set("");
        errorMessage.set(message);
    }

    private void clearResult() {
        lastTrip = null;
        lastCost = null;
        tripSummary.set("");
        costTree.set(null);
        totalCost.set("—");
        perPassengerCost.set("—");
        approval.set("—");
        approvalDetails.set("");
        travelTime.set("—");
        travelTimeDetails.set("");
        approvalRejected.set(false);
        hasResult.set(false);
    }

    /** Отмеченные услуги; если указан вид транспорта - только доступные для него. */
    private Set<TripOption> selectedOptions(TransportType type) {
        Set<TripOption> result = EnumSet.noneOf(TripOption.class);
        options.forEach((option, selected) -> {
            if (selected.get() && (type == null || option.appliesTo(type))) {
                result.add(option);
            }
        });
        return result;
    }

    /** Переводит дерево статей модели в дерево строк для View (рекурсивно). */
    private static CostRow toRow(CostItem item, String currency) {
        List<CostRow> children = item.children().stream().map(c -> toRow(c, currency)).toList();
        return new CostRow(item.name(), money(item.amount(), currency), children);
    }

    /**
     * Разбирает число из текстового поля (допускается запятая).
     * Пустое необязательное поле считается нулём.
     */
    private static double parseNumber(String text, String field, boolean required) {
        if (isBlank(text)) {
            if (required) {
                throw new IllegalArgumentException("Заполните поле «" + field + "»");
            }
            return 0;
        }
        try {
            return Double.parseDouble(text.trim().replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Поле «" + field + "» должно быть числом");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String money(BigDecimal value, String currency) {
        return MONEY.format(value) + " " + currency;
    }

    // ------------------------------------------------------------ свойства для View

    public StringProperty originProperty() { return origin; }
    public StringProperty destinationProperty() { return destination; }
    public StringProperty distanceProperty() { return distance; }
    public ObjectProperty<TransportType> transportProperty() { return transport; }
    public ObjectProperty<Integer> passengersProperty() { return passengers; }
    public StringProperty fuelConsumptionProperty() { return fuelConsumption; }
    public BooleanProperty roundTripProperty() { return roundTrip; }
    public StringProperty tollsProperty() { return tolls; }
    public StringProperty nightsProperty() { return nights; }
    public StringProperty pricePerNightProperty() { return pricePerNight; }
    public StringProperty selectedTemplateProperty() { return selectedTemplate; }
    public ReadOnlyBooleanProperty userTemplateSelectedProperty() { return userTemplateSelected.getReadOnlyProperty(); }
    public ReadOnlyStringProperty templatesHintProperty() { return templatesHint.getReadOnlyProperty(); }
    public ViewModelCommand deleteTemplateCommand() { return deleteTemplateCommand; }
    public StringProperty newTemplateNameProperty() { return newTemplateName; }
    public ObjectProperty<ReportType> reportTypeProperty() { return reportType; }
    public ObjectProperty<ReportFormatType> reportFormatProperty() { return reportFormat; }

    public BooleanBinding fuelEditableBinding() { return fuelEditable; }
    public BooleanBinding tollsEditableBinding() { return tollsEditable; }

    public ReadOnlyStringProperty errorMessageProperty() { return errorMessage.getReadOnlyProperty(); }
    public ReadOnlyStringProperty infoMessageProperty() { return infoMessage.getReadOnlyProperty(); }
    public ReadOnlyStringProperty tripSummaryProperty() { return tripSummary.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<CostRow> costTreeProperty() { return costTree.getReadOnlyProperty(); }
    public BooleanProperty optionProperty(TripOption option) { return options.get(option); }
    public BooleanBinding optionAvailableBinding(TripOption option) { return optionAvailable.get(option); }
    public List<TripOption> getOptions() { return List.of(TripOption.values()); }
    public ReadOnlyStringProperty totalCostProperty() { return totalCost.getReadOnlyProperty(); }
    public ReadOnlyStringProperty perPassengerCostProperty() { return perPassengerCost.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty hasResultProperty() { return hasResult.getReadOnlyProperty(); }
    public ReadOnlyStringProperty approvalProperty() { return approval.getReadOnlyProperty(); }
    public ReadOnlyStringProperty approvalDetailsProperty() { return approvalDetails.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty approvalRejectedProperty() { return approvalRejected.getReadOnlyProperty(); }

    public ObservableList<TransportType> getTransportTypes() { return transportTypes; }
    public ObservableList<String> getTemplateNames() { return templateNames; }
    public ObservableList<VariantRow> getVariants() { return variants; }
    public ObservableList<TransportChoiceStrategy> getChoiceStrategies() { return choiceStrategies; }
    public ObjectProperty<TransportChoiceStrategy> choiceStrategyProperty() { return choiceStrategy; }
    public ReadOnlyStringProperty travelTimeProperty() { return travelTime.getReadOnlyProperty(); }
    public ReadOnlyStringProperty travelTimeDetailsProperty() { return travelTimeDetails.getReadOnlyProperty(); }
    public ObservableList<ReportType> getReportTypes() { return reportTypes; }
    public ObservableList<ReportFormatType> getReportFormats() { return reportFormats; }

    /** Строка таблицы сравнения видов транспорта. */
    public record VariantRow(String transport, String total, String time, String note) {
    }

    /** Узел дерева статей расходов для View: название, сумма и вложенные статьи. */
    public record CostRow(String name, String amount, List<CostRow> children) {
    }
}
