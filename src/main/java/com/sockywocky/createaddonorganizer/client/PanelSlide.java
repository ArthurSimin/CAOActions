package com.sockywocky.createaddonorganizer.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class PanelSlide {

    private static final long MS = 210L;
    private static final float TRAVEL = 16f;

    private long start;
    private int direction;
    private boolean open;

    void play(int fromIndex, int toIndex) {
        play(Integer.compare(toIndex, fromIndex));
    }

    void play(int towards) {
        if (!Config.animOn(Config.ANIM_MENU_ENTRANCE)) {
            start = 0L;
            return;
        }
        direction = towards;
        start = System.currentTimeMillis();
    }

    boolean running() {
        return start != 0L && System.currentTimeMillis() - start < MS;
    }

    float progress() {
        if (start == 0L) {
            return 1f;
        }
        float t = Mth.clamp((System.currentTimeMillis() - start) / (float) MS, 0f, 1f);
        if (t >= 1f) {
            start = 0L;
            return 1f;
        }
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    float offset() {
        return direction * TRAVEL * (1f - progress());
    }

    void begin(GuiGraphics g) {
        if (!running()) {
            return;
        }
        float eased = progress();
        open = true;
        g.pose().pushPose();
        g.pose().translate(0f, direction * TRAVEL * (1f - eased), 0f);
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, eased * ambient());
    }

    void end(GuiGraphics g) {
        if (!open) {
            return;
        }
        open = false;
        g.flush();
        RenderSystem.setShaderColor(1f, 1f, 1f, ambient());
        g.pose().popPose();
    }

    private static float ambient() {
        return ScreenSwoosh.appliesTo(Minecraft.getInstance().screen) ? ScreenSwoosh.opacity() : 1f;
    }
}
