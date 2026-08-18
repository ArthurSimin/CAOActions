package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

final class TitleShine {

    private static final long PERIOD_MS = 5600L;
    private static final long SWEEP_MS = 950L;
    private static final int HALF_BAND = 9;
    private static final int SLICE = 3;
    private static final float PEAK = 0.85f;

    private TitleShine() {}

    static void draw(GuiGraphics g, Font font, List<FormattedCharSequence> lines, int x, int y, int lineGap,
            int color, boolean shadow) {
        int textY = y;
        int widest = 0;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, x, textY, color, shadow);
            widest = Math.max(widest, font.width(line));
            textY += lineGap;
        }
        if (lines.isEmpty() || !Config.animOn(Config.ANIM_TITLE_GLINT)) {
            return;
        }

        long phase = System.currentTimeMillis() % PERIOD_MS;
        if (phase >= SWEEP_MS) {
            return;
        }
        float travel = phase / (float) SWEEP_MS;
        int center = x - HALF_BAND + Math.round(travel * (widest + HALF_BAND * 2));
        int bottom = y + lines.size() * lineGap;

        for (int offset = -HALF_BAND; offset < HALF_BAND; offset += SLICE) {
            float falloff = 1f - Math.abs(offset + SLICE / 2f) / HALF_BAND;
            if (falloff <= 0f) {
                continue;
            }
            int lit = MenuSkin.mixColor(color, 0xFFFFFFFF, Mth.clamp(falloff * falloff * PEAK, 0f, 1f));
            int sliceX = center + offset;
            if (sliceX + SLICE <= x || sliceX >= x + widest) {
                continue;
            }
            g.enableScissor(Math.max(x, sliceX), y - 1, Math.min(x + widest, sliceX + SLICE), bottom);
            int lineY = y;
            for (FormattedCharSequence line : lines) {
                g.drawString(font, line, x, lineY, lit, shadow);
                lineY += lineGap;
            }
            g.disableScissor();
        }
    }
}
