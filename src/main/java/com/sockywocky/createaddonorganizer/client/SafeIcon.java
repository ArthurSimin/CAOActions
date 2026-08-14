package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SafeIcon {
    private static final Set<Object> BROKEN = ConcurrentHashMap.newKeySet();
    private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);
    private static final int FULL_BRIGHT = 15728880;

    private record Pending(ItemStack stack, BakedModel model, int x, int y) {}

    private static final List<Pending> LIT = new ArrayList<>();
    private static final List<Pending> FLAT = new ArrayList<>();
    private static boolean batching;

    private SafeIcon() {}

    public static ItemStack of(CreativeModeTab tab) {
        if (tab == null || BROKEN.contains(tab)) {
            return ItemStack.EMPTY;
        }
        try {
            return tab.getIconItem();
        } catch (Throwable t) {
            if (BROKEN.add(tab)) {
                createaddonorganizer.LOGGER.warn("[CAO] creative tab {} threw while resolving its icon; hiding it",
                        BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab), t);
            }
            return ItemStack.EMPTY;
        }
    }

    private static void renderPlaceholder(GuiGraphics g, ItemStack icon, int x, int y) {
        int hash = BuiltInRegistries.ITEM.getKey(icon.getItem()).hashCode();
        int tint = 0xFF000000 | (hash & 0x003F3F3F) | 0x00404850;
        g.fill(x + 2, y + 2, x + 14, y + 14, tint);
        g.fill(x + 2, y + 2, x + 14, y + 3, 0x40FFFFFF);
        g.fill(x + 2, y + 13, x + 14, y + 14, 0x40000000);
    }

    public static void render(GuiGraphics g, ItemStack icon, int x, int y) {
        if (icon.isEmpty()) {
            return;
        }
        RenderProfiler.begin(RenderProfiler.ITEM);
        try {
            renderDirect(g, icon, x, y);
        } finally {
            RenderProfiler.end(RenderProfiler.ITEM);
        }
    }

    private static void renderDirect(GuiGraphics g, ItemStack icon, int x, int y) {
        if (Minecraft.getInstance().player == null) {
            renderWithoutPlayer(g, icon, x, y);
            return;
        }
        if (BROKEN.contains(icon.getItem())) {
            g.renderItem(FALLBACK, x, y);
            return;
        }
        try {
            g.renderItem(icon, x, y);
        } catch (Throwable t) {
            markBroken(icon, t);
            g.renderItem(FALLBACK, x, y);
        }
    }

    public static void beginBatch() {
        batching = true;
        LIT.clear();
        FLAT.clear();
    }

    public static void batched(GuiGraphics g, ItemStack icon, int x, int y) {
        if (icon.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!batching || mc.player == null || BROKEN.contains(icon.getItem())) {
            render(g, icon, x, y);
            return;
        }
        BakedModel model;
        try {
            model = mc.getItemRenderer().getModel(icon, mc.level, mc.player, 0);
        } catch (Throwable t) {
            markBroken(icon, t);
            render(g, icon, x, y);
            return;
        }
        if (model.isCustomRenderer()) {
            render(g, icon, x, y);
            return;
        }
        (model.usesBlockLight() ? LIT : FLAT).add(new Pending(icon, model, x, y));
    }

    public static void endBatch(GuiGraphics g) {
        batching = false;
        RenderProfiler.begin(RenderProfiler.ITEM);
        drawGroup(g, LIT, false);
        drawGroup(g, FLAT, true);
        RenderProfiler.end(RenderProfiler.ITEM);
    }

    private static void drawGroup(GuiGraphics g, List<Pending> group, boolean flat) {
        if (group.isEmpty()) {
            return;
        }
        if (flat) {
            Lighting.setupForFlatItems();
        }
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        MultiBufferSource.BufferSource buffers = g.bufferSource();
        PoseStack pose = g.pose();
        for (Pending pending : group) {
            pose.pushPose();
            pose.translate(pending.x() + 8f, pending.y() + 8f, 150f);
            pose.scale(16f, -16f, 16f);
            try {
                renderer.render(pending.stack(), ItemDisplayContext.GUI, false, pose, buffers,
                        FULL_BRIGHT, OverlayTexture.NO_OVERLAY, pending.model());
            } catch (Throwable t) {
                markBroken(pending.stack(), t);
            }
            pose.popPose();
        }
        g.flush();
        if (flat) {
            Lighting.setupFor3DItems();
        }
        group.clear();
    }

    private static void markBroken(ItemStack icon, Throwable t) {
        if (BROKEN.add(icon.getItem())) {
            createaddonorganizer.LOGGER.warn("[CAO] item icon {} threw while rendering; using fallback",
                    BuiltInRegistries.ITEM.getKey(icon.getItem()), t);
        }
    }

    private static void renderWithoutPlayer(GuiGraphics g, ItemStack icon, int x, int y) {
        if (BROKEN.contains(icon.getItem())) {
            renderPlaceholder(g, icon, x, y);
            return;
        }
        PoseStack pose = g.pose();
        PoseStack.Pose marker = pose.last();
        try {
            g.renderFakeItem(icon, x, y);
        } catch (Throwable t) {
            while (pose.last() != marker) {
                pose.popPose();
            }
            if (BROKEN.add(icon.getItem())) {
                createaddonorganizer.LOGGER.warn("[CAO] item icon {} needs a player to render; using a placeholder",
                        BuiltInRegistries.ITEM.getKey(icon.getItem()));
            }
            renderPlaceholder(g, icon, x, y);
        }
    }
}
