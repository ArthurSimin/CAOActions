package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.sockywocky.createaddonorganizer.Config;

public class AccentHueSlider extends AbstractWidget {

    private static final float STRIP_SATURATION = 0.355f;
    private static final float STRIP_VALUE = 0.839f;
    private static final int INSET = 2;
    private static final int NEUTRAL_W = 16;

    public AccentHueSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("createaddonorganizer.style.accent"));
    }

    private int trackLeft() {
        return getX() + INSET;
    }

    private int hueLeft() {
        return trackLeft() + NEUTRAL_W;
    }

    private int hueSpan() {
        return Math.max(1, getWidth() - INSET * 2 - NEUTRAL_W);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int top = getY() + INSET;
        int bottom = getY() + getHeight() - INSET;

        int neutral = MenuSkin.hsvToRgb(0f, 0f, STRIP_VALUE);
        g.fill(trackLeft(), top, trackLeft() + NEUTRAL_W, bottom, neutral);
        g.fill(trackLeft() + NEUTRAL_W - 1, top, trackLeft() + NEUTRAL_W, bottom, 0xFF000000);

        float strip = Mth.clamp(STRIP_SATURATION * MenuSkin.accentSaturationFactor(), 0f, 1f);
        int span = hueSpan();
        for (int i = 0; i < span; i++) {
            g.fill(hueLeft() + i, top, hueLeft() + i + 1, bottom,
                    MenuSkin.hsvToRgb(i * 360f / span, strip, STRIP_VALUE));
        }

        MenuSkin.gradientOutline(g, getX(), getY(), getWidth(), getHeight(),
                MenuSkin.ruleColor(0xFFFFFFFF), MenuSkin.ruleColor(0x90FFFFFF));

        int hue = Config.menuAccentHue();
        int handle = hue == Config.MENU_ACCENT_NEUTRAL
                ? trackLeft() + NEUTRAL_W / 2
                : Mth.clamp(hueLeft() + Math.round(hue / 360f * span), hueLeft(), hueLeft() + span - 1);
        g.fill(handle - 1, getY() - 1, handle + 2, getY() + getHeight() + 1, 0xFF000000);
        g.fill(handle, getY() - 1, handle + 1, getY() + getHeight() + 1, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        applyHue(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        applyHue(mouseX);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        Config.setMenuAccentHue(Config.menuAccentHue());
    }

    private void applyHue(double mouseX) {
        if (mouseX < hueLeft()) {
            Config.setMenuAccentHueLive(Config.MENU_ACCENT_NEUTRAL);
            return;
        }
        float fraction = (float) (mouseX - hueLeft()) / hueSpan();
        Config.setMenuAccentHueLive(Mth.clamp(Math.round(fraction * 360f), 0, 360));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
