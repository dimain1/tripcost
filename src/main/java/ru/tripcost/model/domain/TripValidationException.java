package ru.tripcost.model.domain;

import java.util.List;

/**
 * Ошибка проверки данных поездки. Содержит список всех найденных проблем.
 */
public class TripValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> errors;

    public TripValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
