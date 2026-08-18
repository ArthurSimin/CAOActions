package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.fml.ModList;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class CreateCompat {

    private static Boolean available;

    private CreateCompat() {}

    public static boolean available() {
        if (available == null) {
            available = ModList.get().isLoaded("create") && ModList.get().isLoaded("ponder");
            if (available) {
                createaddonorganizer.LOGGER.info("[CAO] Create detected; menu skin will use Create's own widgets");
            }
        }
        return available;
    }

    public static void beginFrame() {
        if (available()) {
            com.sockywocky.createaddonorganizer.client.create.CreateSkin.beginFrame();
        }
    }

    public static void endFrame() {
        if (available()) {
            com.sockywocky.createaddonorganizer.client.create.CreateSkin.endFrame();
        }
    }

    public static void discardStaleFrame() {
        if (available()) {
            com.sockywocky.createaddonorganizer.client.create.CreateSkin.discardStaleFrame();
        }
    }

    public static boolean rebindFrame() {
        return available() && com.sockywocky.createaddonorganizer.client.create.CreateSkin.rebindFrame();
    }

    public static boolean renderCog(GuiGraphics g, int width, int height, float alpha) {
        if (!available()) {
            return false;
        }
        com.sockywocky.createaddonorganizer.client.create.CreateSkin.renderCog(g, width, height, alpha);
        return true;
    }

    public static boolean tickCog() {
        if (!available()) {
            return false;
        }
        com.sockywocky.createaddonorganizer.client.create.CreateSkin.tickCog();
        return true;
    }

    public static boolean bumpCog(double scrollDeltaY) {
        if (!available()) {
            return false;
        }
        com.sockywocky.createaddonorganizer.client.create.CreateSkin.bumpCog(scrollDeltaY);
        return true;
    }

    public static boolean editIcon(GuiGraphics g, int x, int y, int argb) {
        if (!available()) {
            return false;
        }
        com.sockywocky.createaddonorganizer.client.create.CreateSkin.editIcon(g, x, y, argb);
        return true;
    }

    public static boolean arrowIcon(GuiGraphics g, int centerX, int centerY, float rotation, int argb) {
        if (!available()) {
            return false;
        }
        com.sockywocky.createaddonorganizer.client.create.CreateSkin.arrowIcon(g, centerX, centerY, rotation, argb);
        return true;
    }

    public static boolean box(GuiGraphics g, int x, int y, int width, int height,
            int background, int borderTop, int borderBottom) {
        if (!available()) {
            return false;
        }
        com.sockywocky.createaddonorganizer.client.create.CreateSkin.box(g, x, y, width, height,
                background, borderTop, borderBottom);
        return true;
    }
}
