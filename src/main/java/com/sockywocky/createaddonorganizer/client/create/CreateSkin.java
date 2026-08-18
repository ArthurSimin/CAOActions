package com.sockywocky.createaddonorganizer.client.create;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.createmod.catnip.config.ui.ConfigScreen;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.enums.PonderGuiTextures;

import com.sockywocky.createaddonorganizer.client.MenuSkin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class CreateSkin {

    private static boolean swapped;

    private CreateSkin() {}

    public static void beginFrame() {
        if (UIRenderHelper.framebuffer == null) {
            return;
        }
        if (swapped) {
            endFrame();
        }
        Minecraft mc = Minecraft.getInstance();
        UIRenderHelper.swapAndBlitColor(mc.getMainRenderTarget(), UIRenderHelper.framebuffer);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT, Minecraft.ON_OSX);
        swapped = true;
    }

    public static void endFrame() {
        if (!swapped) {
            return;
        }
        swapped = false;
        UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, Minecraft.getInstance().getMainRenderTarget());
    }

    public static boolean framePending() {
        return swapped;
    }

    public static void discardStaleFrame() {
        swapped = false;
    }

    public static boolean rebindFrame() {
        if (!swapped || UIRenderHelper.framebuffer == null) {
            return false;
        }
        UIRenderHelper.framebuffer.bindWrite(true);
        return true;
    }

    private static final DelegatedStencilElement COG = new DelegatedStencilElement(
            (graphics, w, h, a) -> renderCogStencil(graphics),
            (graphics, w, h, a) -> graphics.fill(-200, -200, 200, 200, shadowColor(a)));

    private static void renderCogStencil(GuiGraphics graphics) {
        float partialTicks = MenuSkin.cogPartialTick();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(-100f, 100f, -100f);
        pose.scale(200f, 200f, 1f);
        GuiGameElement.of(ConfigScreen.shadowState)
                .rotateBlock(22.5, ConfigScreen.cogSpin.getValue(partialTicks), 22.5)
                .render(graphics);
        pose.popPose();
    }

    private static int shadowColor(float alpha) {
        return Math.round(0x60 * Mth.clamp(alpha, 0f, 1f)) << 24;
    }

    public static void renderCog(GuiGraphics g, int width, int height, float alpha) {
        if (!swapped) {
            return;
        }
        COG.withAlpha(alpha);
        COG.at(width * 0.5f, height * 0.5f, 0f);
        COG.render(g);
    }

    public static void tickCog() {
        ConfigScreen.cogSpin.tick();
    }

    public static void bumpCog(double scrollDeltaY) {
        ConfigScreen.cogSpin.bump(3, -scrollDeltaY * 5.0);
    }

    public static void editIcon(GuiGraphics g, int x, int y, int argb) {
        PonderGuiTextures.ICON_CONFIG_OPEN.render(g, x, y, new Color(argb, true));
    }

    public static void arrowIcon(GuiGraphics g, int centerX, int centerY, float rotation, int argb) {
        g.pose().pushPose();
        g.pose().translate(centerX, centerY, 0f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rotation));
        g.pose().translate(-8f, -8f, 0f);
        PonderGuiTextures.ICON_CONFIG_NEXT.render(g, 0, 0, new Color(argb, true));
        g.pose().popPose();
    }

    public static void box(GuiGraphics g, int x, int y, int width, int height,
            int background, int borderTop, int borderBottom) {
        g.flush();
        new BoxElement()
                .withBackground(new Color(background, true))
                .gradientBorder(Couple.create(new Color(borderTop, true), new Color(borderBottom, true)))
                .withBorderOffset(0)
                .at(x + 1, y + 1, 0f)
                .withBounds(width - 2, height - 2)
                .render(g);
    }

}
