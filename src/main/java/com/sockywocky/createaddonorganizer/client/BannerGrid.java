package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;

public final class BannerGrid {
    public static final int MAX_COLUMNS = 2;
    public static final int CELL_GAP = 6;
    public static final int SCROLLBAR_ALLOWANCE = 12;

    private BannerGrid() {}

    public static int columnsFor(int panelWidth) {
        int usable = panelWidth - SCROLLBAR_ALLOWANCE;
        return usable >= BannerTextures.WIDTH * MAX_COLUMNS + CELL_GAP ? MAX_COLUMNS : 1;
    }

    public static int rowWidth(int columns) {
        return BannerTextures.WIDTH * columns + CELL_GAP * (columns - 1);
    }

    public static int cellX(int rowLeft, int rowWidth, int columns, int index) {
        int span = rowWidth(columns);
        int start = rowLeft + Math.max(0, (rowWidth - span) / 2);
        return start + index * (BannerTextures.WIDTH + CELL_GAP);
    }

    public static int cellAt(double mouseX, int rowLeft, int rowWidth, int columns) {
        for (int i = 0; i < columns; i++) {
            int x = cellX(rowLeft, rowWidth, columns, i);
            if (mouseX >= x && mouseX < x + BannerTextures.WIDTH) {
                return i;
            }
        }
        return -1;
    }

    public static <T> List<List<T>> chunk(List<T> items, int columns) {
        int step = Math.max(1, columns);
        List<List<T>> rows = new ArrayList<>();
        for (int i = 0; i < items.size(); i += step) {
            rows.add(List.copyOf(items.subList(i, Math.min(items.size(), i + step))));
        }
        return rows;
    }
}
