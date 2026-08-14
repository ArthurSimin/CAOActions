package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.client.CreateCompat;
import com.sockywocky.createaddonorganizer.client.MenuSkin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public abstract class MenuSkinScreenMixin {

    @Inject(method = "render", require = 0, at = @At("HEAD"))
    private void createaddonorganizer$advanceSkin(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        MenuSkin.advanceBlend();
    }

    @Inject(method = "render", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$endSkinFrame(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        CreateCompat.endFrame();
    }

    @WrapOperation(method = "renderBackground", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderMenuBackground"
                    + "(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void createaddonorganizer$fadeMenuBackground(Screen instance, GuiGraphics g,
            Operation<Void> original) {
        float blend = MenuSkin.active() ? MenuSkin.blend() : 0f;
        if (blend <= 0f) {
            original.call(instance, g);
            return;
        }
        if (blend >= 0.998f) {
            return;
        }
        g.setColor(1f, 1f, 1f, 1f - blend);
        original.call(instance, g);
        g.setColor(1f, 1f, 1f, 1f);
    }

    @Inject(method = "renderBackground", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$skinBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!MenuSkin.active()) {
            return;
        }
        if (MenuSkin.cogEnabled()) {
            g.flush();
            CreateCompat.beginFrame();
        }
        Screen self = (Screen) (Object) this;
        float blend = MenuSkin.blend();
        MenuSkin.overlay(g, self.width, self.height, blend);
        MenuSkin.cog(g, self.width, self.height, blend);
    }
}
