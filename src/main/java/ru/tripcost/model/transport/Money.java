package ru.tripcost.model.transport;

import java.math.BigDecimal;

/** Вспомогательное форматирование сумм для описаний тарифов. */
final class Money {

    private Money() {
    }

    static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
