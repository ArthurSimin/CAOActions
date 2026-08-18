package com.sockywocky.createaddonorganizer.client;

import java.util.Locale;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class FpsMonitor {

    private static final int SAMPLES = 100;
    private static final int PAD = 4;
    private static final int GRAPH_H = 16;
    private static final int LINE_H = 9;
    private static final int GAP = 3;
    private static final int PANEL_W = SAMPLES + PAD * 2;
    private static final int PANEL_H = PAD * 2 + LINE_H * 5 + GRAPH_H + GAP * 3;

    private static final int UNCAPPED = 260;
    private static final int TARGET_FPS = 120;
    private static final float MIN_DELTA_MS = 0.05f;

    private static final int BG = 0xD00E1116;
    private static final int FRAME = (0x70 << 24) | (GlassSkin.DEFAULT_ACCENT & 0x00FFFFFF);
    private static final int GRAPH_BG = 0x50000000;
    private static final int TARGET_LINE = 0x40FFFFFF;
    private static final int LABEL = 0xFF7C848D;

    private static final int GOOD = 0xFF7BD88F;
    private static final int FAIR = 0xFFE0C766;
    private static final int POOR = 0xFFE09A5A;
    private static final int BAD = 0xFFE06A62;

    private static final float[] frameMs = new float[SAMPLES];
    private static int cursor;
    private static int filled;
    private static long lastNanos;

    private FpsMonitor() {}

    public static void render(GuiGraphics g, Minecraft mc) {
        if (!DevMode.isUnlocked() || !Config.devFpsDisplay()) {
            RenderProfiler.setActive(false);
            lastNanos = 0L;
            filled = 0;
            cursor = 0;
            return;
        }
        RenderProfiler.setActive(true);
        sample();
        if (filled == 0) {
            return;
        }

        float total = 0f;
        float worst = 0f;
        for (int i = 0; i < filled; i++) {
            float ms = frameMs[i];
            total += ms;
            worst = Math.max(worst, ms);
        }
        float avgMs = total / filled;
        float lastMs = frameMs[(cursor - 1 + SAMPLES) % SAMPLES];

        int cap = effectiveLimit(mc);
        int reference = Math.min(cap, TARGET_FPS);
        float targetMs = 1000f / reference;
        int avgFps = Math.round(1000f / Math.max(0.01f, avgMs));
        int lowFps = Math.round(1000f / Math.max(0.01f, worst));

        Font font = mc.font;
        int x = mc.getWindow().getGuiScaledWidth() - PANEL_W - PAD;
        int y = PAD;

        g.pose().pushPose();
        g.pose().translate(0f, 0f, 400f);

        g.fill(x, y, x + PANEL_W, y + PANEL_H, BG);
        g.fill(x, y, x + PANEL_W, y + 1, MenuSkin.accent(FRAME));
        g.fill(x, y + PANEL_H - 1, x + PANEL_W, y + PANEL_H, MenuSkin.accent(FRAME));
        g.fill(x, y, x + 1, y + PANEL_H, MenuSkin.accent(FRAME));
        g.fill(x + PANEL_W - 1, y, x + PANEL_W, y + PANEL_H, MenuSkin.accent(FRAME));

        int textX = x + PAD;
        int textRight = x + PANEL_W - PAD;
        int lineY = y + PAD;

        String fpsText = avgFps + " fps";
        String msText = String.format(Locale.ROOT, "%.1f ms", lastMs);
        g.drawString(font, fpsText, textX, lineY, gradeColor(avgFps, reference));
        g.drawString(font, msText, textRight - font.width(msText), lineY, LABEL);

        int graphY = lineY + LINE_H + GAP;
        drawGraph(g, textX, graphY, targetMs);

        int footY = graphY + GRAPH_H + GAP;
        String lowText = "low " + lowFps;
        String capText = cap >= UNCAPPED ? "cap off" : "cap " + cap;
        g.drawString(font, lowText, textX, footY, gradeColor(lowFps, reference));
        g.drawString(font, capText, textRight - font.width(capText), footY, LABEL);

        int rowY = footY + LINE_H + GAP;
        float measured = 0f;
        for (int i = 0; i < RenderProfiler.COUNT; i++) {
            if (i != RenderProfiler.ITEM) {
                measured += RenderProfiler.ms(i);
            }
            if ((i & 1) == 0) {
                g.drawString(font, section(i), textX, rowY, LABEL);
            } else {
                String s = section(i);
                g.drawString(font, s, textRight - font.width(s), rowY, LABEL);
                rowY += LINE_H;
            }
        }
        String rest = String.format(Locale.ROOT, "rest %.1f", Math.max(0f, lastMs - measured));
        g.drawString(font, rest, textX, rowY, LABEL);

        g.pose().popPose();
        RenderProfiler.frame();
    }

    private static String section(int id) {
        return String.format(Locale.ROOT, "%s %.1f", RenderProfiler.label(id), RenderProfiler.ms(id));
    }

    private static void drawGraph(GuiGraphics g, int x, int y, float targetMs) {
        g.fill(x, y, x + SAMPLES, y + GRAPH_H, GRAPH_BG);
        float scaleMs = targetMs * 2f;
        for (int i = 0; i < filled; i++) {
            float ms = frameMs[(cursor - filled + i + SAMPLES) % SAMPLES];
            int h = Math.max(1, Math.round(Mth.clamp(ms / scaleMs, 0f, 1f) * GRAPH_H));
            int bx = x + SAMPLES - filled + i;
            int color = ms <= targetMs * 1.15f ? GOOD : ms <= targetMs * 2f ? FAIR : BAD;
            g.fill(bx, y + GRAPH_H - h, bx + 1, y + GRAPH_H, color);
        }
        int targetY = y + GRAPH_H - Math.round(GRAPH_H * 0.5f);
        g.fill(x, targetY, x + SAMPLES, targetY + 1, TARGET_LINE);
    }

    private static void sample() {
        long now = System.nanoTime();
        if (lastNanos == 0L) {
            lastNanos = now;
            return;
        }
        float ms = (now - lastNanos) / 1_000_000f;
        if (ms < MIN_DELTA_MS) {
            return;
        }
        lastNanos = now;
        frameMs[cursor] = ms;
        cursor = (cursor + 1) % SAMPLES;
        filled = Math.min(filled + 1, SAMPLES);
    }

    private static int effectiveLimit(Minecraft mc) {
        int windowLimit = mc.getWindow().getFramerateLimit();
        if (mc.level == null && (mc.screen != null || mc.getOverlay() != null)) {
            return Math.min(Math.max(60, Config.menuFramerate()), windowLimit);
        }
        return windowLimit;
    }

    private static int gradeColor(int fps, int reference) {
        float ratio = fps / (float) Math.max(1, reference);
        if (ratio >= 0.9f) {
            return GOOD;
        }
        if (ratio >= 0.6f) {
            return FAIR;
        }
        return ratio >= 0.35f ? POOR : BAD;
    }
}
