package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.client.MenuSkin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.util.Mth;

@Mixin(AbstractButton.class)
public abstract class MenuSkinButtonMixin {

    @Unique
    private MenuSkin.ButtonAnim createaddonorganizer$anim;

    @Unique
    private MenuSkin.ButtonAnim createaddonorganizer$anim() {
        if (createaddonorganizer$anim == null) {
            createaddonorganizer$anim = new MenuSkin.ButtonAnim();
        }
        return createaddonorganizer$anim;
    }

    @Inject(method = "renderWidget", require = 0, at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$skinButton(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!MenuSkin.active() || !MenuSkin.full()) {
            return;
        }
        createaddonorganizer$draw(g, 1f);
        ci.cancel();
    }

    @Inject(method = "renderWidget", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$blendButton(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!MenuSkin.active() || MenuSkin.full()) {
            return;
        }
        createaddonorganizer$draw(g, MenuSkin.blend());
    }

    @WrapOperation(method = "renderWidget", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;setColor(FFFF)V"))
    private void createaddonorganizer$fadeVanillaSprite(GuiGraphics g, float r, float gr, float b, float a,
            Operation<Void> original) {
        if (MenuSkin.active() && !MenuSkin.full()) {
            a *= 1f - MenuSkin.blend();
        }
        original.call(g, r, gr, b, a);
    }

    @ModifyArg(method = "renderWidget", require = 0, index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/AbstractButton;renderString"
                    + "(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V"))
    private int createaddonorganizer$fadeVanillaText(int color) {
        if (!MenuSkin.active() || MenuSkin.full()) {
            return color;
        }
        int a = Math.round(((color >>> 24) & 0xFF) * (1f - MenuSkin.blend()));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Unique
    private void createaddonorganizer$draw(GuiGraphics g, float blend) {
        AbstractButton self = (AbstractButton) (Object) this;
        float alpha = ((AbstractWidgetAlphaAccessor) (Object) this).createaddonorganizer$alpha() * blend;
        boolean hovered = self.isHoveredOrFocused();
        MenuSkin.button(g, createaddonorganizer$anim(), self.getX(), self.getY(), self.getWidth(), self.getHeight(),
                self.active, hovered, alpha);

        int rgb = MenuSkin.buttonTextColor(self.active, hovered);
        if (self instanceof CycleButton<?> cycle && cycle.getValue() instanceof Boolean on) {
            rgb = MenuSkin.booleanValueColor(on);
        }
        int argb = (rgb & 0x00FFFFFF) | (Mth.ceil(alpha * 255f) << 24);

        if (MenuSkin.isEditButton(self)
                && MenuSkin.editIcon(g, self.getX() + self.getWidth() / 2, self.getY() + self.getHeight() / 2, argb)) {
            return;
        }
        self.renderString(g, Minecraft.getInstance().font, argb);
    }

    @Inject(method = "onClick", require = 0, at = @At("HEAD"))
    private void createaddonorganizer$flashOnClick(double mouseX, double mouseY, CallbackInfo ci) {
        if (!MenuSkin.active()) {
            return;
        }
        AbstractButton self = (AbstractButton) (Object) this;
        createaddonorganizer$anim().click(self.active, self.isHoveredOrFocused());
    }
}
