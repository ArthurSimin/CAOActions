package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import com.sockywocky.createaddonorganizer.Config;

public class AccentSaturationSlider extends AbstractWidget {

    private static final int INSET = 2;
    private static final int TRACK_EMPTY = 0xFF2A2A2A;
    private static final int TRACK_FILL = 0xFF9A9A9A;
    private static final int TRACK_MARK = 0xFF6A6A6A;

    public AccentSaturationSlider(int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("createaddonorganizer.style.saturation"));
    }

    private int trackLeft() {
        return getX() + INSET;
    }

    private int trackSpan() {
        return Math.max(1, getWidth() - INSET * 2);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int top = getY() + INSET;
        int bottom = getY() + getHeight() - INSET;
        int span = trackSpan();
        int left = trackLeft();

        int filled = Math.round(Config.menuAccentSaturation() / (float) Config.MENU_SATURATION_MAX * span);
        g.fill(left, top, left + span, bottom, TRACK_EMPTY);
        g.fill(left, top, left + Mth.clamp(filled, 0, span), bottom, TRACK_FILL);

        int mark = left + Math.round(Config.MENU_SATURATION_DEFAULT / (float) Config.MENU_SATURATION_MAX * span);
        g.fill(mark, top, mark + 1, bottom, TRACK_MARK);

        MenuSkin.gradientOutline(g, getX(), getY(), getWidth(), getHeight(),
                MenuSkin.ruleColor(0xFFFFFFFF), MenuSkin.ruleColor(0x90FFFFFF));

        int handle = Mth.clamp(left + filled, left, left + span - 1);
        g.fill(handle - 1, getY() - 1, handle + 2, getY() + getHeight() + 1, 0xFF000000);
        g.fill(handle, getY() - 1, handle + 1, getY() + getHeight() + 1, 0xFFFFFFFF);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        applySaturation(mouseX);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        applySaturation(mouseX);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        Config.setMenuAccentSaturation(Config.menuAccentSaturation());
    }

    private void applySaturation(double mouseX) {
        float fraction = (float) (mouseX - trackLeft()) / trackSpan();
        Config.setMenuAccentSaturationLive(
                Mth.clamp(Math.round(fraction * Config.MENU_SATURATION_MAX), 0, Config.MENU_SATURATION_MAX));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        defaultButtonNarrationText(narration);
    }
}
