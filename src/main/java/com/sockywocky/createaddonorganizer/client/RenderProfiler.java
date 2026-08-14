package com.sockywocky.createaddonorganizer.client;

public final class RenderProfiler {

    public static final int BG = 0;
    public static final int LIB = 1;
    public static final int GRID = 2;
    public static final int ITEM = 3;
    public static final int COUNT = 4;

    private static final String[] LABELS = {"bg", "lib", "grid", "item"};
    private static final float SMOOTHING = 0.85f;

    private static final long[] start = new long[COUNT];
    private static final int[] depth = new int[COUNT];
    private static final long[] accum = new long[COUNT];
    private static final float[] shown = new float[COUNT];

    private static boolean active;

    private RenderProfiler() {}

    public static void setActive(boolean value) {
        if (!value && active) {
            for (int i = 0; i < COUNT; i++) {
                accum[i] = 0L;
                shown[i] = 0f;
                depth[i] = 0;
            }
        }
        active = value;
    }

    public static void begin(int id) {
        if (active && depth[id]++ == 0) {
            start[id] = System.nanoTime();
        }
    }

    public static void end(int id) {
        if (active && depth[id] > 0 && --depth[id] == 0) {
            accum[id] += System.nanoTime() - start[id];
        }
    }

    public static void frame() {
        if (!active) {
            return;
        }
        for (int i = 0; i < COUNT; i++) {
            shown[i] = shown[i] * SMOOTHING + accum[i] / 1_000_000f * (1f - SMOOTHING);
            accum[i] = 0L;
            depth[i] = 0;
        }
    }

    public static float ms(int id) {
        return shown[id];
    }

    public static String label(int id) {
        return LABELS[id];
    }
}
