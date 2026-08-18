package com.sockywocky.createaddonorganizer.client;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class GlassToggle extends AbstractButton {

    public static final int HEIGHT = 20;

    private static final int TRACK_W = 24;
    private static final int TRACK_H = 12;
    private static final int KNOB_W = 10;
    private static final int LABEL_GAP = 6;
    private static final float SECONDS = 0.14f;

    private static final Map<String, Float> PROGRESS = new HashMap<>();

    private final Consumer<Boolean> onChange;
    private String key;
    private boolean selected;
    private long frameNanos;

    public GlassToggle(int x, int y, Component label, boolean selected, Consumer<Boolean> onChange) {
        super(x, y, TRACK_W + LABEL_GAP + Minecraft.getInstance().font.width(label), HEIGHT, label);
        this.selected = selected;
        this.key = label.getString();
        this.onChange = onChange;
    }

    public GlassToggle key(String value) {
        this.key = value;
        return this;
    }

    public boolean selected() {
        return selected;
    }

    @Override
    public void onPress() {
        selected = !selected;
        onChange.accept(selected);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.active && isHoveredOrFocused();
        int trackX = getX();
        int trackY = getY() + (getHeight() - TRACK_H) / 2;

        float eased = advance();
        int off = GlassSkin.vanilla() ? 0xFF5A5A5A : MenuSkin.mutedColor(0xFF5A5A5A);
        int track = MenuSkin.mixColor(off, GlassSkin.accent(), eased);
        if (hovered) {
            track = MenuSkin.mixColor(track, 0xFFFFFFFF, 0.15f);
        }
        g.fill(trackX, trackY, trackX + TRACK_W, trackY + TRACK_H, MenuSkin.fade(track, this.alpha));
        GlassSkin.outline(g, trackX, trackY, TRACK_W, TRACK_H,
                MenuSkin.fade(MenuSkin.mixColor(track, 0xFF000000, 0.35f), this.alpha));

        int knobX = trackX + 1 + Math.round(eased * (TRACK_W - 2 - KNOB_W));
        g.fill(knobX, trackY + 1, knobX + KNOB_W, trackY + TRACK_H - 1,
                MenuSkin.fade(hovered ? 0xFFFFFFFF : 0xFFF0F0F0, this.alpha));

        int labelColor = this.active
                ? (hovered ? GlassSkin.titleTextColor() : GlassSkin.rowTextColor())
                : GlassSkin.mutedTextColor();
        g.drawString(Minecraft.getInstance().font, getMessage(), trackX + TRACK_W + LABEL_GAP,
                getY() + (getHeight() - 8) / 2, MenuSkin.fade(labelColor, this.alpha), GlassSkin.shadow());
    }

    private float advance() {
        float target = selected ? 1f : 0f;
        if (!Config.animOn(Config.ANIM_BUTTON_HOVER)) {
            PROGRESS.put(key, target);
            return target;
        }
        float progress = PROGRESS.getOrDefault(key, target);
        long now = System.nanoTime();
        float delta = frameNanos == 0L ? 0f : Math.min(0.25f, (now - frameNanos) / 1_000_000_000f);
        frameNanos = now;
        float step = delta / SECONDS;
        progress = progress < target ? Math.min(target, progress + step) : Math.max(target, progress - step);
        PROGRESS.put(key, progress);
        return progress * progress * (3f - 2f * progress);
    }
}
