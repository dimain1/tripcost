package ru.tripcost.model.command;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Инициатор (Invoker) паттерна «Команда»: выполняет команды и хранит историю.
 *
 * <p>Выполненные команды лежат в стеке отмены, отменённые - в стеке повтора.
 * Новая команда очищает стек повтора.</p>
 */
public class CommandHistory {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear();
    }

    public void undo() {
        if (!canUndo()) {
            throw new IllegalStateException("Нечего отменять");
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() {
        if (!canRedo()) {
            throw new IllegalStateException("Нечего повторять");
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public String undoDescription() {
        return canUndo() ? undoStack.peek().description() : "";
    }

    public String redoDescription() {
        return canRedo() ? redoStack.peek().description() : "";
    }

    /** Выполненные команды, от первой к последней. */
    public List<String> executed() {
        List<String> list = new ArrayList<>();
        undoStack.descendingIterator().forEachRemaining(c -> list.add(c.description()));
        return list;
    }

    /** Отменённые команды, которые можно повторить (ближайшая первой). */
    public List<String> undone() {
        List<String> list = new ArrayList<>();
        redoStack.forEach(c -> list.add(c.description()));
        return list;
    }
}
