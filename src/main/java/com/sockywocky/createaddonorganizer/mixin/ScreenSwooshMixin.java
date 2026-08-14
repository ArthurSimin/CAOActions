package com.sockywocky.createaddonorganizer.mixin;

import com.mojang.blaze3d.systems.RenderSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import com.sockywocky.createaddonorganizer.client.ScreenSwoosh;

@Mixin(Screen.class)
public abstract class ScreenSwooshMixin {

    @Unique
    private boolean createaddonorganizer$swooshing;

    @Unique
    private boolean createaddonorganizer$backgroundLifted;

    @Unique
    private float createaddonorganizer$opacity = 1f;

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void createaddonorganizer$beginSwoosh(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        ScreenSwoosh.update();
        createaddonorganizer$swooshing = ScreenSwoosh.appliesTo((Screen) (Object) this);
        if (!createaddonorganizer$swooshing) {
            return;
        }
        createaddonorganizer$opacity = ScreenSwoosh.opacity();
        g.pose().pushPose();
        g.pose().translate(ScreenSwoosh.offsetX(), ScreenSwoosh.offsetY(), 0f);
        if (createaddonorganizer$opacity < 1f) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1f, 1f, 1f, createaddonorganizer$opacity);
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void createaddonorganizer$holdBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        createaddonorganizer$backgroundLifted = createaddonorganizer$swooshing;
        if (!createaddonorganizer$backgroundLifted) {
            return;
        }
        g.flush();
        ScreenSwoosh.setBackgroundPass(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.pose().pushPose();
        g.pose().translate(-ScreenSwoosh.offsetX(), -ScreenSwoosh.offsetY(), 0f);
    }

    @Inject(method = "renderBackground", at = @At("RETURN"))
    private void createaddonorganizer$releaseBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!createaddonorganizer$backgroundLifted) {
            return;
        }
        createaddonorganizer$backgroundLifted = false;
        g.pose().popPose();
        g.flush();
        ScreenSwoosh.setBackgroundPass(false);
        if (createaddonorganizer$opacity < 1f) {
            RenderSystem.setShaderColor(1f, 1f, 1f, createaddonorganizer$opacity);
        }
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void createaddonorganizer$endSwoosh(GuiGraphics g, int mouseX, int mouseY, float partialTick,
            CallbackInfo ci) {
        if (!createaddonorganizer$swooshing) {
            return;
        }
        createaddonorganizer$swooshing = false;
        g.pose().popPose();
        g.flush();
        ScreenSwoosh.setBackgroundPass(false);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
