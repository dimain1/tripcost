package ru.tripcost.viewmodel;

import ru.tripcost.viewmodel.PlannerEvent.AddLegRequested;
import ru.tripcost.viewmodel.PlannerEvent.CreateRequestRequested;
import ru.tripcost.viewmodel.PlannerEvent.RequestChanged;
import ru.tripcost.viewmodel.PlannerEvent.JourneyChanged;
import ru.tripcost.viewmodel.PlannerEvent.OpenInCalculatorRequested;

/**
 * Конкретный посредник: знает все вкладки и согласует их работу.
 *
 * <ul>
 *     <li>«В маршрут» на вкладках «Расчёт» и «Направления» - участок добавляется
 *     в маршрут, открывается вкладка «Маршрут»;</li>
 *     <li>«Открыть в расчёте» на вкладке «Маршрут» - участок рассчитывается
 *     на вкладке «Расчёт»;</li>
 *     <li>«Оформить заявку» на вкладке «Расчёт» - создаётся заявка на командировку,
 *     открывается вкладка «Заявки»;</li>
 *     <li>о каждом событии сообщается в строке состояния главного окна.</li>
 * </ul>
 */
public class TripPlannerMediator implements PlannerMediator {

    private final MainViewModel main;
    private final TripCalculatorViewModel calculator;
    private final JourneyViewModel journey;
    /** Вкладка «Направления» только отправляет события, поэтому ссылка на неё нужна лишь для регистрации. */
    private final DirectionsViewModel directions;
    private final RequestsViewModel requests;

    public TripPlannerMediator(MainViewModel main, TripCalculatorViewModel calculator,
                               JourneyViewModel journey, DirectionsViewModel directions,
                               RequestsViewModel requests) {
        this.main = main;
        this.calculator = calculator;
        this.journey = journey;
        this.directions = directions;
        this.requests = requests;
        calculator.setMediator(this);
        journey.setMediator(this);
        directions.setMediator(this);
        requests.setMediator(this);
    }

    @Override
    public void notify(PlannerColleague sender, PlannerEvent event) {
        if (event instanceof AddLegRequested e) {
            if (journey.addLegFrom(e.from(), e.to(), e.transport())) {
                main.selectTab(AppTab.JOURNEY);
                main.showStatus("Вкладка «" + e.source() + "»: участок " + e.from() + " → " + e.to()
                        + " (" + e.transport() + ") добавлен в маршрут");
            } else {
                main.showStatus("Не удалось добавить участок в маршрут");
            }
        } else if (event instanceof OpenInCalculatorRequested e) {
            calculator.openTrip(e.from(), e.to(), e.transport(), e.passengers());
            main.selectTab(AppTab.CALCULATOR);
            main.showStatus("Вкладка «Маршрут»: участок " + e.from() + " → " + e.to() + " открыт в расчёте");
        } else if (event instanceof CreateRequestRequested e) {
            var request = requests.createRequest(e.trip(), e.cost());
            main.selectTab(AppTab.REQUESTS);
            main.showStatus("Вкладка «Расчёт»: создана заявка №" + request.id() + " (черновик)");
        } else if (event instanceof RequestChanged e) {
            main.showStatus(e.description());
        } else if (event instanceof JourneyChanged e) {
            main.showStatus(e.legs() == 0 ? "Маршрут пуст"
                    : "Маршрут: участков " + e.legs() + ", итого " + e.total());
        }
    }
}
