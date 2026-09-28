package ru.tripcost.model.transport;

/** Конкретный создатель: создаёт Train. */
public class TrainFactory extends TransportFactory {

    @Override
    public Transport createTransport() {
        return new Train();
    }
}
