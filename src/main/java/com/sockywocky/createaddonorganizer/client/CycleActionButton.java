package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class CycleActionButton extends GlassButton {
    private final Runnable onForward;
    private final Runnable onBackward;

    CycleActionButton(int x, int y, int width, int height, Component message, Runnable onForward, Runnable onBackward) {
        super(x, y, width, height, message, b -> onForward.run());
        this.onForward = onForward;
        this.onBackward = onBackward;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.active || !this.visible || scrollY == 0 || !this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        if (scrollY > 0) {
            onForward.run();
        } else {
            onBackward.run();
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (this.active && this.visible && this.clicked(mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                onBackward.run();
                return true;
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
