package ru.tripcost.model.request;

import ru.tripcost.model.approval.ApprovalDecision;
import ru.tripcost.model.booking.SagaResult;

import java.util.EnumSet;
import java.util.Set;

import static ru.tripcost.model.request.RequestAction.APPROVE;
import static ru.tripcost.model.request.RequestAction.CANCEL;
import static ru.tripcost.model.request.RequestAction.COMPLETE;
import static ru.tripcost.model.request.RequestAction.PAY;
import static ru.tripcost.model.request.RequestAction.REJECT;
import static ru.tripcost.model.request.RequestAction.REVISE;
import static ru.tripcost.model.request.RequestAction.SUBMIT;

/**
 * Конкретные состояния заявки. Состояния не хранят данных заявки, поэтому
 * каждое существует в единственном экземпляре и общее для всех заявок.
 *
 * <p>Переходы: Черновик - отправить - На согласовании (или сразу Согласована при
 * автосогласовании, Отклонена при нарушении политики); На согласовании - согласовать -
 * Согласована, отклонить - Отклонена; Согласована - оплатить - Оплачена - завершить -
 * Завершена; Отклонена - вернуть в черновик - Черновик; отменить можно черновик, заявку
 * на согласовании и согласованную заявку.</p>
 */
public final class RequestStates {

    public static final RequestState DRAFT = new Draft();
    public static final RequestState PENDING = new Pending();
    public static final RequestState APPROVED = new Approved();
    public static final RequestState REJECTED = new Rejected();
    public static final RequestState PAID = new Paid();
    public static final RequestState COMPLETED = new Completed();
    public static final RequestState CANCELLED = new Cancelled();

    private RequestStates() {
    }

    /** Черновик: заявку можно отправить на согласование или отменить. */
    static final class Draft extends AbstractRequestState {
        @Override
        public String name() {
            return "Черновик";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.of(SUBMIT, CANCEL);
        }

        /** Запрос проходит цепочку согласования; результат определяет следующее состояние. */
        @Override
        public void submit(TripRequest request) {
            ApprovalDecision decision = request.runApproval();
            if (!decision.approved()) {
                request.changeState(REJECTED, decision.reason());
            } else if (decision.handler().startsWith("Автосогласование")) {
                request.changeState(APPROVED, "Согласована автоматически: " + decision.reason());
            } else {
                request.changeState(PENDING, "Ожидает решения: " + decision.handler());
            }
        }

        @Override
        public void cancel(TripRequest request) {
            request.changeState(CANCELLED, "Заявка отменена сотрудником");
        }
    }

    /** На согласовании у руководителя, директора или финансового комитета. */
    static final class Pending extends AbstractRequestState {
        @Override
        public String name() {
            return "На согласовании";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.of(APPROVE, REJECT, CANCEL);
        }

        @Override
        public void approve(TripRequest request) {
            request.changeState(APPROVED, "Согласовано: " + request.approver());
        }

        @Override
        public void reject(TripRequest request, String reason) {
            request.changeState(REJECTED, (reason == null || reason.isBlank())
                    ? "Отклонено: " + request.approver() : reason.trim());
        }

        @Override
        public void cancel(TripRequest request) {
            request.changeState(CANCELLED, "Заявка отозвана с согласования");
        }
    }

    /** Согласована: бухгалтерия может оплатить билеты и проживание. */
    static final class Approved extends AbstractRequestState {
        @Override
        public String name() {
            return "Согласована";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.of(PAY, CANCEL);
        }

        /**
         * Оплата - сага бронирования: билеты, гостиница, страховка, списание с бюджета.
         * При сбое сага отменяет выполненные шаги, заявка остаётся согласованной.
         */
        @Override
        public void pay(TripRequest request) {
            SagaResult result = request.runBooking();
            if (result.success()) {
                request.changeState(PAID, "Оплачено и забронировано: " + result.summary());
            } else {
                request.changeState(APPROVED, "Бронирование отменено - " + result.failure());
            }
        }

        @Override
        public void cancel(TripRequest request) {
            request.changeState(CANCELLED, "Поездка отменена после согласования");
        }
    }

    /** Отклонена: заявку можно вернуть в черновик и отправить снова. */
    static final class Rejected extends AbstractRequestState {
        @Override
        public String name() {
            return "Отклонена";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.of(REVISE);
        }

        @Override
        public void revise(TripRequest request) {
            request.changeState(DRAFT, "Заявка возвращена в черновик для доработки");
        }
    }

    /** Оплачена: после поездки заявку закрывают. */
    static final class Paid extends AbstractRequestState {
        @Override
        public String name() {
            return "Оплачена";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.of(COMPLETE);
        }

        @Override
        public void complete(TripRequest request) {
            request.changeState(COMPLETED, "Поездка состоялась, заявка закрыта");
        }
    }

    /** Завершена - конечное состояние. */
    static final class Completed extends AbstractRequestState {
        @Override
        public String name() {
            return "Завершена";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.noneOf(RequestAction.class);
        }
    }

    /** Отменена - конечное состояние. */
    static final class Cancelled extends AbstractRequestState {
        @Override
        public String name() {
            return "Отменена";
        }

        @Override
        public Set<RequestAction> allowedActions() {
            return EnumSet.noneOf(RequestAction.class);
        }
    }
}
