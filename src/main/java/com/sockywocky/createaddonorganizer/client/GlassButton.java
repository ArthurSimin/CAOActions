package com.sockywocky.createaddonorganizer.client;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class GlassButton extends Button {

    public enum Style { BOX, ACCENT, DANGER }

    private static final int TEXT_PAD = 6;

    private Style style = Style.BOX;
    private BooleanSupplier changed;

    public GlassButton(int x, int y, int width, int height, Component label, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
    }

    public GlassButton style(Style value) {
        this.style = value;
        return this;
    }

    public GlassButton changed(BooleanSupplier value) {
        this.changed = value;
        return this;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        float a = this.alpha;
        boolean hovered = this.active && isHoveredOrFocused();

        switch (style) {
            case ACCENT -> GlassSkin.accentBox(g, x, y, w, h, hovered, a);
            case DANGER -> GlassSkin.dangerBox(g, x, y, w, h, hovered, a);
            default -> GlassSkin.widgetBox(g, x, y, w, h, hovered, a);
        }
        if (changed != null && changed.getAsBoolean()) {
            GlassSkin.changedBar(g, x, y, h, a);
        }

        Font font = Minecraft.getInstance().font;
        String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, w - TEXT_PAD * 2));
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2,
                MenuSkin.fade(textColor(hovered), a), GlassSkin.shadow());
    }

    private int textColor(boolean hovered) {
        if (!this.active) {
            return GlassSkin.mutedTextColor();
        }
        return switch (style) {
            case ACCENT -> GlassSkin.vanilla() && hovered ? 0xFFFFFFA0 : 0xFFFFFFFF;
            case DANGER -> GlassSkin.dangerTextColor(hovered);
            default -> hovered ? GlassSkin.titleTextColor() : GlassSkin.rowTextColor();
        };
    }
}
