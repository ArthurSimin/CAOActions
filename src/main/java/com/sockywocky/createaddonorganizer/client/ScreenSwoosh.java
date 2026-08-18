package com.sockywocky.createaddonorganizer.client;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.ModConfigSpec;

import com.sockywocky.createaddonorganizer.Config;

public final class ScreenSwoosh {

    public static final int FORWARD = 1;
    public static final int BACK = -1;

    private static final String OWN_PACKAGE = "com.sockywocky.createaddonorganizer.";
    private static final long ARM_GRACE_MS = 750;

    private enum Phase { IDLE, EXIT, ENTER_ARMED, ENTER }

    private static Phase phase = Phase.IDLE;
    private static long started;
    private static int direction = FORWARD;
    private static boolean vertical;
    private static Supplier<Screen> pending;
    private static boolean swapScheduled;
    private static boolean fadeOnly;

    private ScreenSwoosh() {
    }

    public static boolean busy() {
        return phase != Phase.IDLE;
    }

    public static void push(Supplier<Screen> next, ModConfigSpec.BooleanValue route) {
        start(next, FORWARD, false, route);
    }

    public static void pull(Supplier<Screen> next, ModConfigSpec.BooleanValue route) {
        start(next, BACK, false, route);
    }

    public static void drill(Supplier<Screen> next, ModConfigSpec.BooleanValue route) {
        start(next, depthSign() * FORWARD, depthVertical(), route);
    }

    public static void surface(Supplier<Screen> next, ModConfigSpec.BooleanValue route) {
        start(next, depthSign() * BACK, depthVertical(), route);
    }

    public static boolean depthVertical() {
        return Config.swooshDepthDirection() != Config.SwooshDirection.SIDEWAYS;
    }

    private static int depthSign() {
        return Config.swooshDepthDirection() == Config.SwooshDirection.DOWN ? -1 : 1;
    }

    public static void reveal(ModConfigSpec.BooleanValue route) {
        if (busy() || !Config.swooshOn(route)) {
            return;
        }
        direction = depthSign() * FORWARD;
        vertical = depthVertical();
        fadeOnly = true;
        pending = null;
        swapScheduled = true;
        phase = Phase.ENTER_ARMED;
        started = System.currentTimeMillis();
    }

    private static void start(Supplier<Screen> next, int dir, boolean onYAxis,
            ModConfigSpec.BooleanValue route) {
        if (next == null) {
            return;
        }
        if (busy()) {
            return;
        }
        if (route == null || !Config.swooshOn(route)) {
            Minecraft.getInstance().setScreen(next.get());
            return;
        }
        direction = dir;
        vertical = onYAxis;
        fadeOnly = false;
        pending = next;
        swapScheduled = false;
        phase = Phase.EXIT;
        started = System.currentTimeMillis();
    }

    public static void update() {
        if (phase == Phase.IDLE) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - started;
        if (phase == Phase.EXIT) {
            if (!swapScheduled && elapsed >= Config.swooshOutMs() + Config.swooshHoldMs()) {
                swapScheduled = true;
                Supplier<Screen> next = pending;
                pending = null;
                Minecraft.getInstance().execute(() -> {
                    phase = Phase.ENTER_ARMED;
                    started = System.currentTimeMillis();
                    Minecraft.getInstance().setScreen(next.get());
                });
            }
        } else if (phase == Phase.ENTER_ARMED) {
            if (ours(Minecraft.getInstance().screen)) {
                phase = Phase.ENTER;
                started = now;
            } else if (elapsed >= ARM_GRACE_MS) {
                phase = Phase.IDLE;
            }
        } else if (elapsed >= Config.swooshInMs()) {
            phase = Phase.IDLE;
        }
    }

    public static boolean appliesTo(Screen screen) {
        return (phase == Phase.EXIT || phase == Phase.ENTER) && ours(screen);
    }

    private static boolean ours(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith(OWN_PACKAGE);
    }

    private static boolean backgroundPass;

    public static void setBackgroundPass(boolean value) {
        backgroundPass = value;
    }

    public static float fadeMultiplier() {
        if (phase == Phase.IDLE || backgroundPass || !Config.swooshFade()) {
            return 1f;
        }
        if (phase == Phase.ENTER_ARMED) {
            return ours(Minecraft.getInstance().screen) ? 0f : 1f;
        }
        return appliesTo(Minecraft.getInstance().screen) ? opacity() : 1f;
    }

    public static float offsetX() {
        return vertical ? 0f : travelOffset();
    }

    public static float offsetY() {
        return vertical ? travelOffset() : 0f;
    }

    private static float travelOffset() {
        if (fadeOnly || (phase != Phase.EXIT && phase != Phase.ENTER)) {
            return 0f;
        }
        float travel = Config.swooshTravel();
        long elapsed = System.currentTimeMillis() - started;
        if (phase == Phase.EXIT) {
            float t = progress(elapsed, Config.swooshOutMs());
            return -direction * travel * exitCurve(t);
        }
        float t = progress(elapsed, Config.swooshInMs());
        return direction * travel * (1f - enterCurve(t));
    }

    public static float opacity() {
        if (phase == Phase.ENTER_ARMED) {
            return 0f;
        }
        if (phase == Phase.IDLE || !Config.swooshFade()) {
            return 1f;
        }
        long elapsed = System.currentTimeMillis() - started;
        if (phase == Phase.EXIT) {
            return 1f - progress(elapsed, Config.swooshOutMs());
        }
        return progress(elapsed, Math.max(1, Math.round(Config.swooshInMs() * 0.8f)));
    }

    public static long previewCycleMs() {
        return Config.swooshOutMs() + Config.swooshHoldMs() + Config.swooshInMs();
    }

    public static float previewOffsetX(long elapsed, float travel) {
        int out = Config.swooshOutMs();
        int hold = Config.swooshHoldMs();
        if (elapsed < out) {
            return -travel * exitCurve(progress(elapsed, out));
        }
        if (elapsed < out + hold) {
            return -travel;
        }
        return travel * (1f - enterCurve(progress(elapsed - out - hold, Config.swooshInMs())));
    }

    public static float previewOpacity(long elapsed) {
        if (!Config.swooshFade()) {
            return 1f;
        }
        int out = Config.swooshOutMs();
        int hold = Config.swooshHoldMs();
        if (elapsed < out) {
            return 1f - progress(elapsed, out);
        }
        if (elapsed < out + hold) {
            return 0f;
        }
        return progress(elapsed - out - hold, Math.max(1, Math.round(Config.swooshInMs() * 0.8f)));
    }

    private static float progress(long elapsed, long span) {
        return Mth.clamp(elapsed / (float) Math.max(1L, span), 0f, 1f);
    }

    private static float exitCurve(float t) {
        return switch (Config.swooshExitCurve()) {
            case ACCELERATE -> t * t * t;
            case SMOOTH -> t * t * (3f - 2f * t);
            case LINEAR -> t;
            case WINDUP -> t * t * (2.70158f * t - 1.70158f);
        };
    }

    private static float enterCurve(float t) {
        float inv = 1f - t;
        return switch (Config.swooshEnterCurve()) {
            case DECELERATE -> 1f - inv * inv * inv;
            case OVERSHOOT -> 1f - 1.8f * inv * inv * inv + 0.8f * inv * inv;
            case SPRING -> 1f - 2.70158f * inv * inv * inv + 1.70158f * inv * inv;
            case LINEAR -> t;
        };
    }
}
