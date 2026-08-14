package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.PackDefaults;
import com.sockywocky.createaddonorganizer.client.simulated.NativeSections;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

public final class ShippedBanners {
    private static final String MARKER = "banner";
    private static final String ROOT = "textures/gui";
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 164;
    private static final int[] FALLBACK_FRAME_HEIGHTS = {18, 19, 17};

    private static final Map<String, List<ResourceLocation>> BY_NAMESPACE = new LinkedHashMap<>();
    private static final Set<String> SEEDED_REFS = new LinkedHashSet<>();
    private static final Set<String> SCANNED = new LinkedHashSet<>();

    private static boolean swept = false;

    private ShippedBanners() {}

    public static void invalidate() {
        BY_NAMESPACE.clear();
        SEEDED_REFS.clear();
        SCANNED.clear();
        swept = false;
    }

    public static boolean seedFor(ResourceLocation tabId) {
        if (Config.isNativeSeeded(tabId) && !Config.nativeSeedOutdated()) {
            return false;
        }
        if (PackDefaults.bannerFor(tabId) != null || PackDefaults.nameFor(tabId) != null) {
            return false;
        }
        if (SimulatedSupport.isLoaded() && NativeSections.seedLookOfOwningMod(tabId)) {
            Config.markNativeSeeded(tabId);
            return true;
        }
        ResourceLocation art = bestFor(tabId.getNamespace());
        if (art == null) {
            if (SCANNED.add(tabId.getNamespace())) {
                createaddonorganizer.LOGGER.debug("[CAO] {} ships no banner-shaped image under {}",
                        tabId.getNamespace(), ROOT);
            }
            return false;
        }
        String ref = BannerTextures.shippedRef(art);
        ResourceLocation texture = BannerTextures.resolve(ref);
        if (texture == null) {
            return false;
        }
        Config.setSectionBanner(tabId, ref);
        Config.addExtraPoolEntry(tabId, ref);
        if (BannerAnimation.isAnimatable(texture)) {
            Config.setAnimatedBanner(texture, frameTicksOf(art));
            Config.setBannerAlwaysAnimates(texture, true);
            BannerAnimation.invalidate(texture);
        }
        Config.setSectionName(tabId, "");
        Config.markNativeSeeded(tabId);
        SEEDED_REFS.add(ref);
        createaddonorganizer.LOGGER.info("[CAO] copied in the banner {} that {} ships for its own tab, and left the"
                + " title blank because a mod that draws no title of its own has its name painted into the art",
                art, tabId.getNamespace());
        return true;
    }

    public static List<String> galleryRefs() {
        sweep();
        Set<String> refs = new LinkedHashSet<>();
        for (List<ResourceLocation> found : BY_NAMESPACE.values()) {
            if (found.isEmpty()) {
                continue;
            }
            String ref = BannerTextures.shippedRef(found.get(0));
            if (BannerTextures.resolve(ref) != null) {
                refs.add(ref);
            }
        }
        refs.addAll(SEEDED_REFS);
        return List.copyOf(refs);
    }

    private static ResourceLocation bestFor(String namespace) {
        List<ResourceLocation> found = candidatesFor(namespace);
        return found.isEmpty() ? null : found.get(0);
    }

    public static List<ResourceLocation> candidatesFor(String namespace) {
        sweep();
        return BY_NAMESPACE.getOrDefault(namespace, List.of());
    }

    private static synchronized void sweep() {
        if (swept) {
            return;
        }
        swept = true;
        Map<ResourceLocation, Resource> found = Minecraft.getInstance().getResourceManager()
                .listResources(ROOT, p -> p.getPath().endsWith(".png")
                        && p.getPath().toLowerCase(Locale.ROOT).contains(MARKER));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            if (isBannerShaped(entry.getKey(), entry.getValue())) {
                BY_NAMESPACE.computeIfAbsent(entry.getKey().getNamespace(), k -> new ArrayList<>())
                        .add(entry.getKey());
            }
        }
        for (List<ResourceLocation> usable : BY_NAMESPACE.values()) {
            usable.sort(Comparator.comparingInt(ShippedBanners::nameRank).thenComparing(ResourceLocation::toString));
        }
        createaddonorganizer.LOGGER.info("[CAO] {} mod(s) ship a banner-shaped image of their own: {}",
                BY_NAMESPACE.size(), BY_NAMESPACE.keySet());
    }

    private static int nameRank(ResourceLocation art) {
        String stem = art.getPath().substring(art.getPath().lastIndexOf('/') + 1).replace(".png", "")
                .toLowerCase(Locale.ROOT);
        if (stem.equals(MARKER)) {
            return 0;
        }
        return stem.endsWith(MARKER) ? 1 : 2;
    }

    private static boolean isBannerShaped(ResourceLocation art, Resource resource) {
        int[] size = readSize(art, resource);
        if (size == null) {
            return false;
        }
        int width = size[0];
        int height = size[1];
        if (width < MIN_WIDTH || width > MAX_WIDTH || height <= 0) {
            return false;
        }
        Integer declared = declaredFrameHeight(resource, width, height);
        if (declared != null) {
            return declared > 0 && height % declared == 0;
        }
        for (int candidate : FALLBACK_FRAME_HEIGHTS) {
            if (height % candidate == 0) {
                return true;
            }
        }
        return false;
    }

    private static Integer declaredFrameHeight(Resource resource, int width, int height) {
        try {
            Optional<AnimationMetadataSection> anim = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER);
            return anim.map(a -> a.calculateFrameSize(width, height).height()).orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static int frameTicksOf(ResourceLocation art) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(art);
        if (resource.isEmpty()) {
            return 1;
        }
        try {
            return resource.get().metadata().getSection(AnimationMetadataSection.SERIALIZER)
                    .map(a -> Math.max(1, a.getDefaultFrameTime())).orElse(1);
        } catch (IOException e) {
            return 1;
        }
    }

    private static int[] readSize(ResourceLocation art, Resource resource) {
        try (InputStream in = resource.open()) {
            byte[] header = in.readNBytes(24);
            if (header.length < 24) {
                return null;
            }
            return new int[] {intAt(header, 16), intAt(header, 20)};
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not measure the image {}", art, e);
            return null;
        }
    }

    private static int intAt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }
}

