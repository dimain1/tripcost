package ru.tripcost.model.config;

import ru.tripcost.model.domain.TransportType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * Настройки и тарифы приложения.
 *
 * <p><b>Паттерн «Одиночка» (Singleton).</b> Настройки читаются из файла
 * {@code app.properties} один раз, и все части программы (расчёт стоимости,
 * экраны) работают с одним и тем же экземпляром. Конструктор закрыт,
 * доступ только через {@link #getInstance()}.</p>
 *
 * <p>Используется идиома «Initialization-on-demand holder»: экземпляр
 * создаётся лениво, при первом вызове {@code getInstance()}, а потокобезопасность
 * обеспечивает сама JVM при загрузке вложенного класса {@link Holder}.</p>
 */
public final class AppConfig {

    private static final String RESOURCE = "/app.properties";

    private final String currency;
    private final BigDecimal fuelPrice;
    private final double defaultFuelConsumption;
    private final Map<TransportType, Tariff> tariffs;
    private final BigDecimal baggageFee;
    private final BigDecimal insurancePercent;
    private final BigDecimal peakSeasonPercent;
    private final BigDecimal discountPercent;
    private final long geoLatencyMs;
    private final double policyTaxiMaxKm;
    private final double policyPlaneMinKm;
    private final BigDecimal policyHotelMaxPerNight;
    private final BigDecimal autoApprovalLimit;
    private final BigDecimal managerApprovalLimit;
    private final BigDecimal directorApprovalLimit;
    private final BigDecimal hourValue;
    private final BigDecimal departmentBudget;
    private final String storageDir;
    private final int poolThreads;

    /** Закрытый конструктор: создать объект извне класса нельзя. */
    private AppConfig() {
        Properties props = load();
        this.currency = props.getProperty("currency", "₽");
        this.fuelPrice = decimal(props, "fuel.price");
        this.defaultFuelConsumption = decimal(props, "fuel.consumption.default").doubleValue();

        Map<TransportType, Tariff> map = new EnumMap<>(TransportType.class);
        for (TransportType type : TransportType.values()) {
            if (type.usesFuel()) {
                continue; // для личного автомобиля тариф не нужен: считается топливо
            }
            String prefix = "tariff." + type.name() + ".";
            map.put(type, new Tariff(decimal(props, prefix + "baseFee"), decimal(props, prefix + "perKm")));
        }
        this.tariffs = Collections.unmodifiableMap(map);

        this.baggageFee = decimal(props, "option.baggage.perPassengerLeg");
        this.insurancePercent = decimal(props, "option.insurance.percent");
        this.peakSeasonPercent = decimal(props, "option.peakSeason.percent");
        this.discountPercent = decimal(props, "option.discount.percent");
        this.geoLatencyMs = decimal(props, "geo.latencyMs").longValue();
        this.policyTaxiMaxKm = decimal(props, "policy.taxi.maxKm").doubleValue();
        this.policyPlaneMinKm = decimal(props, "policy.plane.minKm").doubleValue();
        this.policyHotelMaxPerNight = decimal(props, "policy.hotel.maxPerNight");
        this.autoApprovalLimit = decimal(props, "approval.auto.limit");
        this.managerApprovalLimit = decimal(props, "approval.manager.limit");
        this.directorApprovalLimit = decimal(props, "approval.director.limit");
        this.hourValue = decimal(props, "choice.hourValue");
        this.departmentBudget = decimal(props, "budget.department");
        this.storageDir = props.getProperty("storage.dir", "").trim();
        this.poolThreads = decimal(props, "pool.threads").intValue();
    }

    /** Держатель экземпляра: класс загружается JVM только при первом обращении. */
    private static final class Holder {
        private static final AppConfig INSTANCE = new AppConfig();
    }

    /** Единственная точка доступа к экземпляру. */
    public static AppConfig getInstance() {
        return Holder.INSTANCE;
    }

    public String getCurrency() {
        return currency;
    }

    /** Цена топлива за литр. */
    public BigDecimal getFuelPrice() {
        return fuelPrice;
    }

    /** Расход топлива по умолчанию, л/100 км. */
    public double getDefaultFuelConsumption() {
        return defaultFuelConsumption;
    }

    /** Тариф вида транспорта. Для личного автомобиля тарифа нет. */
    public Tariff getTariff(TransportType type) {
        Tariff tariff = tariffs.get(type);
        if (tariff == null) {
            throw new IllegalArgumentException("Для вида транспорта «" + type + "» тариф не задан");
        }
        return tariff;
    }

    public Map<TransportType, Tariff> getTariffs() {
        return tariffs;
    }

    /** Багаж: плата за пассажира за поездку в одну сторону. */
    public BigDecimal getBaggageFee() {
        return baggageFee;
    }

    /** Страховка, % от стоимости проезда. */
    public BigDecimal getInsurancePercent() {
        return insurancePercent;
    }

    /** Наценка высокого сезона, %. */
    public BigDecimal getPeakSeasonPercent() {
        return peakSeasonPercent;
    }

    /** Льготная скидка, %. */
    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    /** Имитация задержки сетевого запроса к геосервису, мс. */
    public long getGeoLatencyMs() {
        return geoLatencyMs;
    }

    /** Политика поездок: максимальное расстояние на такси, км. */
    public double getPolicyTaxiMaxKm() {
        return policyTaxiMaxKm;
    }

    /** Политика поездок: минимальное расстояние для перелёта, км. */
    public double getPolicyPlaneMinKm() {
        return policyPlaneMinKm;
    }

    /** Политика поездок: максимальная цена проживания за ночь. */
    public BigDecimal getPolicyHotelMaxPerNight() {
        return policyHotelMaxPerNight;
    }

    /** Лимит автосогласования. */
    public BigDecimal getAutoApprovalLimit() {
        return autoApprovalLimit;
    }

    /** Лимит руководителя отдела. */
    public BigDecimal getManagerApprovalLimit() {
        return managerApprovalLimit;
    }

    /** Лимит директора. */
    public BigDecimal getDirectorApprovalLimit() {
        return directorApprovalLimit;
    }

    /** Стоимость часа времени пассажира для выбора транспорта «время - деньги». */
    public BigDecimal getHourValue() {
        return hourValue;
    }

    /** Бюджет отдела на командировки (сага бронирования списывает из него). */
    public BigDecimal getDepartmentBudget() {
        return departmentBudget;
    }

    /**
     * Папка для сохранённых данных: системное свойство {@code tripcost.storage.dir}
     * (например, для тестов), затем {@code storage.dir} из настроек, иначе папка
     * {@code .tripcost} в домашнем каталоге пользователя.
     */
    public java.nio.file.Path getStorageDir() {
        String override = System.getProperty("tripcost.storage.dir");
        if (override != null && !override.isBlank()) {
            return java.nio.file.Path.of(override);
        }
        if (!storageDir.isEmpty()) {
            return java.nio.file.Path.of(storageDir);
        }
        return java.nio.file.Path.of(System.getProperty("user.home"), ".tripcost");
    }

    /** Число потоков в пуле фоновых расчётов. */
    public int getPoolThreads() {
        return poolThreads;
    }

    private static Properties load() {
        try (InputStream in = AppConfig.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Не найден файл настроек " + RESOURCE);
            }
            Properties props = new Properties();
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
            return props;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать " + RESOURCE, e);
        }
    }

    private static BigDecimal decimal(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("В " + RESOURCE + " нет параметра " + key);
        }
        return new BigDecimal(value.trim());
    }
}
