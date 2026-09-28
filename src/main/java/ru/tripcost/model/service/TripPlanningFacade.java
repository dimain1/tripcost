package ru.tripcost.model.service;

import ru.tripcost.external.geo.WorldGeoApi;
import ru.tripcost.model.approval.ApprovalChain;
import ru.tripcost.model.approval.ApprovalDecision;
import ru.tripcost.model.approval.ApprovalHandler;
import ru.tripcost.model.approval.ApprovalRequest;
import ru.tripcost.model.concurrent.AsyncJob;
import ru.tripcost.model.concurrent.ProgressListener;
import ru.tripcost.model.concurrent.QuotePool;
import ru.tripcost.model.data.FileTripTemplateRepository;
import ru.tripcost.model.data.InMemoryTripTemplateRepository;
import ru.tripcost.model.data.SavedTrip;
import ru.tripcost.model.data.TripTemplateRepository;
import ru.tripcost.model.booking.BookingSaga;
import ru.tripcost.model.booking.TravelServices;
import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.City;
import ru.tripcost.model.domain.CityFactory;
import ru.tripcost.model.domain.RouteQuote;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripBuilder;
import ru.tripcost.model.domain.TripTemplateRegistry;
import ru.tripcost.model.domain.TripValidationException;
import ru.tripcost.model.journey.Journey;
import ru.tripcost.model.journey.JourneyQuote;
import ru.tripcost.model.journey.Leg;
import ru.tripcost.model.journey.LegIterator;
import ru.tripcost.model.journey.LegQuote;
import ru.tripcost.model.request.NotificationCenter;
import ru.tripcost.model.request.RequestJournal;
import ru.tripcost.model.request.TripRequest;
import ru.tripcost.model.pricing.ComparisonRow;
import ru.tripcost.model.pricing.CostBreakdown;
import ru.tripcost.model.pricing.TransportChoiceStrategy;
import ru.tripcost.model.pricing.TransportComparison;
import ru.tripcost.model.transport.TravelTime;
import ru.tripcost.model.transport.TravelTimeEstimators;
import ru.tripcost.model.pricing.TripCostCalculator;
import ru.tripcost.model.report.ReportFormatType;
import ru.tripcost.model.report.ReportType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Фасад модели: одна точка входа для планирования поездки.
 *
 * <p><b>Паттерн «Фасад» (Facade).</b> За фасадом стоят несколько подсистем:
 * реестр шаблонов, сервис расстояний с адаптером геобиблиотеки, калькулятор
 * с фабриками и декораторами транспорта, сравнение видов транспорта, отчёты
 * и фабрика городов. Модели представления работают только с фасадом и не знают,
 * как эти подсистемы устроены и в каком порядке их вызывать.</p>
 *
 * <p>К геосервису фасад обращается через заместителя {@link CachingDistanceProxy}
 * (паттерн «Заместитель»): повторные запросы расстояний обслуживаются из кэша.</p>
 */
public class TripPlanningFacade {

    private final TripTemplateRegistry templates = TripTemplateRegistry.withPresets();
    /** Сохранённые шаблоны пользователя (паттерн «Репозиторий»). */
    private final TripTemplateRepository savedTemplates;
    private final CachingDistanceProxy distances;
    private final TripCostCalculator calculator = new TripCostCalculator();
    private final TransportComparison comparison = new TransportComparison();
    private final CityFactory cities = new CityFactory();
    /** Пул потоков для фоновых расчётов (общий для всех фасадов). */
    private static final QuotePool pool = new QuotePool(AppConfig.getInstance().getPoolThreads());
    /** Первое звено цепочки согласования командировки. */
    private final ApprovalHandler approval = ApprovalChain.standard();
    /** Наблюдатели за заявками: журнал переходов и уведомления. */
    private final RequestJournal requestJournal = new RequestJournal();
    private final NotificationCenter notifications = new NotificationCenter();
    private int nextRequestId = 1;
    /** Сервисы-участники саги бронирования и сама сага. */
    private final TravelServices travelServices =
            TravelServices.create(AppConfig.getInstance().getDepartmentBudget());
    private final BookingSaga bookingSaga = BookingSaga.standard(travelServices);

    /** Геосервис с имитацией сетевой задержки из настроек, закрытый заместителем; шаблоны - в файле. */
    public TripPlanningFacade() {
        this(new CachingDistanceProxy(() -> new GeoApiDistanceAdapter(
                new WorldGeoApi(), AppConfig.getInstance().getGeoLatencyMs())),
                new FileTripTemplateRepository(AppConfig.getInstance().getStorageDir().resolve("templates.properties")));
    }

    public TripPlanningFacade(CachingDistanceProxy distances) {
        this(distances, new InMemoryTripTemplateRepository());
    }

    public TripPlanningFacade(CachingDistanceProxy distances, TripTemplateRepository savedTemplates) {
        this.distances = distances;
        this.savedTemplates = savedTemplates;
        for (SavedTrip saved : savedTemplates.findAll()) {       // шаблоны прошлых запусков
            templates.register(saved.name(), saved.toBuilder());
        }
    }

    // ------------------------------------------------------------ шаблоны

    public List<String> templateNames() {
        return templates.names();
    }

    public boolean hasTemplate(String name) {
        return templates.contains(name);
    }

    /** Копия шаблона (прототипа) для заполнения формы. */
    public TripBuilder template(String name) {
        return templates.create(name);
    }

    /** Сохраняет шаблон в реестре и в репозитории - он будет доступен после перезапуска. */
    public void saveTemplate(String name, TripBuilder builder) {
        if (isPresetTemplate(name)) {
            throw new IllegalArgumentException("Встроенный шаблон «" + name.trim() + "» заменить нельзя");
        }
        SavedTrip saved = new SavedTrip(name, builder.copy().build());
        savedTemplates.save(saved);
        templates.register(saved.name(), builder);
    }

    /** Удаляет шаблон пользователя; встроенные шаблоны удалить нельзя. */
    public boolean deleteTemplate(String name) {
        if (!isUserTemplate(name)) {
            return false;
        }
        savedTemplates.deleteById(name);
        return templates.remove(name);
    }

    public boolean isUserTemplate(String name) {
        return name != null && savedTemplates.existsById(name.trim());
    }

    private static boolean isPresetTemplate(String name) {
        return name != null && java.util.Arrays.stream(ru.tripcost.model.domain.TripPreset.values())
                .anyMatch(p -> p.getDisplayName().equalsIgnoreCase(name.trim()));
    }

    /** Где хранятся шаблоны пользователя (для подсказки в интерфейсе). */
    public String templatesLocation() {
        return savedTemplates instanceof FileTripTemplateRepository f ? f.file().toString() : "в памяти";
    }

    // ------------------------------------------------------------ расстояния

    public RouteDistance distance(String from, String to) {
        return distances.distance(from, to);
    }

    public List<String> knownCities() {
        return distances.knownCities();
    }

    /** Статистика заместителя геосервиса. */
    public CachingDistanceProxy.Stats distanceStats() {
        return distances.stats();
    }

    public void clearDistanceCache() {
        distances.clearCache();
    }

    // ------------------------------------------------------------ расчёт

    public CostBreakdown calculate(Trip trip) {
        return calculator.calculate(trip);
    }

    public List<ComparisonRow> compare(TripBuilder prototype) {
        return comparison.compare(prototype);
    }

    /** Сравнение транспорта с выбранной стратегией выбора лучшего варианта. */
    public List<ComparisonRow> compare(TripBuilder prototype, TransportChoiceStrategy strategy) {
        comparison.setStrategy(strategy);
        return comparison.compare(prototype);
    }

    public List<TransportChoiceStrategy> choiceStrategies() {
        return TransportComparison.strategies();
    }

    /** Время в пути в одну сторону (шаблонный метод оценки времени). */
    public TravelTime travelTime(Trip trip) {
        return TravelTimeEstimators.forType(trip.getTransport()).estimate(trip.getDistanceKm());
    }

    /**
     * Быстрая оценка по названиям городов: фасад сам определяет расстояние,
     * собирает поездку строителем и считает стоимость.
     */
    public Quote quickQuote(String from, String to, TransportType transport, int passengers, boolean roundTrip) {
        RouteDistance route = distances.distance(from, to);
        Trip trip = Trip.builder()
                .from(from).to(to)
                .distance(route.forTransport(transport))
                .by(transport)
                .passengers(passengers)
                .roundTrip(roundTrip)
                .build();
        return new Quote(trip, calculator.calculate(trip));
    }

    /**
     * Стоимость поездки из города во все остальные города справочника,
     * от дешёвых к дорогим. Города в результатах - общие объекты фабрики городов.
     */
    public List<RouteQuote> directionsFrom(String origin, TransportType transport, int passengers, boolean roundTrip) {
        City from = cities.get(origin);
        List<RouteQuote> quotes = new ArrayList<>();
        for (String name : distances.knownCities()) {
            City to = cities.get(name);
            if (to != from) {
                quotes.add(routeQuote(from, to, transport, passengers, roundTrip));
            }
        }
        return sortQuotes(quotes);
    }

    /**
     * То же, но в фоне: стоимость до каждого города считается отдельной задачей
     * в пуле потоков (паттерн «Future / Promise»). Метод сразу возвращает задание,
     * результат придёт в обещании {@code job.result()}.
     */
    public AsyncJob<RouteQuote, List<RouteQuote>> directionsFromAsync(String origin, TransportType transport,
                                                                      int passengers, boolean roundTrip,
                                                                      ProgressListener listener) {
        City from = cities.get(origin);
        List<Supplier<RouteQuote>> tasks = new ArrayList<>();
        for (String name : distances.knownCities()) {
            City to = cities.get(name);
            if (to != from) {
                tasks.add(() -> routeQuote(from, to, transport, passengers, roundTrip));
            }
        }
        return new AsyncJob<>(tasks, pool, this::sortQuotes, listener);
    }

    /** Число потоков в пуле расчётов. */
    public int poolSize() {
        return pool.size();
    }

    /** Одно направление: расчёт и согласование. Вызывается и из пула потоков. */
    private RouteQuote routeQuote(City from, City to, TransportType transport, int passengers, boolean roundTrip) {
        try {
            Quote q = quickQuote(from.getName(), to.getName(), transport, passengers, roundTrip);
            return new RouteQuote(from, to, transport, q.trip().getDistanceKm(),
                    q.cost().total(), q.cost().perPassenger(), "",
                    approve(q.trip(), q.cost()).summary());
        } catch (TripValidationException e) {
            return new RouteQuote(from, to, transport, 0, null, null, e.getErrors().get(0), "");
        }
    }

    private List<RouteQuote> sortQuotes(List<RouteQuote> quotes) {
        List<RouteQuote> sorted = new ArrayList<>(quotes);
        sorted.sort(Comparator.comparing(RouteQuote::isAvailable).reversed()
                .thenComparing(q -> q.total() == null ? BigDecimal.ZERO : q.total()));
        return sorted;
    }

    /**
     * Стоимость составного маршрута. Участки перебираются итераторами маршрута:
     * прямым - для расчёта, с фильтром по транспорту - для сводки по километрам.
     */
    public JourneyQuote quoteJourney(Journey journey, int passengers) {
        List<LegQuote> quotes = new ArrayList<>();
        Map<Integer, Double> kmByNumber = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Leg previous = null;
        LegIterator it = journey.iterator();
        while (it.hasNext()) {
            Leg leg = it.next();
            String gap = previous != null && !previous.to().equalsIgnoreCase(leg.from())
                    ? "Разрыв: предыдущий участок заканчивается в городе " + previous.to() : "";
            try {
                Quote q = quickQuote(leg.from(), leg.to(), leg.transport(), passengers, false);
                total = total.add(q.cost().total());
                kmByNumber.put(it.number(), q.trip().getDistanceKm());
                quotes.add(new LegQuote(it.number(), leg, q.trip().getDistanceKm(), q.cost().total(), gap));
            } catch (TripValidationException e) {
                quotes.add(new LegQuote(it.number(), leg, 0, null, e.getErrors().get(0)));
            } catch (IllegalArgumentException e) {
                quotes.add(new LegQuote(it.number(), leg, 0, null, e.getMessage()));
            }
            previous = leg;
        }
        Map<TransportType, Double> kmByTransport = new EnumMap<>(TransportType.class);
        for (TransportType type : TransportType.values()) {
            LegIterator byType = journey.iterator(type);
            double km = 0;
            boolean used = false;
            while (byType.hasNext()) {
                byType.next();
                used = true;
                km += kmByNumber.getOrDefault(byType.number(), 0.0);
            }
            if (used) {
                kmByTransport.put(type, km);
            }
        }
        BigDecimal perPassenger = total.divide(BigDecimal.valueOf(Math.max(1, passengers)), 2, RoundingMode.HALF_UP);
        return new JourneyQuote(quotes, total, perPassenger, kmByTransport);
    }

    /** Статистика фабрики городов: сколько запросов и сколько реально созданных объектов. */
    public CityStats cityStats() {
        return new CityStats(cities.requestCount(), cities.createdCount());
    }

    // ------------------------------------------------------------ согласование

    /** Отправляет поездку в первое звено цепочки согласования. */
    public ApprovalDecision approve(Trip trip, CostBreakdown cost) {
        return approval.handle(new ApprovalRequest(trip, cost));
    }

    /** Звенья цепочки согласования по порядку. */
    public List<String> approvalChain() {
        return approval.chainNames();
    }

    // ------------------------------------------------------------ заявки на командировку

    /** Новая заявка в состоянии «Черновик»; журнал и уведомления подписываются на неё. */
    public TripRequest createRequest(Trip trip, CostBreakdown cost) {
        TripRequest request = new TripRequest(nextRequestId++, trip, cost, approval, bookingSaga);
        request.addListener(requestJournal);
        request.addListener(notifications);
        return request;
    }

    /** Сервисы бронирования: имитация сбоев и остаток бюджета для интерфейса. */
    public TravelServices travelServices() {
        return travelServices;
    }

    public RequestJournal requestJournal() {
        return requestJournal;
    }

    public NotificationCenter notifications() {
        return notifications;
    }

    // ------------------------------------------------------------ отчёты

    public String report(ReportType type, ReportFormatType format, Trip trip, CostBreakdown cost) {
        return type.create(format.create(), trip, cost).render();
    }

    public String reportExtension(ReportFormatType format) {
        return format.create().extension();
    }

    /** Поездка и её стоимость. */
    public record Quote(Trip trip, CostBreakdown cost) {
    }

    /** Запросов к фабрике городов и созданных объектов City. */
    public record CityStats(int requests, int created) {
    }
}
