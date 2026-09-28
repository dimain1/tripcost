/**
 * Параллельные вычисления.
 *
 * <p>Паттерн «Future / Promise»: долгий расчёт запускается в фоне и сразу возвращает
 * обещание результата ({@link java.util.concurrent.CompletableFuture}); интерфейс не ждёт,
 * а подписывается на завершение. Задачи выполняет пул потоков
 * {@link ru.tripcost.model.concurrent.QuotePool}.</p>
 */
package ru.tripcost.model.concurrent;
