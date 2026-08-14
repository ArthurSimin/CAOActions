package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.ModSounds;

public final class Sfx {

    private static final long GRID_THROTTLE_MS = 45L;
    private static final long SCROLL_THROTTLE_MS = 55L;
    private static final long BLOCKED_THROTTLE_MS = 220L;
    private static final int PICKUP_STEPS = 20;

    private static long lastGridTick;
    private static long lastScrollTick;
    private static long lastBlocked;
    private static boolean scrollDetent;

    private Sfx() {}

    private static boolean thocky() {
        return Config.sfxStyle() == Config.SfxStyle.THOCKY;
    }

    private static void play(SoundEvent sound, float pitch, float volume) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) {
            return;
        }
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void grab() {
        if (!Config.sfxOn(Config.SFX_GRAB)) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.LEVER_CLICK, 0.70f, 0.50f);
            return;
        }
        play(SoundEvents.LEVER_CLICK, 0.90f, 0.35f);
    }

    public static void groupToggle(boolean opened) {
        if (!Config.sfxOn(Config.SFX_SNAP)) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON, opened ? 1.05f : 0.80f, 0.50f);
            return;
        }
        play(SoundEvents.BAMBOO_WOOD_BUTTON_CLICK_ON, opened ? 1.15f : 0.90f, 0.35f);
    }

    public static void release() {
        if (!Config.sfxOn(Config.SFX_RELEASE)) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.LEVER_CLICK, 0.55f, 0.55f);
            return;
        }
        play(SoundEvents.LEVER_CLICK, 0.70f, 0.35f);
    }

    public static void snap() {
        if (!Config.sfxOn(Config.SFX_SNAP)) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.LEVER_CLICK, 0.78f, 0.45f);
            return;
        }
        play(SoundEvents.LEVER_CLICK, 1.5f, 0.30f);
    }

    public static void bin() {
        if (!Config.sfxOn(Config.SFX_BIN_CLOSE)) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.BARREL_CLOSE, 0.80f, 0.55f);
            return;
        }
        play(SoundEvents.BARREL_CLOSE, 1.35f, 0.45f);
    }

    public static void scroll() {
        if (!Config.sfxOn(Config.SFX_SCROLL)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastScrollTick < SCROLL_THROTTLE_MS) {
            return;
        }
        lastScrollTick = now;
        scrollDetent = !scrollDetent;
        float jitter = scrollDetent ? 0.05f : -0.05f;
        if (thocky()) {
            play(SoundEvents.LEVER_CLICK, 1.40f + jitter, 0.12f);
            return;
        }
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.90f + jitter, 0.09f);
    }

    public static void scrolled(double before, double after, boolean scrollable) {
        if (before != after) {
            scroll();
            return;
        }
        if (scrollable && Config.sfxOn(Config.SFX_SCROLL)) {
            scrollDeadEnd();
        }
    }

    private static void scrollDeadEnd() {
        if (!Config.sfxOn(Config.SFX_BLOCKED) || !throttleBlocked()) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.LEVER_CLICK, 0.50f, 0.20f);
            return;
        }
        play(SoundEvents.UI_BUTTON_CLICK.value(), 0.55f, 0.16f);
    }

    public static void denied() {
        if (!Config.sfxOn(Config.SFX_BLOCKED) || !throttleBlocked()) {
            return;
        }
        play(ModSounds.DENIED.get(), thocky() ? 0.88f : 1.00f, 0.40f);
    }

    private static boolean throttleBlocked() {
        long now = System.currentTimeMillis();
        if (now - lastBlocked < BLOCKED_THROTTLE_MS) {
            return false;
        }
        lastBlocked = now;
        return true;
    }

    public static void pickup(int index) {
        if (!Config.sfxOn(Config.SFX_PICKUP)) {
            return;
        }
        int step = Math.min(index, PICKUP_STEPS);
        float pitch = 1.00f + 0.045f * step;
        if (thocky()) {
            play(SoundEvents.ITEM_PICKUP, pitch - 0.20f, 0.30f);
            return;
        }
        play(SoundEvents.ITEM_PICKUP, pitch, 0.22f);
    }

    public static void binHover() {
        if (!Config.sfxOn(Config.SFX_BIN_HOVER)) {
            return;
        }
        if (thocky()) {
            play(SoundEvents.BARREL_OPEN, 1.15f, 0.35f);
            return;
        }
        play(SoundEvents.IRON_TRAPDOOR_OPEN, 1.80f, 0.22f);
    }

    public static void binItem(int index) {
        if (!Config.sfxOn(Config.SFX_BIN_ITEM)) {
            return;
        }
        int step = Math.min(index, 6);
        float pitch = 0.92f + 0.055f * step;
        float volume = 0.42f - 0.025f * step;
        if (thocky()) {
            play(SoundEvents.BUNDLE_INSERT, pitch - 0.20f, volume + 0.10f);
            return;
        }
        play(SoundEvents.BUNDLE_INSERT, pitch, volume);
    }

    public static void gridStep() {
        if (!Config.sfxOn(Config.SFX_GRID_STEP)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastGridTick < GRID_THROTTLE_MS) {
            return;
        }
        lastGridTick = now;
        if (thocky()) {
            play(SoundEvents.LEVER_CLICK, 0.88f, 0.22f);
            return;
        }
        play(SoundEvents.UI_BUTTON_CLICK.value(), 1.7f, 0.18f);
    }
}
