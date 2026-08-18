package com.sockywocky.createaddonorganizer.client;

import com.mojang.math.Axis;
import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.GuiGraphics;

final class GlassArrow {

    static final int W = 14;
    static final int H = 26;
    static final int MARGIN = 2;
    private static final int PAD = 2;

    private static final float SECONDS = 0.16f;
    private static final float DRIFT = 4f;
    private static final float REST_ALPHA = 0.35f;
    private static final float ARM = 7f;
    private static final float THICK = 2.2f;

    private float expand;

    static int top(int screenHeight) {
        return screenHeight / 2 - H / 2;
    }

    static int leftX() {
        return MARGIN;
    }

    static int rightX(int screenWidth) {
        return screenWidth - MARGIN - W;
    }

    static boolean contains(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x - PAD && mouseX < x + W + PAD && mouseY >= y - PAD && mouseY < y + H + PAD;
    }

    void render(GuiGraphics g, int x, int y, boolean right, boolean hovered, float delta) {
        float target = hovered ? 1f : 0f;
        if (!Config.animOn(Config.ANIM_BUTTON_HOVER)) {
            expand = target;
        } else {
            float step = delta / SECONDS;
            expand = expand < target ? Math.min(target, expand + step) : Math.max(target, expand - step);
        }

        float eased = expand * expand * (3f - 2f * expand);
        float drift = eased * DRIFT;
        float alpha = REST_ALPHA + (1f - REST_ALPHA) * eased;
        int color = MenuSkin.mixColor(GlassSkin.bodyTextColor(), GlassSkin.titleTextColor(), eased);
        chevron(g, x + W / 2f + (right ? -drift : drift), y + H / 2f, right,
                MenuSkin.fade(color, alpha));
    }

    static void chevron(GuiGraphics g, float tipX, float centerY, boolean right, int color) {
        float tip = tipX + (right ? ARM / 2f : -ARM / 2f);
        for (int side = -1; side <= 1; side += 2) {
            g.pose().pushPose();
            g.pose().translate(tip, centerY, 0f);
            g.pose().mulPose(Axis.ZP.rotationDegrees(right ? side * 135f : side * 45f));
            g.pose().translate(-THICK * 0.4f, -THICK / 2f, 0f);
            g.pose().scale(ARM + THICK * 0.4f, THICK, 1f);
            g.fill(0, 0, 1, 1, color);
            g.pose().popPose();
        }
    }
}
