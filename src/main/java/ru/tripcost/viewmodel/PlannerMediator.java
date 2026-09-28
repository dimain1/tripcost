package ru.tripcost.viewmodel;

/**
 * Посредник между вкладками приложения.
 *
 * <p><b>Паттерн «Посредник» (Mediator).</b> Модели представления вкладок не ссылаются
 * друг на друга: о событиях они сообщают посреднику, а он решает, какие ещё вкладки
 * должны отреагировать.</p>
 */
public interface PlannerMediator {

    void notify(PlannerColleague sender, PlannerEvent event);
}
