package ru.tripcost.model.transport;

import ru.tripcost.model.domain.TransportType;

/**
 * Конкретные классы оценки времени для каждого вида транспорта и выбор нужного.
 */
public final class TravelTimeEstimators {

    private TravelTimeEstimators() {
    }

    public static TravelTimeEstimator forType(TransportType type) {
        return switch (type) {
            case CAR -> new CarTime();
            case TAXI -> new TaxiTime();
            case BUS -> new BusTime();
            case TRAIN -> new TrainTime();
            case PLANE -> new PlaneTime();
        };
    }

    /** Личный автомобиль: 80 км/ч, 15 минут отдыха на каждые 2 часа за рулём. */
    static final class CarTime extends TravelTimeEstimator {
        @Override
        protected double speedKmh() {
            return 80;
        }

        @Override
        protected double stopsHours(double movingHours) {
            return Math.floor(movingHours / 2) * 0.25;
        }

        @Override
        protected String stopsName() {
            return "отдых водителя";
        }
    }

    /** Такси: 70 км/ч и 15 минут на подачу машины. */
    static final class TaxiTime extends TravelTimeEstimator {
        @Override
        protected double speedKmh() {
            return 70;
        }

        @Override
        protected double boardingHours() {
            return 0.25;
        }
    }

    /** Автобус: 65 км/ч, дорога до автостанции, посадка, 10 минут стоянки на каждый час пути. */
    static final class BusTime extends TravelTimeEstimator {
        @Override
        protected double speedKmh() {
            return 65;
        }

        @Override
        protected double accessHours() {
            return 0.5;
        }

        @Override
        protected double boardingHours() {
            return 0.25;
        }

        @Override
        protected double stopsHours(double movingHours) {
            return Math.floor(movingHours) * 10 / 60.0;
        }

        @Override
        protected double egressHours() {
            return 0.5;
        }
    }

    /** Поезд: 75 км/ч с учётом стоянок, дорога до вокзала и посадка. */
    static final class TrainTime extends TravelTimeEstimator {
        @Override
        protected double speedKmh() {
            return 75;
        }

        @Override
        protected double accessHours() {
            return 0.5;
        }

        @Override
        protected double boardingHours() {
            return 0.5;
        }

        @Override
        protected double egressHours() {
            return 0.5;
        }
    }

    /** Самолёт: 700 км/ч, но долгая дорога до аэропорта, регистрация и досмотр. */
    static final class PlaneTime extends TravelTimeEstimator {
        @Override
        protected double speedKmh() {
            return 700;
        }

        @Override
        protected double accessHours() {
            return 1.0;
        }

        @Override
        protected double boardingHours() {
            return 1.5;
        }

        @Override
        protected double egressHours() {
            return 0.75;
        }
    }
}
