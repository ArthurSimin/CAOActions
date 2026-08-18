package com.sockywocky.createaddonorganizer.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class IconAtlas {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, "icon_atlas");

    private static final int ATLAS = 2048;
    private static final int ICON = 16;
    private static final int UPLOAD_BUDGET = 24;
    private static final int COLOR_AND_DEPTH = 16640;
    private static final float QUAD_Z = 150f;

    private static RenderTarget target;
    private static boolean registered;
    private static boolean failed;

    private static int tile;
    private static int cols;
    private static int capacity;
    private static int builtForScale;

    private static final Map<Item, Integer> slotOf = new HashMap<>();
    private static final Map<Item, Long> lastUsed = new HashMap<>();
    private static final Map<Item, ItemStack> pending = new LinkedHashMap<>();
    private static int nextSlot;
    private static long useClock;

    private static int[] quads = new int[512 * 3];
    private static int quadCount;

    private IconAtlas() {}

    public static void invalidate() {
        slotOf.clear();
        lastUsed.clear();
        pending.clear();
        nextSlot = 0;
        quadCount = 0;
        if (target != null) {
            try {
                target.destroyBuffers();
            } catch (Throwable ignored) {
            }
            target = null;
        }
        builtForScale = 0;
    }

    private static boolean enabled() {
        return !failed && Config.cacheItemIcons() && RenderSystem.isOnRenderThread();
    }

    private static void fail(Throwable t) {
        if (!failed) {
            failed = true;
            createaddonorganizer.LOGGER.warn("[CAO] icon cache disabled after a render failure", t);
        }
        invalidate();
    }

    private static final int[] boundViewport = new int[4];
    private static int boundFramebuffer = -1;

    private static void captureTarget() {
        boundFramebuffer = GlStateManager.getBoundFramebuffer();
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, boundViewport);
    }

    private static void restoreTarget() {
        if (boundFramebuffer < 0) {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            return;
        }
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFramebuffer);
        GlStateManager._viewport(boundViewport[0], boundViewport[1], boundViewport[2], boundViewport[3]);
        boundFramebuffer = -1;
    }

    private static boolean ensure() {
        int scale = Math.max(1, Math.min(4, (int) Math.ceil(Minecraft.getInstance().getWindow().getGuiScale())));
        if (target != null && builtForScale == scale) {
            return true;
        }
        if (target != null) {
            invalidate();
        }
        tile = ICON * scale;
        cols = ATLAS / tile;
        capacity = cols * cols;
        captureTarget();
        target = new TextureTarget(ATLAS, ATLAS, true, Minecraft.ON_OSX);
        target.setClearColor(0f, 0f, 0f, 0f);
        target.setFilterMode(9728);
        target.clear(Minecraft.ON_OSX);
        restoreTarget();
        if (!registered) {
            registered = true;
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new AtlasTexture());
        }
        builtForScale = scale;
        return true;
    }

    private static int allocate(Item item) {
        if (nextSlot < capacity) {
            return nextSlot++;
        }
        Item victim = null;
        long oldest = Long.MAX_VALUE;
        for (Map.Entry<Item, Long> entry : lastUsed.entrySet()) {
            if (entry.getValue() < oldest) {
                oldest = entry.getValue();
                victim = entry.getKey();
            }
        }
        if (victim == null || victim == item) {
            return -1;
        }
        Integer slot = slotOf.remove(victim);
        lastUsed.remove(victim);
        return slot == null ? -1 : slot;
    }

    public static void uploadPending(GuiGraphics g) {
        if (!enabled() || pending.isEmpty()) {
            return;
        }
        try {
            if (!ensure()) {
                return;
            }
            int budget = UPLOAD_BUDGET;
            var iterator = pending.entrySet().iterator();
            while (iterator.hasNext() && budget-- > 0) {
                Map.Entry<Item, ItemStack> entry = iterator.next();
                iterator.remove();
                int slot = allocate(entry.getKey());
                if (slot < 0) {
                    continue;
                }
                renderTile(g, entry.getValue(), slot);
                slotOf.put(entry.getKey(), slot);
                lastUsed.put(entry.getKey(), ++useClock);
            }
        } catch (Throwable t) {
            fail(t);
        }
    }

    private static void renderTile(GuiGraphics g, ItemStack stack, int slot) {
        int tx = (slot % cols) * tile;
        int ty = (slot / cols) * tile;

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting oldSorting = RenderSystem.getVertexSorting();

        captureTarget();
        target.bindWrite(true);

        RenderSystem.enableScissor(tx, ATLAS - ty - tile, tile, tile);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
        RenderSystem.clear(COLOR_AND_DEPTH, Minecraft.ON_OSX);
        RenderSystem.disableScissor();

        RenderSystem.setProjectionMatrix(
                new Matrix4f().setOrtho(0f, ATLAS, ATLAS, 0f, 1000f, 21000f),
                VertexSorting.ORTHOGRAPHIC_Z);

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        modelView.translate(0f, 0f, -11000f);
        RenderSystem.applyModelViewMatrix();

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.setIdentity();
        pose.translate(tx, ty, 0f);
        float scale = tile / (float) ICON;
        pose.scale(scale, scale, scale);
        try {
            SafeIcon.render(g, stack, 0, 0);
            g.flush();
        } finally {
            pose.popPose();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
            restoreTarget();
        }
    }

    public static boolean queue(ItemStack stack, int x, int y) {
        if (!enabled() || stack.isEmpty()) {
            return false;
        }
        try {
            if (!ensure()) {
                return false;
            }
            Item item = stack.getItem();
            Integer slot = slotOf.get(item);
            if (slot == null) {
                if (pending.size() < capacity) {
                    pending.putIfAbsent(item, stack);
                }
                return false;
            }
            lastUsed.put(item, ++useClock);
            if (quadCount * 3 + 3 > quads.length) {
                int[] grown = new int[quads.length * 2];
                System.arraycopy(quads, 0, grown, 0, quads.length);
                quads = grown;
            }
            int base = quadCount * 3;
            quads[base] = x;
            quads[base + 1] = y;
            quads[base + 2] = slot;
            quadCount++;
            return true;
        } catch (Throwable t) {
            fail(t);
            return false;
        }
    }

    public static void flushQuads(GuiGraphics g) {
        if (quadCount == 0) {
            return;
        }
        try {
            g.flush();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEXTURE);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();

            Matrix4f matrix = g.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            for (int i = 0; i < quadCount; i++) {
                int base = i * 3;
                float x1 = quads[base];
                float y1 = quads[base + 1];
                float x2 = x1 + ICON;
                float y2 = y1 + ICON;
                int slot = quads[base + 2];
                float tx = (slot % cols) * tile;
                float ty = (slot / cols) * tile;
                float u1 = tx / ATLAS;
                float u2 = (tx + tile) / ATLAS;
                float v1 = 1f - ty / ATLAS;
                float v2 = 1f - (ty + tile) / ATLAS;
                buffer.addVertex(matrix, x1, y1, QUAD_Z).setUv(u1, v1);
                buffer.addVertex(matrix, x1, y2, QUAD_Z).setUv(u1, v2);
                buffer.addVertex(matrix, x2, y2, QUAD_Z).setUv(u2, v2);
                buffer.addVertex(matrix, x2, y1, QUAD_Z).setUv(u2, v1);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
            RenderSystem.disableBlend();
        } catch (Throwable t) {
            fail(t);
        } finally {
            quadCount = 0;
        }
    }

    private static final class AtlasTexture extends AbstractTexture {
        @Override
        public void load(ResourceManager resourceManager) {
        }

        @Override
        public int getId() {
            return target == null ? 0 : target.getColorTextureId();
        }

        @Override
        public void releaseId() {
        }

        @Override
        public void close() {
        }
    }
}
