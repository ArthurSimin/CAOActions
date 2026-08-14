package com.sockywocky.createaddonorganizer.client;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.GuiGraphics;

final class GlassArrow {

    static final int W = 16;
    static final int H = 28;
    static final int MARGIN = 5;

    private static final float SECONDS = 0.14f;
    private static final int REST_INSET_X = 6;
    private static final int REST_INSET_Y = 10;
    private static final int ARM = 9;
    private static final int THICK = 3;

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
        return mouseX >= x && mouseX < x + W && mouseY >= y && mouseY < y + H;
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
        if (eased > 0.02f) {
            int insetX = Math.round((1f - eased) * REST_INSET_X);
            int insetY = Math.round((1f - eased) * REST_INSET_Y);
            GlassSkin.widgetBox(g, x + insetX, y + insetY, W - insetX * 2, H - insetY * 2, hovered, eased);
        }
        chevron(g, x + W / 2, y + H / 2, right,
                hovered ? GlassSkin.titleTextColor() : GlassSkin.bodyTextColor());
    }

    static void chevron(GuiGraphics g, int centerX, int centerY, boolean right, int color) {
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0f);
        g.pose().scale(0.5f, 0.5f, 1f);

        int dir = right ? 1 : -1;
        int offset = dir * ARM / 2;
        int half = THICK / 2;
        for (int t = 0; t <= ARM; t++) {
            int bx = offset - dir * t;
            g.fill(bx - half, -t - half, bx - half + THICK, -t - half + THICK, color);
            g.fill(bx - half, t - half, bx - half + THICK, t - half + THICK, color);
        }
        g.pose().popPose();
    }
}
