package ru.tripcost.model.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Группа статей расходов - составной объект (Composite) паттерна «Компоновщик».
 *
 * <p>Может содержать как отдельные статьи, так и другие группы. Сумма группы
 * рекурсивно складывается из сумм вложенных элементов. Объект неизменяемый:
 * {@link #with(CostItem)} возвращает новую группу.</p>
 */
public final class CostGroup implements CostItem {

    private final String name;
    private final List<CostItem> items;

    public CostGroup(String name, List<? extends CostItem> items) {
        this.name = Objects.requireNonNull(name);
        this.items = List.copyOf(items);
    }

    public static CostGroup of(String name, CostItem... items) {
        return new CostGroup(name, List.of(items));
    }

    @Override
    public String name() {
        return name;
    }

    /** Сумма всех вложенных статей (рекурсивно через amount() каждого элемента). */
    @Override
    public BigDecimal amount() {
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        for (CostItem item : items) {
            sum = sum.add(item.amount());
        }
        return sum;
    }

    @Override
    public List<CostItem> children() {
        return items;
    }

    @Override
    public boolean isGroup() {
        return true;
    }

    /** Новая группа с добавленной статьёй. */
    public CostGroup with(CostItem item) {
        List<CostItem> copy = new ArrayList<>(items);
        copy.add(item);
        return new CostGroup(name, copy);
    }

    /** Количество отдельных статей во всём поддереве. */
    public int leafCount() {
        int count = 0;
        for (CostItem item : items) {
            count += item instanceof CostGroup group ? group.leafCount() : 1;
        }
        return count;
    }

    @Override
    public String toString() {
        return name + " " + items;
    }
}
