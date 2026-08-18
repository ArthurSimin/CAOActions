package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.mixin.TooltipMessageAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TextEditorOverlay {

    private static final int BOX_IDLE = 0x40FFFFFF;
    private static final int BOX_EDITED = 0x66FFD966;
    private static final int LABEL_BACKDROP = 0xD0101418;

    private static boolean showBoxes;

    private TextEditorOverlay() {}

    public static boolean enabled() {
        return DevMode.isUnlocked() && Config.devTextEditor();
    }

    public static boolean showingBoxes() {
        return showBoxes && enabled();
    }

    public static void tick() {
        LangEditor.ensureInstalled();
        boolean on = enabled();
        if (!on) {
            showBoxes = false;
        }
        TextProbe.setCapturing(on);
    }

    public static void toggleBoxes() {
        if (!enabled()) {
            return;
        }
        showBoxes = !showBoxes;
        Notice.show(Component.translatable(showBoxes
                ? "createaddonorganizer.textEditor.overlayOn"
                : "createaddonorganizer.textEditor.overlayOff"), showBoxes ? Notice.GREEN : Notice.RED);
    }

    public static void render(GuiGraphics g, Minecraft mc) {
        if (!enabled()) {
            return;
        }
        TextProbe.endFrame();
        if (!showBoxes) {
            return;
        }
        TextProbe.suspend(true);
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 400f);
        try {
            double[] mouse = mousePosition(mc);
            TextProbe.Hit hovered = topmostAt(mouse[0], mouse[1]);
            for (TextProbe.Hit hit : TextProbe.lastFrame()) {
                if (hit.tooltip() || hit == hovered) {
                    continue;
                }
                GlassSkin.outline(g, hit.x() - 1, hit.y() - 1, hit.width() + 2, hit.height() + 2,
                        LangEditor.isEdited(hit.key()) ? BOX_EDITED : BOX_IDLE);
            }
            if (hovered != null) {
                GlassSkin.outline(g, hovered.x() - 1, hovered.y() - 1, hovered.width() + 2, hovered.height() + 2,
                        GlassSkin.accent() | 0xFF000000);
                drawLabel(g, mc, hovered);
            }
        } finally {
            g.pose().popPose();
            TextProbe.suspend(false);
        }
    }

    private static void drawLabel(GuiGraphics g, Minecraft mc, TextProbe.Hit hovered) {
        String label = hovered.key();
        int width = mc.font.width(label) + 6;
        int x = Math.max(2, Math.min(hovered.x(), g.guiWidth() - width - 2));
        int y = hovered.y() - mc.font.lineHeight - 3;
        if (y < 2) {
            y = hovered.y() + hovered.height() + 3;
        }
        g.fill(x, y, x + width, y + mc.font.lineHeight + 2, LABEL_BACKDROP);
        GlassSkin.outline(g, x, y, width, mc.font.lineHeight + 2, GlassSkin.accent() | 0xFF000000);
        g.drawString(mc.font, label, x + 3, y + 2, 0xFFFFFFFF, false);
    }

    public static void editHovered() {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled() || mc.screen instanceof TextEditScreen) {
            return;
        }
        double[] mouse = mousePosition(mc);
        List<String> keys = candidates(mc, mouse[0], mouse[1]);
        if (keys.isEmpty()) {
            Notice.show(Component.translatable("createaddonorganizer.textEditor.nothingHere"), Notice.RED);
            return;
        }
        mc.setScreen(new TextEditScreen(mc.screen, keys, 0));
    }

    private static List<String> candidates(Minecraft mc, double mouseX, double mouseY) {
        Set<String> keys = new LinkedHashSet<>();
        List<TextProbe.Hit> under = new ArrayList<>();
        List<TextProbe.Hit> tooltips = new ArrayList<>();
        for (TextProbe.Hit hit : TextProbe.lastFrame()) {
            if (hit.tooltip()) {
                tooltips.add(hit);
            } else if (hit.contains(mouseX, mouseY)) {
                under.add(hit);
            }
        }
        under.sort(Comparator.comparingInt(hit -> hit.width() * hit.height()));
        for (TextProbe.Hit hit : under) {
            keys.add(hit.key());
        }
        if (mc.screen != null) {
            collectHoveredTooltips(mc.screen.children(), mouseX, mouseY, keys);
        }
        for (TextProbe.Hit hit : tooltips) {
            keys.add(hit.key());
        }
        return List.copyOf(keys);
    }

    private static void collectHoveredTooltips(List<? extends GuiEventListener> children, double mouseX,
            double mouseY, Set<String> out) {
        for (GuiEventListener child : children) {
            if (child instanceof AbstractWidget widget) {
                if (!widget.visible || !widget.isMouseOver(mouseX, mouseY)) {
                    continue;
                }
                Tooltip tooltip = widget.getTooltip();
                if (tooltip instanceof TooltipMessageAccessor accessor) {
                    TextProbe.collectKeys(accessor.createaddonorganizer$message(), out);
                }
                TextProbe.collectKeys(widget.getMessage(), out);
            } else if (child instanceof ContainerEventHandler container) {
                collectHoveredTooltips(container.children(), mouseX, mouseY, out);
            }
        }
    }

    private static TextProbe.Hit topmostAt(double mouseX, double mouseY) {
        TextProbe.Hit best = null;
        for (TextProbe.Hit hit : TextProbe.lastFrame()) {
            if (hit.tooltip() || !hit.contains(mouseX, mouseY)) {
                continue;
            }
            if (best == null || hit.width() * hit.height() <= best.width() * best.height()) {
                best = hit;
            }
        }
        return best;
    }

    private static double[] mousePosition(Minecraft mc) {
        Screen screen = mc.screen;
        int screenWidth = mc.getWindow().getScreenWidth();
        int screenHeight = mc.getWindow().getScreenHeight();
        double guiWidth = screen != null ? screen.width : mc.getWindow().getGuiScaledWidth();
        double guiHeight = screen != null ? screen.height : mc.getWindow().getGuiScaledHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return new double[] { 0, 0 };
        }
        return new double[] {
                mc.mouseHandler.xpos() * guiWidth / screenWidth,
                mc.mouseHandler.ypos() * guiHeight / screenHeight,
        };
    }
}
