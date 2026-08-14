package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.Util;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class MenuSkin {

    private static final String OWN_SCREEN_PACKAGE = "com.sockywocky.createaddonorganizer.";

    private static final ResourceLocation COG = ResourceLocation.fromNamespaceAndPath(
            createaddonorganizer.MODID, "textures/gui/cog_shadow.png");
    private static final int COG_TEX = 256;
    private static final ResourceLocation MENU_LIST_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    private static final float COG_SIZE = 400f;
    private static final float COG_ALPHA = 0.376f;

    private static final float HOVER_CHASE = 0.6f;
    private static final float CLICK_CHASE = 0.15f;
    private static final float TICKS_PER_SECOND = 20f;

    private static final float COG_STATIC_FORCE = 0.2f;
    private static final float COG_DRAG = 0.3f;
    private static final float COG_SPEED_LIMIT = 10f;
    private static final int COG_BUMP_TICKS = 3;
    private static final float COG_BUMP_FORCE = 5f;

    private static Screen cachedScreen;
    private static boolean cachedOwnScreen;
    private static boolean previousWasOwn;

    private static final List<float[]> cogBumps = new ArrayList<>();
    private static long cogTickMs;

    private static float cogValue;
    private static float cogPreviousValue;
    private static float cogSpeed;

    private static final float BLEND_SECONDS = 0.30f;
    private static float blend = -1f;
    private static long blendNanos;

    private static MenuPalette shownPalette = MenuPalette.CREATE;
    private static MenuPalette targetPalette = MenuPalette.CREATE;
    private static float paletteBlend = 1f;

    private static final int BASE_HUE = 205;
    private static final Map<Integer, Integer> TINT_CACHE = new HashMap<>();
    private static int tintedHue = Integer.MIN_VALUE;

    private static int tintedSaturation = Integer.MIN_VALUE;

    private MenuSkin() {}

    static MenuPalette paletteFor(Config.MenuStyle style) {
        return switch (style) {
            case ESSENTIAL -> MenuPalette.ESSENTIAL;
            case DEFAULT_MODERN -> MenuPalette.MODERN;
            default -> MenuPalette.CREATE;
        };
    }

    public static MenuPalette palette() {
        return MenuPalette.lerp(shownPalette, targetPalette, paletteBlend);
    }

    private static int overlaynoworld() {
        return tint(palette().overlayNoWorld());
    }

    private static int overlayinworld() {
        return tint(palette().overlayInWorld());
    }

    private static int opaquebackground() {
        return tint(palette().opaqueBackground());
    }

    private static int boxbackground(boolean hovered) {
        return tint(hovered ? palette().boxBackgroundHover() : palette().boxBackground());
    }

    private static int idletop() {
        return tint(palette().idleTop());
    }

    private static int idlebottom() {
        return tint(palette().idleBottom());
    }

    private static int hovertop() {
        return tint(palette().hoverTop());
    }

    private static int hoverbottom() {
        return tint(palette().hoverBottom());
    }

    private static int disabledtop() {
        return palette().disabledTop();
    }

    private static int disabledbottom() {
        return palette().disabledBottom();
    }

    private static int clicktop() {
        return palette().clickTop();
    }

    private static int clickbottom() {
        return palette().clickBottom();
    }

    private static int text() {
        return tint(palette().text());
    }

    private static int texthover() {
        return palette().textHover();
    }

    private static int textoff() {
        return palette().textOff();
    }

    private static int header() {
        return tint(palette().header());
    }

    private static int hint() {
        return tint(palette().hint());
    }

    private static int scrolltrack() {
        return palette().scrollTrack();
    }

    private static int scrollthumb() {
        return tint(palette().scrollThumb());
    }

    private static int listbackground() {
        return palette().listBackground();
    }

    private static int rule() {
        return tint(palette().rule());
    }

    private static int accentcolor() {
        return tint(palette().accent());
    }

    private static int selectedbackground() {
        return tint(palette().selectedBackground());
    }

    private static int rowstripe() {
        return palette().rowStripe();
    }

    public static int tint(int argb) {
        int hue = Config.menuAccentHue();
        int saturation = Config.menuAccentSaturation();
        if (hue != tintedHue || saturation != tintedSaturation) {
            TINT_CACHE.clear();
            tintedHue = hue;
            tintedSaturation = saturation;
        }
        if (hue == BASE_HUE && saturation == Config.MENU_SATURATION_DEFAULT) {
            return argb;
        }
        Integer cached = TINT_CACHE.get(argb);
        if (cached != null) {
            return cached;
        }
        int shifted = hue == Config.MENU_ACCENT_NEUTRAL
                ? desaturate(argb)
                : regrade(argb, hue - BASE_HUE, saturation / 100f);
        TINT_CACHE.put(argb, shifted);
        return shifted;
    }

    public static float accentSaturationFactor() {
        return Config.menuAccentSaturation() / 100f;
    }

    private static int desaturate(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int value = Math.max(r, Math.max(g, b));
        return (argb & 0xFF000000) | (value << 16) | (value << 8) | value;
    }

    private static int regrade(int argb, int degrees, float saturationScale) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        if (delta <= 0.0001f) {
            return argb;
        }
        float h;
        if (max == r) {
            h = ((g - b) / delta) % 6f;
        } else if (max == g) {
            h = (b - r) / delta + 2f;
        } else {
            h = (r - g) / delta + 4f;
        }
        h = (h * 60f + degrees) % 360f;
        if (h < 0f) {
            h += 360f;
        }
        float s = Mth.clamp(delta / max * saturationScale, 0f, 1f);
        return (argb & 0xFF000000) | (hsvToRgb(h, s, max) & 0x00FFFFFF);
    }

    public static int hsvToRgb(float hue, float saturation, float value) {
        float c = value * saturation;
        float x = c * (1f - Math.abs((hue / 60f) % 2f - 1f));
        float m = value - c;
        float r;
        float g;
        float b;
        int segment = (int) (hue / 60f) % 6;
        switch (segment) {
            case 0 -> { r = c; g = x; b = 0f; }
            case 1 -> { r = x; g = c; b = 0f; }
            case 2 -> { r = 0f; g = c; b = x; }
            case 3 -> { r = 0f; g = x; b = c; }
            case 4 -> { r = x; g = 0f; b = c; }
            default -> { r = c; g = 0f; b = x; }
        }
        return 0xFF000000 | (Math.round((r + m) * 255f) << 16)
                | (Math.round((g + m) * 255f) << 8) | Math.round((b + m) * 255f);
    }


    public static void advanceBlend() {
        Config.MenuStyle style = Config.menuStyle();
        float target = style == Config.MenuStyle.DEFAULT ? 0f : 1f;
        long now = System.nanoTime();
        if (!Config.animOn(Config.ANIM_STYLE_BLEND)) {
            blend = target;
            blendNanos = now;
            shownPalette = targetPalette = paletteFor(style);
            paletteBlend = 1f;
            return;
        }
        if (blend < 0f) {
            blend = target;
            blendNanos = now;
            shownPalette = targetPalette = paletteFor(style);
            paletteBlend = 1f;
            return;
        }
        float dt = (now - blendNanos) / 1_000_000_000f;
        blendNanos = now;
        float step = dt / BLEND_SECONDS;
        if (blend != target) {
            blend = blend < target ? Math.min(target, blend + step) : Math.max(target, blend - step);
        }
        advancePalette(style, step);
    }

    private static void advancePalette(Config.MenuStyle style, float step) {
        if (style == Config.MenuStyle.DEFAULT) {
            return;
        }
        MenuPalette wanted = paletteFor(style);
        if (wanted != targetPalette) {
            shownPalette = palette();
            targetPalette = wanted;
            paletteBlend = blend <= 0.002f ? 1f : 0f;
        }
        if (paletteBlend < 1f) {
            paletteBlend = Math.min(1f, paletteBlend + step);
        }
    }

    public static ArrangerSkin arrangerSkin() {
        return new ArrangerSkin(
                boxbackground(false), idletop(), idlebottom(), text(),
                listbackground(), idlebottom(), 0x30000000,
                boxbackground(false), idletop(), selectedbackground(), accentcolor(),
                0x99000000, disabledtop(), scrolltrack(), scrollthumb());
    }

    public static boolean cogEnabled() {
        return palette().cog();
    }

    public static boolean nativeBoxes() {
        return palette().nativeBoxes();
    }

    public static float blend() {
        return blend < 0f ? 0f : blend;
    }

    public static boolean full() {
        return blend >= 0.998f;
    }

    public static boolean active() {
        if (blend() <= 0.002f) {
            return false;
        }
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return false;
        }
        if (screen != cachedScreen) {
            cachedScreen = screen;
            cachedOwnScreen = isOwnScreen(screen);
            previousWasOwn = cachedOwnScreen;
        }
        return cachedOwnScreen;
    }

    private static boolean isOwnScreen(Screen screen) {
        if (screen.getClass().getName().startsWith(OWN_SCREEN_PACKAGE)) {
            return true;
        }
        if (DevMode.isOurScreen(screen)) {
            return true;
        }
        return previousWasOwn && screen instanceof ConfirmScreen;
    }

    public static boolean ownScreen(Screen screen) {
        return screen != null && isOwnScreen(screen);
    }

    public static boolean isModConfigScreen(Screen screen) {
        return DevMode.isOurScreen(screen);
    }

    public static boolean transparent() {
        return Config.menuStyleTransparent();
    }

    public static void endFrame() {
        CreateCompat.endFrame();
    }

    public static void overlay(GuiGraphics g, int width, int height, float alpha) {
        if (!transparent()) {
            g.fill(0, 0, width, height, fade(opaquebackground(), alpha));
            return;
        }
        g.fill(0, 0, width, height, fade(
                Minecraft.getInstance().level == null ? overlaynoworld() : overlayinworld(), alpha));
    }

    public static boolean fancyTitle() {
        return palette().titleBand();
    }

    public static void titleBand(GuiGraphics g, int screenWidth, int top, int height, float alpha) {
        if (!palette().titleBand()) {
            return;
        }
        int bottom = top + height;
        g.fill(-5, top, screenWidth + 5, bottom, fade(tint(palette().titleBandBackground()), alpha));
        g.fill(-5, top, screenWidth + 5, top + 1, fade(palette().titleBandTop(), alpha));
        g.fill(-5, bottom - 1, screenWidth + 5, bottom, fade(palette().titleBandBottom(), alpha));
    }

    public static int titleGradient(float frac) {
        float f = Mth.clamp(frac, 0f, 1f);
        int from = f < 0.5f ? palette().titleTop() : palette().titleMid();
        int to = f < 0.5f ? palette().titleMid() : palette().titleBottom();
        return tint(mixColor(from, to, f < 0.5f ? f * 2f : (f - 0.5f) * 2f));
    }

    public static void tickCog() {
        if (!active()) {
            return;
        }
        cogTickMs = Util.getMillis();
        if (CreateCompat.tickCog()) {
            return;
        }
        cogPreviousValue = cogValue;
        float impulse = COG_STATIC_FORCE - COG_DRAG * cogSpeed;
        for (Iterator<float[]> it = cogBumps.iterator(); it.hasNext();) {
            float[] bump = it.next();
            impulse += bump[1];
            if (--bump[0] <= 0f) {
                it.remove();
            }
        }
        cogSpeed = Mth.clamp(cogSpeed + impulse, -COG_SPEED_LIMIT, COG_SPEED_LIMIT);
        cogValue += cogSpeed;
    }

    public static float cogPartialTick() {
        return Mth.clamp((Util.getMillis() - cogTickMs) / 50f, 0f, 1f);
    }

    public static void bumpCog(double scrollDeltaY) {
        if (!active() || scrollDeltaY == 0) {
            return;
        }
        if (CreateCompat.bumpCog(scrollDeltaY)) {
            return;
        }
        cogBumps.add(new float[] {COG_BUMP_TICKS, (float) (-scrollDeltaY * COG_BUMP_FORCE) / COG_BUMP_TICKS});
    }

    public static void cog(GuiGraphics g, int width, int height, float alpha) {
        if (!palette().cog()) {
            return;
        }
        if (CreateCompat.renderCog(g, width, height, alpha)) {
            return;
        }
        float size = COG_SIZE;
        float angle = Mth.lerp(cogPartialTick(), cogPreviousValue, cogValue);

        RenderSystem.enableBlend();
        g.pose().pushPose();
        g.pose().translate(width / 2f, height / 2f, 0f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
        g.pose().scale(size / COG_TEX, size / COG_TEX, 1f);
        g.setColor(0f, 0f, 0f, COG_ALPHA * alpha);
        g.blit(COG, -COG_TEX / 2, -COG_TEX / 2, 0f, 0f, COG_TEX, COG_TEX, COG_TEX, COG_TEX);
        g.setColor(1f, 1f, 1f, 1f);
        g.pose().popPose();
    }

    public static void button(GuiGraphics g, ButtonAnim anim, int x, int y, int width, int height,
            boolean enabled, boolean hovered, float alpha) {
        anim.update(enabled, hovered);
        int background = fade(boxbackground(hovered), alpha);
        int top = fade(anim.top, alpha);
        int bottom = fade(anim.bottom, alpha);
        if (nativeBoxes() && CreateCompat.box(g, x, y, width, height, background, top, bottom)) {
            return;
        }
        g.fill(x, y, x + width, y + height, background);
        gradientOutline(g, x, y, width, height, top, bottom);
    }

    private static int stateTop(boolean enabled, boolean hovered) {
        return !enabled ? disabledtop() : hovered ? hovertop() : idletop();
    }

    private static int stateBottom(boolean enabled, boolean hovered) {
        return !enabled ? disabledbottom() : hovered ? hoverbottom() : idlebottom();
    }

    public static int mixColor(int from, int to, float f) {
        int a = Mth.lerpInt(f, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = Mth.lerpInt(f, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int gr = Mth.lerpInt(f, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = Mth.lerpInt(f, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    public static final class ButtonAnim {

        private int previousTop;
        private int previousBottom;
        private int targetTop;
        private int targetBottom;
        private int top;
        private int bottom;

        private float remaining;
        private float chase = HOVER_CHASE;
        private boolean wasHovered;
        private boolean wasEnabled = true;
        private boolean started;
        private long lastNanos;

        public void update(boolean enabled, boolean hovered) {
            int stateTop = stateTop(enabled, hovered);
            int stateBottom = stateBottom(enabled, hovered);
            long now = System.nanoTime();

            if (!Config.animOn(Config.ANIM_BUTTON_HOVER)) {
                started = true;
                lastNanos = now;
                wasHovered = hovered;
                wasEnabled = enabled;
                remaining = 0f;
                previousTop = targetTop = top = stateTop;
                previousBottom = targetBottom = bottom = stateBottom;
                return;
            }
            if (!started) {
                started = true;
                lastNanos = now;
                wasHovered = hovered;
                wasEnabled = enabled;
                previousTop = targetTop = top = stateTop;
                previousBottom = targetBottom = bottom = stateBottom;
                return;
            }

            float dt = (now - lastNanos) / 1_000_000_000f;
            lastNanos = now;

            if (hovered != wasHovered || enabled != wasEnabled) {
                wasHovered = hovered;
                wasEnabled = enabled;
                begin(stateTop, stateBottom, HOVER_CHASE);
            } else {
                targetTop = stateTop;
                targetBottom = stateBottom;
            }

            if (remaining > 0f) {
                remaining *= (float) Math.pow(1f - chase, TICKS_PER_SECOND * dt);
                if (remaining < 0.004f) {
                    remaining = 0f;
                }
            }
            float mix = 1f - remaining;
            top = mixColor(previousTop, targetTop, mix);
            bottom = mixColor(previousBottom, targetBottom, mix);
        }

        public void click(boolean enabled, boolean hovered) {
            top = clicktop();
            bottom = clickbottom();
            begin(stateTop(enabled, hovered), stateBottom(enabled, hovered), CLICK_CHASE);
        }

        private void begin(int newTargetTop, int newTargetBottom, float newChase) {
            previousTop = top;
            previousBottom = bottom;
            targetTop = newTargetTop;
            targetBottom = newTargetBottom;
            chase = newChase;
            remaining = 1f - newChase;
        }
    }

    public static int buttonTextColor(boolean enabled, boolean hovered) {
        return !enabled ? textoff() : hovered ? texthover() : text();
    }

    public static void editBox(GuiGraphics g, int x, int y, int width, int height, boolean focused, float alpha) {
        int top = fade(focused ? hovertop() : idletop(), alpha);
        int bottom = fade(focused ? hoverbottom() : idlebottom(), alpha);
        int background = fade(boxbackground(false), alpha);
        if (nativeBoxes() && CreateCompat.box(g, x, y, width, height, background, top, bottom)) {
            return;
        }
        g.fill(x, y, x + width, y + height, background);
        gradientOutline(g, x, y, width, height, top, bottom);
    }

    public static void listBackground(GuiGraphics g, int x, int y, int width, int height, float alpha) {
        g.fill(x, y, x + width, y + height, fade(listbackground(), alpha));
    }

    public static void contrastArea(GuiGraphics g, int x, int y, int width, int height) {
        boolean skinned = active();
        float blend = blend();
        if (!skinned || !full()) {
            if (skinned) {
                g.setColor(1f, 1f, 1f, 1f - blend);
            }
            Screen.renderMenuBackgroundTexture(g, Minecraft.getInstance().level == null
                    ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND, x, y, 0f, 0f, width, height);
            if (skinned) {
                g.setColor(1f, 1f, 1f, 1f);
            }
        }
        if (skinned) {
            listBackground(g, x, y, width, height, blend);
            int rule = ruleColor(0xFFFFFFFF);
            g.fill(x, y - 1, x + width, y, rule);
            g.fill(x, y + height, x + width, y + height + 1, rule);
        }
    }

    public static void scrollTrack(GuiGraphics g, int x, int y, int width, int height, float alpha) {
        g.fill(x, y, x + width, y + height, fade(scrolltrack(), alpha));
    }

    public static void scrollThumb(GuiGraphics g, int x, int y, int width, int height, float alpha) {
        g.fillGradient(x, y, x + width, y + height, fade(scrollthumb(), alpha),
                fade(mixColor(scrollthumb(), scrolltrack(), 0.25f), alpha));
    }

    public static void gradientOutline(GuiGraphics g, int x, int y, int width, int height,
            int top, int bottom) {
        g.fill(x, y, x + width, y + 1, top);
        g.fill(x, y + height - 1, x + width, y + height, bottom);
        g.fillGradient(x, y + 1, x + 1, y + height - 1, top, bottom);
        g.fillGradient(x + width - 1, y + 1, x + width, y + height - 1, top, bottom);
    }

    public static int fade(int argb, float alpha) {
        if (alpha >= 1f) {
            return argb;
        }
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    public static int titleColor(int fallback) {
        return active() ? mixColor(fallback, header(), blend()) : fallback;
    }

    public static int bodyColor(int fallback) {
        return active() ? mixColor(fallback, hint(), blend()) : fallback;
    }

    public static int mutedColor(int fallback) {
        return active() ? mixColor(fallback, textoff(), blend()) : fallback;
    }

    public static int scrollTrackColor(int fallback) {
        return active() ? mixColor(fallback, scrolltrack(), blend()) : fallback;
    }

    public static int scrollThumbColor(int fallback) {
        return active() ? mixColor(fallback, scrollthumb(), blend()) : fallback;
    }

    private static final Set<AbstractButton> EDIT_BUTTONS =
            Collections.newSetFromMap(new WeakHashMap<>());

    public static <T extends AbstractButton> T markEdit(T button) {
        EDIT_BUTTONS.add(button);
        return button;
    }

    public static boolean isEditButton(AbstractButton button) {
        return EDIT_BUTTONS.contains(button);
    }

    public static boolean editIcon(GuiGraphics g, int centerX, int centerY, int argb) {
        return palette().editIcon() && CreateCompat.editIcon(g, centerX - 8, centerY - 8, argb);
    }

    public static boolean arrowIcon(GuiGraphics g, int centerX, int centerY, float rotation, int argb) {
        return CreateCompat.arrowIcon(g, centerX, centerY, rotation, argb);
    }

    public static int accentSwatch() {
        return active() ? accentcolor() : tint(GlassSkin.DEFAULT_ACCENT);
    }

    public static int booleanValueColor(boolean on) {
        return on ? tint(palette().booleanOn()) : palette().booleanOff();
    }

    public static int accent(int fallback) {
        if (!active()) {
            return tint(fallback);
        }
        int target = (fallback & 0xFF000000) | (accentcolor() & 0x00FFFFFF);
        return mixColor(fallback, target, blend());
    }

    public static int ruleColor(int fallback) {
        return active() ? mixColor(fallback, rule(), blend()) : fallback;
    }

    public static int stripeColor(int fallback) {
        return active() ? mixColor(fallback, rowstripe(), blend()) : fallback;
    }
}
