package ru.tripcost.model.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Отдельная статья расходов - лист (Leaf) паттерна «Компоновщик».
 */
public final class CostLeaf implements CostItem {

    private final String name;
    private final BigDecimal amount;

    public CostLeaf(String name, BigDecimal amount) {
        this.name = Objects.requireNonNull(name);
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public BigDecimal amount() {
        return amount;
    }

    @Override
    public List<CostItem> children() {
        return List.of();
    }

    @Override
    public String toString() {
        return name + ": " + amount;
    }
}
