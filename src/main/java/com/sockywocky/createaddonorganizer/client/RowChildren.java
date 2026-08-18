package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class RowChildren {

    private static final List<?> EMPTY = Collections.unmodifiableList(new ArrayList<>(0));

    private RowChildren() {}

    @SuppressWarnings("unchecked")
    static <T> List<T> none() {
        return (List<T>) EMPTY;
    }

    @SafeVarargs
    static <T> List<T> of(T... items) {
        List<T> out = new ArrayList<>(items.length);
        for (T item : items) {
            if (item != null) {
                out.add(item);
            }
        }
        return out.isEmpty() ? none() : Collections.unmodifiableList(out);
    }
}
