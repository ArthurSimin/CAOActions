package com.sockywocky.createaddonorganizer.client;

import java.io.InputStream;

import com.mojang.blaze3d.platform.NativeImage;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class MenuPixels {

    public record Palette(int panel, int slot, int shadow, int highlight, boolean sampled) {}

    private static final ResourceLocation TAB_ITEMS =
            ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_items.png");

    private static final int REFERENCE_SIZE = 256;
    private static final int PANEL_X = 4;
    private static final int PANEL_Y = 4;
    private static final int CELL_X = 8;
    private static final int CELL_Y = 17;
    private static final int CELL = 18;

    public static final Palette VANILLA = new Palette(0xFFC6C6C6, 0xFF8B8B8B, 0xFF373737, 0xFFFFFFFF, false);

    private static Palette cached;

    private MenuPixels() {}

    public static void invalidate() {
        cached = null;
    }

    public static Palette palette() {
        Palette snapshot = cached;
        if (snapshot == null) {
            snapshot = sample();
            cached = snapshot;
        }
        return snapshot;
    }

    public static int shadow() {
        return palette().shadow();
    }

    public static int highlight() {
        return palette().highlight();
    }

    private static Palette sample() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return VANILLA;
        }
        try (InputStream in = minecraft.getResourceManager().open(TAB_ITEMS);
                NativeImage image = NativeImage.read(in)) {
            float scale = image.getWidth() / (float) REFERENCE_SIZE;
            if (scale <= 0f) {
                return VANILLA;
            }
            int midX = CELL_X + CELL / 2;
            Palette sampled = new Palette(
                    at(image, scale, PANEL_X, PANEL_Y),
                    at(image, scale, midX, CELL_Y + CELL / 2),
                    at(image, scale, midX, CELL_Y),
                    at(image, scale, midX, CELL_Y + CELL - 1),
                    true);
            createaddonorganizer.LOGGER.debug("[CAO] menu palette from {}: panel={} slot={} shadow={} highlight={}",
                    TAB_ITEMS, hex(sampled.panel()), hex(sampled.slot()), hex(sampled.shadow()),
                    hex(sampled.highlight()));
            return sampled;
        } catch (Exception e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not read {} to match the menu's own colours; using the "
                    + "stock ones", TAB_ITEMS, e);
            return VANILLA;
        }
    }

    private static int at(NativeImage image, float scale, int x, int y) {
        int px = Math.min(image.getWidth() - 1, Math.max(0, Math.round(x * scale)));
        int py = Math.min(image.getHeight() - 1, Math.max(0, Math.round(y * scale)));
        int abgr = image.getPixelRGBA(px, py);
        int a = (abgr >>> 24) & 0xFF;
        int b = (abgr >>> 16) & 0xFF;
        int g = (abgr >>> 8) & 0xFF;
        int r = abgr & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String hex(int argb) {
        return String.format("#%08X", argb);
    }
}
