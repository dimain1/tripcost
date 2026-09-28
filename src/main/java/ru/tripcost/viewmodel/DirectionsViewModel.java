package ru.tripcost.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.tripcost.model.concurrent.AsyncJob;
import ru.tripcost.model.domain.RouteQuote;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.service.CachingDistanceProxy;
import ru.tripcost.model.service.TripPlanningFacade;
import ru.tripcost.model.service.TripPlanningFacade.CityStats;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * ViewModel вкладки «Направления»: стоимость поездки из выбранного города
 * во все города справочника. Работает только через фасад; города в строках -
 * общие объекты фабрики городов (паттерн «Приспособленец»). Расстояния фасад
 * получает через кэширующего заместителя геосервиса (паттерн «Заместитель»).
 */
public class DirectionsViewModel implements PlannerColleague {

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("ru", "RU")));

    private final TripPlanningFacade facade = new TripPlanningFacade();

    private final ObservableList<String> cities = FXCollections.observableArrayList(facade.knownCities());
    private final ObservableList<TransportType> transportTypes =
            FXCollections.observableArrayList(TransportType.values());

    private final ObjectProperty<String> origin = new SimpleObjectProperty<>("Ставрополь");
    private final ObjectProperty<TransportType> transport = new SimpleObjectProperty<>(TransportType.BUS);
    private final ObjectProperty<Integer> passengers = new SimpleObjectProperty<>(1);
    private final BooleanProperty roundTrip = new SimpleBooleanProperty(true);

    private final ObservableList<DirectionRow> rows = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper stats = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper proxyStats = new ReadOnlyStringWrapper(
            "Расстояния ещё не запрашивались: кэш заместителя геосервиса пуст.");
    private final ReadOnlyStringWrapper errorMessage = new ReadOnlyStringWrapper("");
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(-1);

    private PlannerMediator mediator;

    // --- фоновый расчёт
    private final ReadOnlyBooleanWrapper busy = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(0);
    private volatile AsyncJob<RouteQuote, List<RouteQuote>> job;
    /** По умолчанию - в том же потоке (для тестов без интерфейса). */
    private Executor ui = Runnable::run;

    // --- команды MVVM
    private final ViewModelCommand showCommand = new ViewModelCommand(this::show);
    private final ViewModelCommand cancelCommand = new ViewModelCommand(this::cancel, busy);
    private final ViewModelCommand addToJourneyCommand =
            new ViewModelCommand(this::addSelectedToJourney, selectedIndex.greaterThanOrEqualTo(0));
    private final ViewModelCommand clearCacheCommand = new ViewModelCommand(this::clearCache);
    /** Параметры последнего показа: из какого города и на чём. */
    private String shownOrigin;
    private TransportType shownTransport;

    /**
     * Считает направления в фоне (паттерн «Future / Promise»): стоимость до каждого
     * города - отдельная задача в пуле потоков. Интерфейс не блокируется, прогресс
     * и результат передаются в поток интерфейса через {@link #setUiExecutor}.
     *
     * @return обещание, которое выполняется после обновления таблицы
     */
    public CompletableFuture<Void> show() {
        if (origin.get() == null || transport.get() == null) {
            errorMessage.set("Выберите город и вид транспорта");
            return CompletableFuture.completedFuture(null);
        }
        cancel();                                        // предыдущий расчёт больше не нужен
        errorMessage.set("");
        busy.set(true);
        progress.set(0);
        long start = System.nanoTime();
        String shownFrom = origin.get();
        TransportType shownBy = transport.get();
        AsyncJob<RouteQuote, List<RouteQuote>> current = facade.directionsFromAsync(shownFrom, shownBy,
                passengers.get() == null ? 1 : passengers.get(), roundTrip.get(),
                (done, total) -> ui.execute(() -> progress.set((double) done / total)));
        job = current;
        return current.result().handle((quotes, error) -> {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            ui.execute(() -> finish(current, quotes, error, elapsedMs, shownFrom, shownBy));
            return null;
        });
    }

    /** Отменяет текущий расчёт. */
    public void cancel() {
        if (job != null && !job.isDone()) {
            job.cancel();
        }
    }

    /** Выполняется в потоке интерфейса. */
    private void finish(AsyncJob<?, ?> finished, List<RouteQuote> quotes, Throwable error, long elapsedMs,
                        String shownFrom, TransportType shownBy) {
        if (finished != job) {
            return;                                     // устаревший результат: уже запущен новый расчёт
        }
        busy.set(false);
        if (error != null) {
            proxyStats.set(isCancellation(error) ? "Расчёт отменён." : "Ошибка расчёта: " + error.getMessage());
            return;
        }
        rows.clear();
        shownOrigin = shownFrom;
        shownTransport = shownBy;
        for (RouteQuote q : quotes) {
            rows.add(new DirectionRow(q.to().getName(), q.to().getRegion(),
                    q.isAvailable() ? String.valueOf(Math.round(q.distanceKm())) : "—",
                    q.isAvailable() ? money(q.total()) : "—",
                    q.isAvailable() ? money(q.perPassenger()) : q.note(), q.approval()));
        }
        progress.set(1);
        CityStats s = facade.cityStats();
        stats.set("Строк в таблице: " + quotes.size() + ". Запросов городов к фабрике: " + s.requests()
                + ", создано объектов City: " + s.created() + ".");
        updateProxyStats("Таблица построена за " + elapsedMs + " мс (" + quotes.size() + " задач в пуле из "
                + facade.poolSize() + " потоков). ");
    }

    private static boolean isCancellation(Throwable error) {
        return error instanceof CancellationException || error.getCause() instanceof CancellationException;
    }

    /** Как передавать результаты в поток интерфейса (в программе - Platform::runLater). */
    public void setUiExecutor(Executor ui) {
        this.ui = ui;
    }

    /** Добавляет выбранное направление в маршрут - через посредника. */
    public void addSelectedToJourney() {
        int i = selectedIndex.get();
        if (i < 0 || i >= rows.size()) {
            errorMessage.set("Выберите направление в таблице");
            return;
        }
        DirectionRow row = rows.get(i);
        if (row.total().equals("—")) {
            errorMessage.set("Это направление недоступно для выбранного транспорта");
            return;
        }
        errorMessage.set("");
        if (mediator != null) {
            mediator.notify(this, new PlannerEvent.AddLegRequested(shownOrigin, row.city(), shownTransport,
                    "Направления"));
        }
    }

    @Override
    public void setMediator(PlannerMediator mediator) {
        this.mediator = mediator;
    }

    /** Сбрасывает кэш заместителя геосервиса: следующий показ снова обратится к геосервису. */
    public void clearCache() {
        facade.clearDistanceCache();
        updateProxyStats("Кэш расстояний очищен. ");
    }

    private void updateProxyStats(String prefix) {
        CachingDistanceProxy.Stats p = facade.distanceStats();
        proxyStats.set(prefix + "Запросов расстояний: " + p.requests() + ", из кэша: " + p.cacheHits()
                + ", обращений к геосервису: " + p.realCalls() + ", пар городов в кэше: " + p.cachedRoutes() + ".");
    }

    private static String money(BigDecimal value) {
        return MONEY.format(value) + " ₽";
    }

    public ObservableList<String> getCities() { return cities; }
    public ObservableList<TransportType> getTransportTypes() { return transportTypes; }
    public ObjectProperty<String> originProperty() { return origin; }
    public ObjectProperty<TransportType> transportProperty() { return transport; }
    public ObjectProperty<Integer> passengersProperty() { return passengers; }
    public BooleanProperty roundTripProperty() { return roundTrip; }
    public ObservableList<DirectionRow> getRows() { return rows; }
    public ReadOnlyStringProperty statsProperty() { return stats.getReadOnlyProperty(); }
    public ReadOnlyStringProperty proxyStatsProperty() { return proxyStats.getReadOnlyProperty(); }
    public IntegerProperty selectedIndexProperty() { return selectedIndex; }
    public ViewModelCommand showCommand() { return showCommand; }
    public ViewModelCommand cancelCommand() { return cancelCommand; }
    public ReadOnlyBooleanProperty busyProperty() { return busy.getReadOnlyProperty(); }
    public ReadOnlyDoubleProperty progressProperty() { return progress.getReadOnlyProperty(); }
    public ViewModelCommand addToJourneyCommand() { return addToJourneyCommand; }
    public ViewModelCommand clearCacheCommand() { return clearCacheCommand; }
    public ReadOnlyStringProperty errorMessageProperty() { return errorMessage.getReadOnlyProperty(); }

    /** Строка таблицы направлений. */
    public record DirectionRow(String city, String region, String km, String total, String perPassenger,
                               String approval) {
    }
}
