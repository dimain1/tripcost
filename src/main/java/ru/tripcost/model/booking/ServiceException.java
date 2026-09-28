package ru.tripcost.model.booking;

/**
 * Отказ сервиса-участника (нет мест, нет денег, сервис недоступен).
 */
public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }
}
