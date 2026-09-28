package ru.tripcost.model.transport;

/** Конкретный создатель: создаёт PrivateCar. */
public class CarFactory extends TransportFactory {

    @Override
    public Transport createTransport() {
        return new PrivateCar();
    }
}
