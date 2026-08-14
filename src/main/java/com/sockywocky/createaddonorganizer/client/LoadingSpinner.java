package com.sockywocky.createaddonorganizer.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class LoadingSpinner {

    public static final int SQUARES = 8;
    private static final long STEP_MS = 90L;
    private static final int RING = 16;
    private static final int SIZE = 10;
    private static final int LEAD_SIZE = SIZE + 4;

    private static final int[] OFFSET_X = {-1, 0, 1, 1, 1, 0, -1, -1};
    private static final int[] OFFSET_Y = {-1, -1, -1, 0, 1, 1, 1, 0};

    private static final float PREVIEW_SCALE = 3f;
    private static final int PREVIEW_PAD = 12;

    private static boolean previewOn;
    private static boolean previewComboDown;
    private static long previewStart;

    private LoadingSpinner() {}

    public static int labelOffset() {
        return RING + LEAD_SIZE / 2 + 12;
    }

    public static long cycleMs() {
        return STEP_MS * SQUARES;
    }

    public static void render(GuiGraphics g, int centerX, int centerY, long elapsedMs, int tint) {
        float lead = Math.floorMod(elapsedMs, STEP_MS * SQUARES) / (float) STEP_MS;
        for (int i = 0; i < SQUARES; i++) {
            float behind = lead - i;
            behind -= Mth.floor(behind / SQUARES) * SQUARES;

            float fade = 1f - behind / SQUARES;
            int alpha = Mth.clamp(Math.round(fade * fade * 255f), 26, 255);

            int size = behind < 1f ? LEAD_SIZE : SIZE;
            int x = centerX + OFFSET_X[i] * RING - size / 2;
            int y = centerY + OFFSET_Y[i] * RING - size / 2;
            g.fill(x, y, x + size, y + size, (alpha << 24) | (tint & 0x00FFFFFF));
        }
    }

    public static void renderCentered(GuiGraphics g, int x, int y, int width, int height, long elapsedMs) {
        render(g, x + width / 2, y + height / 2, elapsedMs, ArrangerSkin.current().title());
    }

    public static void tickPreview(Minecraft mc) {
        if (mc.screen == null || !DevMode.isUnlocked()) {
            previewOn = false;
            previewComboDown = false;
            return;
        }
        long window = mc.getWindow().getWindow();
        boolean combo = (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT))
                && InputConstants.isKeyDown(window, GLFW.GLFW_KEY_L);
        if (combo && !previewComboDown) {
            previewOn = !previewOn;
            previewStart = System.currentTimeMillis();
        }
        previewComboDown = combo;
    }

    public static void renderPreview(GuiGraphics g, Minecraft mc) {
        if (!previewOn) {
            return;
        }
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;
        int half = Math.round((RING + LEAD_SIZE / 2f) * PREVIEW_SCALE) + PREVIEW_PAD;

        g.fill(centerX - half, centerY - half, centerX + half, centerY + half, 0xE60D1014);
        g.renderOutline(centerX - half, centerY - half, half * 2, half * 2, 0x40FFFFFF);

        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0);
        g.pose().scale(PREVIEW_SCALE, PREVIEW_SCALE, 1f);
        render(g, 0, 0, System.currentTimeMillis() - previewStart, ArrangerSkin.current().title());
        g.pose().popPose();

        g.drawCenteredString(mc.font, Component.translatable("createaddonorganizer.devmode.spinnerPreview"),
                centerX, centerY + half + 6, 0xFF8A9AA8);
    }
}
