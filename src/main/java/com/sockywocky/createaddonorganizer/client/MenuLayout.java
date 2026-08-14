package com.sockywocky.createaddonorganizer.client;

public final class MenuLayout {
    public static final int PANEL_W = 400;
    public static final int SIDE_MARGIN = 30;
    public static final int ROW_H = 18;
    public static final int GAP = 4;

    public static final int TITLE_Y = 16;
    public static final int DESC_Y = 34;
    public static final int ROW_1 = 54;

    private MenuLayout() {}

    public static int panelWidth(int screenWidth) {
        return Math.max(120, Math.min(PANEL_W, screenWidth - SIDE_MARGIN * 2));
    }

    public static int panelX(int screenWidth) {
        return (screenWidth - panelWidth(screenWidth)) / 2;
    }

    public static int nextRow(int y) {
        return y + ROW_H + GAP;
    }

    public static int doneY(int screenHeight) {
        return screenHeight - ROW_H - 4;
    }

    public static int listBottom(int screenHeight, int footerRows) {
        return doneY(screenHeight) - footerRows * (ROW_H + GAP) - GAP;
    }

    public static int split(int panelWidth, int parts) {
        int count = Math.max(1, parts);
        return Math.max(1, (panelWidth - GAP * (count - 1)) / count);
    }

    public static int splitX(int panelX, int panelWidth, int parts, int index) {
        int count = Math.max(1, parts);
        int each = split(panelWidth, count);
        return index >= count - 1 ? panelX + panelWidth - each : panelX + (each + GAP) * index;
    }
}
