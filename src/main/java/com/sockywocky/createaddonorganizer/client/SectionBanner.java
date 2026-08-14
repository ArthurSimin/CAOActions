package com.sockywocky.createaddonorganizer.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.Window;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;

public final class SectionBanner {
    private SectionBanner() {}

    public static final int WIDTH = 160;
    public static final int HEIGHT = 16;
    public static final int ROW_H = 18;

    private static final Map<String, Component> CLAMPED_TITLES = new HashMap<>();

    public static void draw(GuiGraphics g, Font font, int topLeftX, int topLeftY, ResourceLocation id,
            Component title, ColorSpec bannerColor, ResourceLocation texture, ColorSpec textColor,
            boolean bevels) {
        int x1 = topLeftX + 1;
        int x2 = x1 + WIDTH;
        int contentTop = topLeftY + 1;
        int contentBottom = contentTop + HEIGHT;

        if (bevels && Config.gridBridge()) {
            g.fill(x1 - 1, topLeftY, x2, topLeftY + 1, MenuPixels.shadow());
            g.fill(x1, topLeftY + ROW_H - 1, x2 + 1, topLeftY + ROW_H, MenuPixels.highlight());
        }

        if (texture != null) {
            var anim = BannerAnimation.get(texture);
            float v = 0f;
            int texHeight = BannerTextures.HEIGHT;
            if (anim.isPresent()) {
                boolean hovered = isHoveredNow(x1, contentTop);
                int frame = BannerAnimation.currentFrame(texture, anim.get(), hovered);
                v = frame * BannerTextures.HEIGHT;
                texHeight = anim.get().frameCount() * BannerTextures.HEIGHT;
            }
            reportOnce(id, texture, anim, x1, contentTop, textColor, Config.textSecondaryColorFor(id),
                    Config.twoToneSplitFor(id));
            g.blit(texture, x1, contentTop, WIDTH, HEIGHT, 0f, v,
                    BannerTextures.WIDTH, HEIGHT, BannerTextures.WIDTH, texHeight);
        } else {
            BannerFill.draw(g, x1, contentTop, x2, contentBottom, bannerColor);
        }

        int textX = topLeftX + 6;
        int textY = topLeftY + 5;
        int maxX = x2 - 3;
        int available = maxX - textX;
        float cutoff = Config.scrollCutoffFor(id);
        int viewMaxX = textX + Math.round(available * cutoff);
        int viewAvailable = viewMaxX - textX;
        boolean screenshot = TwoToneText.renderTargetActive();
        Component shown = screenshot ? clampedTitle(font, title, viewAvailable) : title;
        boolean scrolling = !screenshot && font.width(shown) > viewAvailable;

        if (Config.tintedTextBoxFor(id)) {
            int w = scrolling ? viewAvailable : font.width(shown);
            ResourceLocation boxTexture = BoxTextures.resolve(Config.boxTextureRefFor(id));
            BoxTextures.draw(g, boxTexture, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2,
                    Config.boxColorFor(id), Config.boxDarkenFor(id), Config.boxOpacityFor(id));
        }
        boolean shadowOn = Config.titleTextShadow(id);
        Integer shadowOverride = shadowOn ? Config.textShadowColorFor(id) : null;
        boolean vanillaShadow = shadowOn && shadowOverride == null;
        ColorSpec outline = Config.textOutlineColorFor(id);
        TwoToneText.draw(g, font, shown, textX, textY, viewMaxX, textColor, Config.textSecondaryColorFor(id),
                Config.twoToneSplitFor(id), vanillaShadow, shadowOverride != null ? shadowOverride : 0, outline);
    }

    public static void drawResolved(GuiGraphics g, Font font, int topLeftX, int topLeftY,
            ResourceLocation id, Component title) {
        draw(g, font, topLeftX, topLeftY, id, title, Config.bannerColorFor(id),
                BannerTextures.resolve(Config.bannerRefFor(id)), Config.textColorFor(id), false);
    }

    private static Component clampedTitle(Font font, Component title, int maxW) {
        if (font.width(title) <= maxW) {
            return title;
        }
        String key = maxW + " " + title.getString();
        Component cached = CLAMPED_TITLES.get(key);
        if (cached != null) {
            return cached;
        }
        FormattedText cut = font.substrByWidth(title, Math.max(0, maxW - font.width("…")));
        Component shown = Component.literal(cut.getString().stripTrailing() + "…");
        CLAMPED_TITLES.put(key, shown);
        return shown;
    }

    private static final Set<ResourceLocation> REPORTED = new HashSet<>();

    private static void reportOnce(ResourceLocation id, ResourceLocation texture,
            java.util.Optional<BannerAnimation.AnimInfo> anim, int x, int y, ColorSpec primary,
            ColorSpec secondary, float split) {
        if (!Config.bannerDrawDiagnostics() || !REPORTED.add(id)) {
            return;
        }
        Window window = Minecraft.getInstance().getWindow();
        createaddonorganizer.LOGGER.info("[CAO] draw {}: texture={} bound={} anim={} at=({},{}) scale={} "
                        + "primary={} secondary={} split={} target={}",
                id, texture,
                Minecraft.getInstance().getTextureManager().getTexture(texture, null) != null,
                anim.map(a -> a.frameCount() + "f/" + a.frameTicks() + "t/hoverOnly=" + a.hoverOnly())
                        .orElse("none"),
                x, y, window.getGuiScale(),
                primary == null ? "null" : Integer.toHexString(primary.color1()),
                secondary == null ? "null" : Integer.toHexString(secondary.color1()),
                split, TwoToneText.renderTargetActive());
    }

    private static boolean isHoveredNow(int x, int y) {
        Window window = Minecraft.getInstance().getWindow();
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() / window.getGuiScale();
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() / window.getGuiScale();
        return BannerAnimation.isHovering(x, y, WIDTH, HEIGHT, mouseX, mouseY);
    }
}
