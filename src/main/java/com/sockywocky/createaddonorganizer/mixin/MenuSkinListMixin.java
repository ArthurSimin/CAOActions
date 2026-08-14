package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.client.MenuSkin;
import com.sockywocky.createaddonorganizer.client.Sfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.resources.ResourceLocation;

@Mixin(AbstractSelectionList.class)
public abstract class MenuSkinListMixin {

    @Unique
    private double createaddonorganizer$scrollBefore;

    @Unique
    private double createaddonorganizer$scrollAmount() {
        return ((AbstractSelectionList<?>) (Object) this).getScrollAmount();
    }

    @Unique
    private boolean createaddonorganizer$audible() {
        return MenuSkin.ownScreen(Minecraft.getInstance().screen);
    }

    @Inject(method = "mouseScrolled", require = 0, at = @At("HEAD"))
    private void createaddonorganizer$scrollBefore(double mouseX, double mouseY, double scrollX, double scrollY,
            CallbackInfoReturnable<Boolean> cir) {
        createaddonorganizer$scrollBefore = createaddonorganizer$scrollAmount();
    }

    @Inject(method = "mouseScrolled", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$scrollAfter(double mouseX, double mouseY, double scrollX, double scrollY,
            CallbackInfoReturnable<Boolean> cir) {
        if (scrollY != 0 && createaddonorganizer$audible()) {
            Sfx.scrolled(createaddonorganizer$scrollBefore, createaddonorganizer$scrollAmount(),
                    ((AbstractSelectionList<?>) (Object) this).getMaxScroll() > 0);
        }
    }

    @Inject(method = "mouseDragged", require = 0, at = @At("HEAD"))
    private void createaddonorganizer$dragBefore(double mouseX, double mouseY, int button, double dragX, double dragY,
            CallbackInfoReturnable<Boolean> cir) {
        createaddonorganizer$scrollBefore = createaddonorganizer$scrollAmount();
    }

    @Inject(method = "mouseDragged", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$dragAfter(double mouseX, double mouseY, int button, double dragX, double dragY,
            CallbackInfoReturnable<Boolean> cir) {
        if (createaddonorganizer$scrollBefore != createaddonorganizer$scrollAmount()
                && createaddonorganizer$audible()) {
            Sfx.scroll();
        }
    }

    @Inject(method = "renderListBackground", require = 0, at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$skinListBackground(GuiGraphics g, CallbackInfo ci) {
        if (!MenuSkin.active()) {
            return;
        }
        if (MenuSkin.full()) {
            AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
            MenuSkin.listBackground(g, self.getX(), self.getY(), self.getWidth(), self.getHeight(), 1f);
            ci.cancel();
            return;
        }
        g.setColor(1f, 1f, 1f, 1f - MenuSkin.blend());
    }

    @Inject(method = "renderListBackground", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$blendListBackground(GuiGraphics g, CallbackInfo ci) {
        if (!MenuSkin.active() || MenuSkin.full()) {
            return;
        }
        g.setColor(1f, 1f, 1f, 1f);
        AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
        MenuSkin.listBackground(g, self.getX(), self.getY(), self.getWidth(), self.getHeight(), MenuSkin.blend());
    }

    @Inject(method = "renderListSeparators", require = 0, at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$skinListSeparators(GuiGraphics g, CallbackInfo ci) {
        if (!MenuSkin.active()) {
            return;
        }
        if (MenuSkin.full()) {
            createaddonorganizer$separators(g, 1f);
            ci.cancel();
            return;
        }
        g.setColor(1f, 1f, 1f, 1f - MenuSkin.blend());
    }

    @Inject(method = "renderListSeparators", require = 0, at = @At("RETURN"))
    private void createaddonorganizer$blendListSeparators(GuiGraphics g, CallbackInfo ci) {
        if (!MenuSkin.active() || MenuSkin.full()) {
            return;
        }
        g.setColor(1f, 1f, 1f, 1f);
        createaddonorganizer$separators(g, MenuSkin.blend());
    }

    private void createaddonorganizer$separators(GuiGraphics g, float alpha) {
        AbstractSelectionList<?> self = (AbstractSelectionList<?>) (Object) this;
        int rule = MenuSkin.fade(MenuSkin.ruleColor(0xFFFFFFFF), alpha);
        g.fill(self.getX(), self.getY() - 1, self.getRight(), self.getY(), rule);
        g.fill(self.getX(), self.getBottom(), self.getRight(), self.getBottom() + 1, rule);
    }

    @WrapOperation(method = "renderWidget", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void createaddonorganizer$skinScrollbar(GuiGraphics g, ResourceLocation sprite,
            int x, int y, int width, int height, Operation<Void> original) {
        if (!MenuSkin.active()) {
            original.call(g, sprite, x, y, width, height);
            return;
        }
        float blend = MenuSkin.blend();
        if (!MenuSkin.full()) {
            g.setColor(1f, 1f, 1f, 1f - blend);
            original.call(g, sprite, x, y, width, height);
            g.setColor(1f, 1f, 1f, 1f);
        }
        if (sprite.getPath().contains("scroller_background")) {
            MenuSkin.scrollTrack(g, x, y, width, height, blend);
        } else {
            MenuSkin.scrollThumb(g, x, y, width, height, blend);
        }
    }
}
