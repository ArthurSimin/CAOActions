package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.client.MenuSkin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.resources.ResourceLocation;

@Mixin(EditBox.class)
public abstract class MenuSkinEditBoxMixin {

    @WrapOperation(method = "renderWidget", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void createaddonorganizer$skinEditBox(GuiGraphics g, ResourceLocation sprite,
            int x, int y, int width, int height, Operation<Void> original) {
        boolean focused = ((EditBox) (Object) this).isFocused();
        if (!MenuSkin.active()) {
            original.call(g, sprite, x, y, width, height);
            return;
        }
        if (!MenuSkin.full()) {
            float blend = MenuSkin.blend();
            g.setColor(1f, 1f, 1f, 1f - blend);
            original.call(g, sprite, x, y, width, height);
            g.setColor(1f, 1f, 1f, 1f);
            MenuSkin.editBox(g, x, y, width, height, focused, blend);
            return;
        }
        MenuSkin.editBox(g, x, y, width, height, focused, 1f);
    }
}
