package com.sockywocky.createaddonorganizer.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class ModIcons {

    private record Logo(ResourceLocation texture, int width, int height) {}

    private static final String SHIPPED_DIR = "textures/gui/integration/";
    private static final float GREY_DIM = 0.72f;

    private static final Map<String, Logo> COLOUR = new HashMap<>();
    private static final Map<String, Logo> GREY = new HashMap<>();

    private ModIcons() {}

    public static void render(GuiGraphics g, String modId, String displayName, int x, int y, int size,
            boolean lit) {
        Logo logo = cached(lit ? COLOUR : GREY, modId, lit);
        if (logo == null) {
            fallback(g, modId, displayName, x, y, size, lit);
            return;
        }
        if (!lit) {
            g.setColor(1f, 1f, 1f, 0.8f);
        }
        g.blit(logo.texture(), x, y, size, size, 0f, 0f, logo.width(), logo.height(),
                logo.width(), logo.height());
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static Logo cached(Map<String, Logo> cache, String modId, boolean lit) {
        if (cache.containsKey(modId)) {
            return cache.get(modId);
        }
        Logo logo = null;
        try {
            logo = lit ? colour(modId) : grey(modId);
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.debug("[CAO] could not read the mod logo for {}", modId, t);
        }
        cache.put(modId, logo);
        return logo;
    }

    private static Logo colour(String modId) throws Exception {
        ResourceLocation shipped = shippedId(modId);
        if (shipped != null) {
            try (NativeImage image = readShipped(shipped)) {
                if (image != null) {
                    return new Logo(shipped, image.getWidth(), image.getHeight());
                }
            }
        }
        NativeImage image = fromModJar(modId);
        return image == null ? null : register(modId, "mod_logo/", image);
    }

    private static Logo grey(String modId) throws Exception {
        NativeImage image = null;
        ResourceLocation shipped = shippedId(modId);
        if (shipped != null) {
            image = readShipped(shipped);
        }
        if (image == null) {
            image = fromModJar(modId);
        }
        if (image == null) {
            return null;
        }
        desaturate(image);
        return register(modId, "mod_logo_grey/", image);
    }

    private static void desaturate(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int alpha = (abgr >>> 24) & 0xFF;
                int blue = (abgr >>> 16) & 0xFF;
                int green = (abgr >>> 8) & 0xFF;
                int red = abgr & 0xFF;
                int luma = Math.round((0.2126f * red + 0.7152f * green + 0.0722f * blue) * GREY_DIM);
                luma = Math.min(255, Math.max(0, luma));
                image.setPixelRGBA(x, y, (alpha << 24) | (luma << 16) | (luma << 8) | luma);
            }
        }
    }

    private static ResourceLocation shippedId(String modId) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                SHIPPED_DIR + safe(modId) + ".png");
        Minecraft mc = Minecraft.getInstance();
        if (mc.getResourceManager() == null || mc.getResourceManager().getResource(id).isEmpty()) {
            return null;
        }
        return id;
    }

    private static NativeImage readShipped(ResourceLocation id) throws Exception {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(id)) {
            return NativeImage.read(in);
        }
    }

    private static NativeImage fromModJar(String modId) throws Exception {
        Optional<? extends ModContainer> container = ModList.get().getModContainerById(modId);
        if (container.isEmpty()) {
            return null;
        }
        IModInfo info = container.get().getModInfo();
        Optional<String> logoFile = info.getLogoFile();
        if (logoFile.isEmpty() || logoFile.get().isBlank()) {
            return null;
        }
        IModFileInfo fileInfo = info.getOwningFile();
        if (fileInfo == null || fileInfo.getFile() == null) {
            return null;
        }
        Path path = fileInfo.getFile().findResource(logoFile.get());
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(path)) {
            return NativeImage.read(in);
        }
    }

    private static Logo register(String modId, String prefix, NativeImage image) {
        DynamicTexture texture = new DynamicTexture(image);
        texture.setFilter(true, false);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                prefix + safe(modId));
        Minecraft.getInstance().getTextureManager().register(id, texture);
        return new Logo(id, image.getWidth(), image.getHeight());
    }

    private static String safe(String modId) {
        return modId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static void fallback(GuiGraphics g, String modId, String displayName, int x, int y, int size,
            boolean lit) {
        int tint = tintOf(modId);
        int face = lit ? tint : MenuSkin.mixColor(tint, 0xFF20242A, 0.72f);
        g.fill(x + 1, y, x + size - 1, y + size, face);
        g.fill(x, y + 1, x + size, y + size - 1, face);
        GlassSkin.outline(g, x, y, size, size, MenuSkin.fade(0xFFFFFFFF, lit ? 0.22f : 0.10f));

        String initials = initialsOf(displayName == null ? modId : displayName);
        Font font = Minecraft.getInstance().font;
        g.drawString(font, initials, x + (size - font.width(initials)) / 2, y + (size - 8) / 2,
                MenuSkin.fade(0xFFFFFFFF, lit ? 0.92f : 0.45f), false);
    }

    private static String initialsOf(String name) {
        String[] words = name.trim().split("[^A-Za-z0-9]+");
        if (words.length >= 2 && !words[0].isEmpty() && !words[1].isEmpty()) {
            return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase(Locale.ROOT);
        }
        String first = words.length == 0 ? name : words[0];
        if (first.isEmpty()) {
            return "?";
        }
        return first.substring(0, Math.min(2, first.length())).toUpperCase(Locale.ROOT);
    }

    private static int tintOf(String modId) {
        int hash = modId.hashCode();
        float hue = ((hash >>> 8) & 0xFF) / 255f;
        return 0xFF000000 | (ColorUtil.hsvToRgb(hue, 0.42f, 0.52f) & 0x00FFFFFF);
    }
}
