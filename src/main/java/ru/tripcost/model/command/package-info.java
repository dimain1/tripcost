/**
 * Команды редактирования маршрута.
 *
 * <p>Паттерн «Команда» (Command): каждое действие пользователя над маршрутом - объект
 * с методами {@code execute()} и {@code undo()}; {@link ru.tripcost.model.command.CommandHistory}
 * выполняет команды и хранит историю для отмены и повтора.</p>
 */
package ru.tripcost.model.command;
