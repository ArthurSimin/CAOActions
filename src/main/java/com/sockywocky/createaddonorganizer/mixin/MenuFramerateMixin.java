package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.Window;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(Minecraft.class)
public abstract class MenuFramerateMixin {

    @Shadow
    public ClientLevel level;

    @Shadow
    public Screen screen;

    @Shadow
    public abstract Overlay getOverlay();

    @Shadow
    public abstract Window getWindow();

    @Inject(method = "getFramerateLimit", require = 0, at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$raiseMenuFramerate(CallbackInfoReturnable<Integer> cir) {
        if (this.level != null || (this.screen == null && this.getOverlay() == null)) {
            return;
        }
        int menuLimit = Config.menuFramerate();
        if (menuLimit <= 60) {
            return;
        }
        cir.setReturnValue(Math.min(menuLimit, this.getWindow().getFramerateLimit()));
    }
}
