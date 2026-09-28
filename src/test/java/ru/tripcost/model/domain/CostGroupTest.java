package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CostGroupTest {

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }

    @Test
    void groupSumsNestedItemsRecursively() {
        CostGroup travel = CostGroup.of("Проезд",
                new CostLeaf("Билеты", bd("1000")),
                new CostLeaf("Багаж", bd("200")));
        CostGroup all = CostGroup.of("Все расходы", travel, new CostLeaf("Проживание", bd("3000.5")));

        assertEquals(bd("1200.00"), travel.amount());
        assertEquals(bd("4200.50"), all.amount());
        assertEquals(3, all.leafCount());
    }

    @Test
    void leafAndGroupShareInterface() {
        CostItem leaf = new CostLeaf("Билеты", bd("10"));
        CostItem group = CostGroup.of("Проезд", leaf);

        assertFalse(leaf.isGroup());
        assertTrue(leaf.children().isEmpty());
        assertTrue(group.isGroup());
        assertEquals(leaf.amount(), group.amount());
    }

    @Test
    void withReturnsNewGroup() {
        CostGroup group = CostGroup.of("Проезд", new CostLeaf("Билеты", bd("100")));
        CostGroup bigger = group.with(new CostLeaf("Страховка", bd("3")));

        assertEquals(1, group.children().size());
        assertEquals(2, bigger.children().size());
        assertEquals(bd("103.00"), bigger.amount());
    }

    @Test
    void emptyGroupIsZero() {
        assertEquals(0, CostGroup.of("Пусто").amount().signum());
    }
}
