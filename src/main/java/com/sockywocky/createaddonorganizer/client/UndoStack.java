package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayDeque;
import java.util.Deque;

public final class UndoStack<T> {

    private static final int LIMIT = 64;

    private final Deque<T> past = new ArrayDeque<>();
    private final Deque<T> future = new ArrayDeque<>();

    public void push(T previous) {
        past.push(previous);
        while (past.size() > LIMIT) {
            past.removeLast();
        }
        future.clear();
    }

    public boolean canUndo() {
        return !past.isEmpty();
    }

    public boolean canRedo() {
        return !future.isEmpty();
    }

    public T undo(T current) {
        future.push(current);
        return past.pop();
    }

    public T redo(T current) {
        past.push(current);
        return future.pop();
    }

    public void clear() {
        past.clear();
        future.clear();
    }
}
