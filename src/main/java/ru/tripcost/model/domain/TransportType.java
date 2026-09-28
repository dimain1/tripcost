package ru.tripcost.model.domain;

/**
 * Вид транспорта.
 */
public enum TransportType {

    CAR("Личный автомобиль", 5, true, true),
    TAXI("Такси", 4, true, false),
    BUS("Автобус", 50, false, false),
    TRAIN("Поезд", 100, false, false),
    PLANE("Самолёт", 200, false, false);

    private final String displayName;
    private final int maxPassengers;
    private final boolean wholeVehicle;
    private final boolean usesFuel;

    TransportType(String displayName, int maxPassengers, boolean wholeVehicle, boolean usesFuel) {
        this.displayName = displayName;
        this.maxPassengers = maxPassengers;
        this.wholeVehicle = wholeVehicle;
        this.usesFuel = usesFuel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxPassengers() {
        return maxPassengers;
    }

    /** true — платят за машину целиком; false — каждый пассажир покупает билет. */
    public boolean isWholeVehicle() {
        return wholeVehicle;
    }

    /** true — стоимость считается по расходу топлива. */
    public boolean usesFuel() {
        return usesFuel;
    }

    /** Платные дороги имеют смысл только для поездки на машине. */
    public boolean allowsTolls() {
        return wholeVehicle;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
