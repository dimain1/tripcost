package ru.tripcost.model.approval;

import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Первое звено: проверка политики поездок. Нарушение политики останавливает
 * запрос (отказ), иначе запрос уходит дальше - к тем, кто согласует сумму.
 */
public class TravelPolicyHandler extends ApprovalHandler {

    private final double taxiMaxKm;
    private final double planeMinKm;
    private final BigDecimal hotelMaxPerNight;

    public TravelPolicyHandler(double taxiMaxKm, double planeMinKm, BigDecimal hotelMaxPerNight) {
        this.taxiMaxKm = taxiMaxKm;
        this.planeMinKm = planeMinKm;
        this.hotelMaxPerNight = hotelMaxPerNight;
    }

    @Override
    protected ApprovalDecision handle(ApprovalRequest request, List<String> trace) {
        List<String> violations = violations(request.trip());
        if (!violations.isEmpty()) {
            return reject(trace, "Нарушение политики поездок: " + String.join("; ", violations));
        }
        return passToNext(request, trace, "нарушений нет");
    }

    private List<String> violations(Trip trip) {
        List<String> list = new ArrayList<>();
        if (trip.getTransport() == TransportType.TAXI && trip.getDistanceKm() > taxiMaxKm) {
            list.add("такси - не дальше " + Math.round(taxiMaxKm) + " км");
        }
        if (trip.getTransport() == TransportType.PLANE && trip.getDistanceKm() < planeMinKm) {
            list.add("самолёт - только на расстояние от " + Math.round(planeMinKm) + " км");
        }
        if (trip.getNights() > 0 && trip.getPricePerNight().compareTo(hotelMaxPerNight) > 0) {
            list.add("проживание - не дороже " + hotelMaxPerNight.toPlainString() + " ₽ за ночь");
        }
        return list;
    }

    @Override
    public String name() {
        return "Политика поездок";
    }
}
