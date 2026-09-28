package ru.tripcost.viewmodel;

/**
 * Коллега посредника: модель представления вкладки, которая сообщает о событиях
 * посреднику, а не другим вкладкам.
 */
public interface PlannerColleague {

    void setMediator(PlannerMediator mediator);
}
