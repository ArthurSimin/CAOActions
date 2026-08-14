package com.sockywocky.createaddonorganizer.client;

import com.sockywocky.createaddonorganizer.Config;

public final class SmoothScroll {

    private static final double SMOOTHNESS = 0.5d;
    private static final double TICKS_PER_SECOND = 20d;

    private double target;
    private double displayed;
    private long lastNanos = -1L;

    public void jumpTo(double value) {
        target = value;
        displayed = value;
        lastNanos = -1L;
    }

    public void setTarget(double value) {
        target = value;
    }

    public double target() {
        return target;
    }

    public double displayed() {
        return displayed;
    }

    public double advance() {
        long now = System.nanoTime();
        if (!Config.animOn(Config.ANIM_SMOOTH_SCROLL)) {
            displayed = target;
            lastNanos = now;
            return displayed;
        }
        if (lastNanos < 0L) {
            lastNanos = now;
        }
        double dtSeconds = (now - lastNanos) / 1_000_000_000d;
        lastNanos = now;
        double decay = Math.pow(SMOOTHNESS, dtSeconds * TICKS_PER_SECOND);
        displayed = (displayed - target) * decay + target;
        return displayed;
    }

    public boolean isSettled() {
        return Math.abs(displayed - target) < 0.05d;
    }
}
