package ru.tripcost.model.booking;

/**
 * Запись журнала саги.
 */
public record SagaLogEntry(Kind kind, String step, String message) {

    public enum Kind {
        DONE("✓"), SKIPPED("–"), FAILED("✗"), COMPENSATED("↩");

        private final String mark;

        Kind(String mark) {
            this.mark = mark;
        }
    }

    @Override
    public String toString() {
        return kind.mark + " " + step + (message.isEmpty() ? "" : ": " + message);
    }
}
