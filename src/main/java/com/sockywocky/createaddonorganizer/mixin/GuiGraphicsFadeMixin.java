package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.GuiGraphics;

import com.sockywocky.createaddonorganizer.client.ScreenSwoosh;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsFadeMixin {

    @ModifyVariable(method = "setColor(FFFF)V", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private float createaddonorganizer$fadeAlpha(float alpha) {
        return alpha * ScreenSwoosh.fadeMultiplier();
    }
}
