package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.SectionCatalog;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class SlidePreview {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 96;

    private static final long LOOP_PAUSE_MS = 700;

    private final List<Integer> rowTextColors = new ArrayList<>();
    private int bandColor = 0x3D5C2E;
    private long cycleStarted = System.currentTimeMillis();

    public void sample() {
        rowTextColors.clear();
        ResourceLocation hub = null;
        for (SectionCatalog.Entry entry : SectionCatalog.colorables()) {
            if (hub == null && entry.parent()) {
                hub = entry.id();
                Integer highlight = Config.highlightColorFor(hub);
                if (highlight != null) {
                    bandColor = highlight & 0x00FFFFFF;
                }
                continue;
            }
            if (hub != null && !entry.parent() && rowTextColors.size() < 3) {
                Integer nativeColor = entry.nativeTextColor();
                int color = nativeColor != null ? nativeColor : Config.textColorFor(entry.id()).color1();
                rowTextColors.add(color & 0x00FFFFFF);
            }
        }
        while (rowTextColors.size() < 3) {
            rowTextColors.add(0xBFBFBF);
        }
    }

    public void restart() {
        cycleStarted = System.currentTimeMillis();
    }

    public long cycleMillis() {
        return ScreenSwoosh.previewCycleMs();
    }

    public void render(GuiGraphics g, int x, int y) {
        if (rowTextColors.isEmpty()) {
            sample();
        }
        long cycle = cycleMillis();
        long elapsed = System.currentTimeMillis() - cycleStarted;
        if (elapsed > cycle + LOOP_PAUSE_MS) {
            restart();
            elapsed = 0;
        }
        draw(g, x, y, Math.min(elapsed, cycle));
    }

    private void draw(GuiGraphics g, int x, int y, long elapsed) {
        int right = x + WIDTH;
        int bottom = y + HEIGHT;
        g.fill(x - 1, y - 1, right + 1, bottom + 1, 0xFF000000);
        g.fill(x, y, right, bottom, 0xFF101014);

        float scale = WIDTH / 320f;
        boolean vertical = ScreenSwoosh.depthVertical();
        float travel = Config.swooshTravel() * scale * (vertical ? HEIGHT / (float) WIDTH * 2f : 1f);
        float offset = ScreenSwoosh.previewOffsetX(elapsed, travel)
                * (Config.swooshDepthDirection() == Config.SwooshDirection.DOWN ? -1f : 1f);
        float opacity = ScreenSwoosh.previewOpacity(elapsed);
        int alpha = Math.round(Mth.clamp(opacity, 0f, 1f) * 255f);
        if (alpha <= 2) {
            return;
        }

        g.enableScissor(x, y, right, bottom);
        int left = x + 14 + (vertical ? 0 : Math.round(offset));
        int width = WIDTH - 28;
        int rowY = y + 12 + (vertical ? Math.round(offset) : 0);

        g.fill(left + width / 4, rowY, left + width - width / 4, rowY + 5, tint(0xFFFFFF, alpha));
        rowY += 11;
        for (int row = 0; row < 4; row++) {
            int face = row == 0 ? bandColor : 0x2A2A2A;
            int label = row == 0 ? 0xF4F4F4 : rowTextColors.get(row - 1);
            g.fill(left, rowY, left + width, rowY + 12, tint(face, alpha));
            g.fill(left + 4, rowY + 4, left + 4 + width / 3, rowY + 8, tint(label, alpha));
            g.fill(left + width - 26, rowY + 3, left + width - 4, rowY + 10, tint(0x555555, alpha));
            rowY += 14;
        }
        g.disableScissor();
    }

    private static int tint(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
