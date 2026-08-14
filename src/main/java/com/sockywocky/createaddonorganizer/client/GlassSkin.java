package com.sockywocky.createaddonorganizer.client;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class GlassSkin {

    public static final int DEFAULT_ACCENT = 0xFF8A9AA8;
    public static final int DEFAULT_ACCENT_LIT = 0xFFB9C2CA;
    public static final int GLASS_LINE = 0x4DFFFFFF;
    public static final int GLASS_LINE_HOVER = 0x99FFFFFF;
    public static final int DANGER = 0xFFAA2E24;
    public static final int DANGER_LIT = 0xFFE0776B;

    public static final ResourceLocation VANILLA_BUTTON =
            ResourceLocation.withDefaultNamespace("widget/button");
    public static final ResourceLocation VANILLA_BUTTON_HOVER =
            ResourceLocation.withDefaultNamespace("widget/button_highlighted");

    private GlassSkin() {}

    public static boolean vanilla() {
        return !MenuSkin.active();
    }

    public static boolean glass() {
        return Config.menuStyle() == Config.MenuStyle.DEFAULT_MODERN;
    }

    public static boolean shadow() {
        return vanilla();
    }

    public static int accent() {
        return MenuSkin.accent(DEFAULT_ACCENT);
    }

    public static int titleTextColor() {
        return MenuSkin.active() ? MenuSkin.palette().textHover() : 0xFFFFFFFF;
    }

    public static int bodyTextColor() {
        return vanilla() ? 0xFFA0A0A0 : MenuSkin.mutedColor(0xFF9A9A9A);
    }

    public static int headingColor() {
        return vanilla() ? 0xFFFFFFFF : MenuSkin.bodyColor(0xFFB4B4B4);
    }

    public static int rowTextColor() {
        return vanilla() ? 0xFFC6C6C6 : MenuSkin.bodyColor(0xFFB0B0B0);
    }

    public static int panelColor() {
        return MenuSkin.active() ? MenuSkin.tint(MenuSkin.palette().listBackground()) : 0xD0101010;
    }

    public static int cardColor(boolean hovered) {
        if (vanilla()) {
            return hovered ? 0x70000000 : 0x50000000;
        }
        return MenuSkin.tint(hovered ? MenuSkin.palette().boxBackgroundHover() : MenuSkin.palette().boxBackground());
    }

    public static int cardBorder(boolean hovered) {
        if (vanilla()) {
            return hovered ? 0xFFFFFFFF : 0xFF000000;
        }
        if (glass()) {
            return hovered ? GLASS_LINE_HOVER : GLASS_LINE;
        }
        return borderColor();
    }

    public static int borderColor() {
        if (vanilla()) {
            return 0xFF6E6E6E;
        }
        if (glass()) {
            return GLASS_LINE;
        }
        return MenuSkin.mixColor(0xFF000000 | (panelColor() & 0x00FFFFFF), 0xFFFFFFFF, 0.14f);
    }

    public static int hoverLine(boolean lit) {
        if (glass()) {
            return lit ? GLASS_LINE_HOVER : GLASS_LINE;
        }
        return lit ? accent() : borderColor();
    }

    public static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        g.fill(x, y, x + width, y + 1, color);
        g.fill(x, y + height - 1, x + width, y + height, color);
        g.fill(x, y + 1, x + 1, y + height - 1, color);
        g.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void panel(GuiGraphics g, int x, int y, int width, int height) {
        panel(g, x, y, width, height, 1f);
    }

    public static void panel(GuiGraphics g, int x, int y, int width, int height, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        if (vanilla()) {
            if (alpha >= 0.99f) {
                MenuSkin.contrastArea(g, x + 1, y + 1, width - 2, height - 2);
            } else {
                g.fill(x + 1, y + 1, x + width - 1, y + height - 1, MenuSkin.fade(0xB0000000, alpha));
            }
            outline(g, x, y, width, height, MenuSkin.fade(0xFF000000, alpha));
            return;
        }
        g.fill(x, y, x + width, y + height, MenuSkin.fade(panelColor(), alpha));
        outline(g, x, y, width, height, MenuSkin.fade(borderColor(), alpha));
    }

    public static int opaquePanelColor() {
        return MenuSkin.active() ? MenuSkin.tint(MenuSkin.palette().opaqueBackground()) : 0xFF101010;
    }

    public static void popupPanel(GuiGraphics g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, opaquePanelColor());
        panel(g, x, y, width, height);
    }

    public static void widgetBox(GuiGraphics g, int x, int y, int width, int height, boolean hovered) {
        widgetBox(g, x, y, width, height, hovered, 1f);
    }

    public static void widgetBox(GuiGraphics g, int x, int y, int width, int height, boolean hovered, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        if (vanilla()) {
            g.setColor(1f, 1f, 1f, alpha);
            g.blitSprite(hovered ? VANILLA_BUTTON_HOVER : VANILLA_BUTTON, x, y, width, height);
            g.setColor(1f, 1f, 1f, 1f);
            return;
        }
        g.fill(x, y, x + width, y + height, MenuSkin.fade(cardColor(hovered), alpha));
        outline(g, x, y, width, height, MenuSkin.fade(hoverLine(hovered), alpha));
    }

    public static void accentBox(GuiGraphics g, int x, int y, int width, int height, boolean hovered, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        if (vanilla()) {
            g.setColor(1f, 1f, 1f, alpha);
            g.blitSprite(hovered ? VANILLA_BUTTON_HOVER : VANILLA_BUTTON, x, y, width, height);
            g.setColor(1f, 1f, 1f, 1f);
            return;
        }
        if (glass()) {
            g.fill(x, y, x + width, y + height, MenuSkin.fade(accent(), (hovered ? 0.55f : 0.3f) * alpha));
            outline(g, x, y, width, height, MenuSkin.fade(hoverLine(hovered), alpha));
            return;
        }
        int face = hovered ? accent() : MenuSkin.mixColor(accent(), 0xFF000000, 0.28f);
        g.fill(x, y, x + width, y + height, MenuSkin.fade(face, alpha));
        outline(g, x, y, width, height, MenuSkin.fade(MenuSkin.mixColor(face, 0xFFFFFFFF, 0.2f), alpha));
    }

    public static void changedBar(GuiGraphics g, int x, int y, int height, float alpha) {
        g.fill(x + 1, y + 1, x + 3, y + height - 1, MenuSkin.fade(accent(), 0.65f * alpha));
    }

    public static int headerHeight() {
        return 14;
    }

    public static void header(GuiGraphics g, Font font, Component label, int x, int y, int width, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        if (glass()) {
            g.drawString(font, label, x, y, MenuSkin.fade(accent(), alpha), shadow());
            int ruleY = y + 10;
            g.fill(x, ruleY, x + width, ruleY + 1, MenuSkin.fade(accent(), 0.45f * alpha));
            return;
        }
        int textWidth = font.width(label);
        int textX = x + width / 2 - textWidth / 2;
        int lineY = y + 4;
        int line = MenuSkin.fade(borderColor(), alpha);
        if (textX - 6 > x) {
            g.fill(x, lineY, textX - 6, lineY + 1, line);
            g.fill(textX + textWidth + 6, lineY, x + width, lineY + 1, line);
        }
        g.drawString(font, label, textX, y, MenuSkin.fade(headingColor(), alpha), shadow());
    }

    public static void divider(GuiGraphics g, int x, int y, int width, float alpha) {
        g.fill(x, y, x + width, y + 1, MenuSkin.fade(vanilla() ? 0x60FFFFFF : borderColor(), alpha));
    }

    public static int mutedTextColor() {
        return vanilla() ? 0xFF707070 : MenuSkin.mutedColor(0xFF707070);
    }

    public static int dangerTextColor(boolean hovered) {
        return hovered ? DANGER_LIT : rowTextColor();
    }

    public static void dangerBox(GuiGraphics g, int x, int y, int width, int height, boolean hovered, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }
        if (vanilla()) {
            widgetBox(g, x, y, width, height, hovered, alpha);
            return;
        }
        g.fill(x, y, x + width, y + height, MenuSkin.fade(DANGER, (hovered ? 0.45f : 0.22f) * alpha));
        outline(g, x, y, width, height, MenuSkin.fade(hovered ? DANGER_LIT : borderColor(), alpha));
    }
}
