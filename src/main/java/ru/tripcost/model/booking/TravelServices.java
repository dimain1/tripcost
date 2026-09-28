package ru.tripcost.model.booking;

import java.math.BigDecimal;

/**
 * Сервисы-участники саги. В реальной системе это отдельные микросервисы;
 * здесь - независимые объекты со своим состоянием и имитацией сбоев.
 */
public record TravelServices(TicketService tickets, HotelService hotels, InsuranceService insurance,
                             BudgetService budget) {

    public static TravelServices create(BigDecimal departmentBudget) {
        return new TravelServices(new TicketService(), new HotelService(), new InsuranceService(),
                new BudgetService(departmentBudget));
    }
}
