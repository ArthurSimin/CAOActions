package com.sockywocky.createaddonorganizer.client.simulated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.BannerAnimation;
import com.sockywocky.createaddonorganizer.client.BannerTextures;
import com.sockywocky.createaddonorganizer.client.ColorSpec;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class NativeSections {
    private static final float NATIVE_SPLIT = 5f / 9f;


    private static int galleryReported = -1;

    private NativeSections() {}

    public static void adoptAll() {
        List<SimulatedHub.NativeSection> pending = SimulatedHub.nativeSections();
        if (pending.isEmpty()) {
            return;
        }
        boolean recopy = Config.nativeSeedOutdated();
        List<ResourceLocation> seeded = new ArrayList<>();
        for (SimulatedHub.NativeSection section : pending) {
            if (recopy || !Config.isNativeSeeded(section.id())) {
                seed(section);
                Config.markNativeSeeded(section.id());
                seeded.add(section.id());
            }
            SimulatedHub.adopt(section.id(), titleFor(section));
        }
        if (!seeded.isEmpty()) {
            createaddonorganizer.LOGGER.info("[CAO] copied in the look of {} section(s) drawn by their own mod: {}",
                    seeded.size(), seeded);
        }
    }

    public static void release(ResourceLocation id) {
        SimulatedHub.releaseAdopted(id);
        Config.purgeSectionConfig(id);
        Config.clearNativeSeeded(id);
    }

    public static void readopt(ResourceLocation id) {
        for (SimulatedHub.NativeSection section : SimulatedHub.nativeSections()) {
            if (section.id().equals(id)) {
                seed(section);
                Config.markNativeSeeded(id);
                SimulatedHub.adopt(id, titleFor(section));
                return;
            }
        }
    }

    public static List<String> galleryRefs() {
        Set<String> refs = new LinkedHashSet<>();
        List<ResourceLocation> sprites = SimulatedHub.nativeSprites();
        List<ResourceLocation> unusable = new ArrayList<>();
        for (ResourceLocation sprite : sprites) {
            String ref = BannerTextures.nativeRef(sprite);
            if (BannerTextures.resolve(ref) != null) {
                refs.add(ref);
            } else {
                unusable.add(sprite);
            }
        }
        if (galleryReported != sprites.size()) {
            galleryReported = sprites.size();
            createaddonorganizer.LOGGER.info("[CAO] banner gallery: {} of {} sprite(s) drawn by their own mod are"
                    + " usable{}", refs.size(), sprites.size(),
                    unusable.isEmpty() ? "" : ", unusable: " + unusable);
        }
        return List.copyOf(refs);
    }

    private static void seed(SimulatedHub.NativeSection section) {
        seedLook(section.id(), section);
    }

    public static boolean seedLookOfOwningMod(ResourceLocation id) {
        SimulatedHub.NativeSection style = SimulatedHub.houseStyleFor(id.getNamespace());
        if (style == null) {
            return false;
        }
        seedLook(id, style);
        createaddonorganizer.LOGGER.info("[CAO] {} takes the banner and colours {} draws for its own section {}",
                id, id.getNamespace(), style.id());
        return true;
    }

    private static void seedLook(ResourceLocation id, SimulatedHub.NativeSection style) {
        seedBanner(id, style.sprite(), style.animateOnHover());
        Config.setTextColor(id, ColorSpec.solid(style.textColor()));
        Config.setTextSecondaryColor(id, ColorSpec.solid(style.secondaryTextColor()));
        Config.setTwoToneSplit(id, NATIVE_SPLIT);
        Config.setTitleTextShadow(id, true);
        seedTitleBox(id, style.backgroundColor());
    }

    private static void seedBanner(ResourceLocation id, ResourceLocation sprite, boolean animateOnHover) {
        if (sprite == null) {
            createaddonorganizer.LOGGER.warn("[CAO] section {} draws no banner sprite, so it keeps a plain one", id);
            return;
        }
        String ref = BannerTextures.nativeRef(sprite);
        ResourceLocation texture = BannerTextures.resolve(ref);
        if (texture == null) {
            createaddonorganizer.LOGGER.warn("[CAO] could not reuse the banner sprite {} for section {}", sprite, id);
            return;
        }
        Config.setSectionBanner(id, ref);
        Config.addExtraPoolEntry(id, ref);
        if (BannerAnimation.isAnimatable(texture)) {
            Config.setAnimatedBanner(texture, BannerTextures.nativeFrameTicks(sprite));
            Config.setBannerAlwaysAnimates(texture, !animateOnHover);
            BannerAnimation.invalidate(texture);
        }
    }

    private static void seedTitleBox(ResourceLocation id, int background) {
        boolean visible = (background >>> 24) != 0;
        Config.setTintedTextBoxFor(id, visible);
        if (!visible) {
            return;
        }
        Config.setBoxColor(id, background);
        Config.setBoxDarken(id, 0f);
        Config.setBoxOpacity(id, 1f);
        Config.clearSectionBoxTexture(id);
    }

    private static Component titleFor(SimulatedHub.NativeSection section) {
        String override = Config.sectionNameOverride(section.id());
        return override != null ? Component.literal(override) : section.title();
    }
}

