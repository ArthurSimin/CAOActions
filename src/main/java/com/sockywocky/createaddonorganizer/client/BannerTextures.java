package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import com.mojang.blaze3d.platform.NativeImage;
import com.sockywocky.createaddonorganizer.createaddonorganizer;
import com.sockywocky.createaddonorganizer.client.simulated.NativeSections;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.fml.loading.FMLPaths;

public final class BannerTextures {
    public static final int WIDTH = 160;
    public static final int HEIGHT = 17;

    public static void blitCropped(net.minecraft.client.gui.GuiGraphics g, ResourceLocation tex,
            int x, int y, int w, int h, int textureTotalHeight) {
        int u = Math.max(0, (WIDTH - w) / 2);
        g.blit(tex, x, y, w, h, u, 0.0F, w, h, WIDTH, textureTotalHeight);
    }

    private static final Path BANNERS_DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer/banners");

    private static final Map<String, ResourceLocation> FILE_REGISTERED = new HashMap<>();
    private static final Map<String, ResourceLocation> REMOTE_REGISTERED = new HashMap<>();
    private static final Map<ResourceLocation, Integer> FILE_HEIGHTS = new HashMap<>();
    private static final Set<ResourceLocation> BUNDLED_REGISTERED = new HashSet<>();
    private static final Map<String, ResourceLocation> NATIVE_REGISTERED = new HashMap<>();
    private static final Set<String> NATIVE_FAILED = new HashSet<>();
    private static final Map<String, ResourceLocation> SHIPPED_REGISTERED = new HashMap<>();
    private static final Set<String> SHIPPED_FAILED = new HashSet<>();

    private static final int NATIVE_FRAME_HEIGHT = 18;
    private static final int CONTENT_HEIGHT = 16;

    private static final int[] FALLBACK_FRAME_HEIGHTS = {NATIVE_FRAME_HEIGHT, 19, HEIGHT};
    private static final String NATIVE_PREFIX = "native:";
    private static final String SHIPPED_PREFIX = "shipped:";

    private BannerTextures() {}

    public static String shippedRef(ResourceLocation art) {
        return SHIPPED_PREFIX + art;
    }

    public static boolean isShippedRef(String ref) {
        return ref != null && ref.startsWith(SHIPPED_PREFIX);
    }

    public static ResourceLocation shippedArtOf(String ref) {
        return isShippedRef(ref) ? ResourceLocation.tryParse(ref.substring(SHIPPED_PREFIX.length())) : null;
    }

    public static String nativeRef(ResourceLocation sprite) {
        return NATIVE_PREFIX + sprite;
    }

    public static boolean isNativeRef(String ref) {
        return ref != null && ref.startsWith(NATIVE_PREFIX);
    }

    public static ResourceLocation nativeSpriteOf(String ref) {
        return isNativeRef(ref) ? ResourceLocation.tryParse(ref.substring(NATIVE_PREFIX.length())) : null;
    }

    public static ResourceLocation nativeTexturePath(ResourceLocation sprite) {
        return ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(),
                "textures/gui/sprites/" + sprite.getPath() + ".png");
    }

    public static int nativeFrameTicks(ResourceLocation sprite) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                .getResource(nativeTexturePath(sprite));
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

    public static ResourceLocation resolve(String ref) {
        if (ref == null) {
            return null;
        }
        if (ref.startsWith("res:")) {
            ResourceLocation rl = ResourceLocation.tryParse(ref.substring(4));
            if (rl != null) {
                ensureBundledRegistered(rl);
            }
            return rl;
        }
        if (ref.startsWith("file:")) {
            String fileName = ref.substring(5);
            ResourceLocation cached = FILE_REGISTERED.get(fileName);
            return cached != null ? cached : loadFileAndCache(BANNERS_DIR.resolve(fileName), fileName);
        }
        if (ref.startsWith(NATIVE_PREFIX)) {
            ResourceLocation cached = NATIVE_REGISTERED.get(ref);
            if (cached != null) {
                return cached;
            }
            ResourceLocation sprite = nativeSpriteOf(ref);
            return sprite != null && !NATIVE_FAILED.contains(ref) ? loadNativeAndCache(ref, sprite) : null;
        }
        if (ref.startsWith(SHIPPED_PREFIX)) {
            ResourceLocation cached = SHIPPED_REGISTERED.get(ref);
            if (cached != null) {
                return cached;
            }
            ResourceLocation art = shippedArtOf(ref);
            return art != null && !SHIPPED_FAILED.contains(ref) ? loadShippedAndCache(ref, art) : null;
        }
        if (ref.startsWith("remote:")) {
            dropRemoteTexturesIfRedownloaded();
            String fileName = ref.substring(7);
            ResourceLocation cached = REMOTE_REGISTERED.get(fileName);
            if (cached != null) {
                return cached;
            }
            return RemoteBanners.isCachedOnDisk(fileName)
                    ? loadRemoteAndCache(RemoteBanners.fileFor(fileName), fileName)
                    : null;
        }
        return ResourceLocation.tryParse(ref);
    }

    public static List<String> gallery() {
        Map<ResourceLocation, Resource> found = Minecraft.getInstance().getResourceManager()
                .listResources("textures/banner", p -> p.getPath().endsWith(".png"));
        List<String> bundled = new ArrayList<>();
        List<String> bundledFileNames = new ArrayList<>();
        for (ResourceLocation tex : found.keySet()) {
            bundled.add(resRef(tex));
            bundledFileNames.add(tex.getPath().substring(tex.getPath().lastIndexOf('/') + 1));
        }
        bundled.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> uploads = new ArrayList<>();
        if (Files.isDirectory(BANNERS_DIR)) {
            try (var files = Files.list(BANNERS_DIR)) {
                files.filter(p -> p.getFileName().toString().endsWith(".png"))
                        .forEach(p -> uploads.add("file:" + p.getFileName()));
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to list uploaded banners", e);
            }
        }
        uploads.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> remotes = RemoteBanners.availableFilenames().stream()
                .filter(f -> bundledFileNames.stream().noneMatch(b -> b.equalsIgnoreCase(f)))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(f -> "remote:" + f)
                .toList();

        List<String> natives = SimulatedSupport.isLoaded() ? NativeSections.galleryRefs() : List.of();
        Set<ResourceLocation> nativeArt = new HashSet<>();
        for (String ref : natives) {
            ResourceLocation sprite = nativeSpriteOf(ref);
            if (sprite != null) {
                nativeArt.add(nativeTexturePath(sprite));
            }
        }
        List<String> shipped = ShippedBanners.galleryRefs().stream()
                .filter(ref -> !nativeArt.contains(shippedArtOf(ref)))
                .toList();

        List<String> out = new ArrayList<>(bundled.size() + uploads.size() + remotes.size()
                + natives.size() + shipped.size());
        out.addAll(bundled);
        out.addAll(natives);
        out.addAll(shipped);
        out.addAll(uploads);
        out.addAll(remotes);
        return out;
    }

    public static String resRef(ResourceLocation texture) {
        return "res:" + texture;
    }

    public static Optional<Integer> frameHeightFor(ResourceLocation texture) {
        return Optional.ofNullable(FILE_HEIGHTS.get(texture));
    }

    public static Optional<Path> chooseFile() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png"));
            filters.flip();
            String result = TinyFileDialogs.tinyfd_openFileDialog(
                    "Choose a banner PNG (160 wide; a multiple of 17 tall for animation)", "", filters, "PNG image (*.png)", false);
            if (result == null || result.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(Path.of(result));
        } catch (Throwable t) {

            createaddonorganizer.LOGGER.warn("[CAO] native file dialog unavailable", t);
            return Optional.empty();
        }
    }

    private static int seenRemoteContentVersion;

    private static void dropRemoteTexturesIfRedownloaded() {
        int version = RemoteBanners.contentVersion();
        if (version == seenRemoteContentVersion) {
            return;
        }
        seenRemoteContentVersion = version;
        invalidateRemoteCache();
    }

    public static void invalidateRemoteCache() {
        for (ResourceLocation rl : REMOTE_REGISTERED.values()) {
            Minecraft.getInstance().getTextureManager().release(rl);
            FILE_HEIGHTS.remove(rl);
            BannerAnimation.invalidate(rl);
        }
        REMOTE_REGISTERED.clear();
    }

    public static void deleteFile(String ref) {
        if (ref == null || !ref.startsWith("file:")) {
            return;
        }
        String fileName = ref.substring(5);
        ResourceLocation registered = FILE_REGISTERED.remove(fileName);
        if (registered != null) {
            Minecraft.getInstance().getTextureManager().release(registered);
            FILE_HEIGHTS.remove(registered);
            BannerAnimation.invalidate(registered);
        }
        try {
            Files.deleteIfExists(BANNERS_DIR.resolve(fileName));
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to delete banner image {}", fileName, e);
        }
    }

    public static String importFile(Path source) throws IOException {
        Files.createDirectories(BANNERS_DIR);
        String fileName = dedupedName(sanitizeStem(source.getFileName().toString()));
        Path dst = BANNERS_DIR.resolve(fileName);
        Files.copy(source, dst, StandardCopyOption.REPLACE_EXISTING);
        loadFileAndCache(dst, fileName);
        return "file:" + fileName;
    }

    private static void ensureBundledRegistered(ResourceLocation rl) {
        if (BUNDLED_REGISTERED.contains(rl)) {
            return;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStream in = resource.get().open()) {
            NativeImage image = NativeImage.read(in);
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(image));
            BUNDLED_REGISTERED.add(rl);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to register bundled banner texture {}", rl, e);
        }
    }

    private static ResourceLocation loadFileAndCache(Path path, String fileName) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                "custom_banner/" + sanitizeStem(fileName));
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage resized = resizeForImport(NativeImage.read(in));
            FILE_HEIGHTS.put(rl, resized.getHeight());
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(resized));
            FILE_REGISTERED.put(fileName, rl);
            BannerAnimation.invalidate(rl);
            return rl;
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load banner image {}", path, e);
            return null;
        }
    }

    private static ResourceLocation loadRemoteAndCache(Path path, String fileName) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                "custom_banner/remote/" + sanitizeStem(fileName));
        try (InputStream in = Files.newInputStream(path)) {
            NativeImage resized = resizeForImport(NativeImage.read(in));
            FILE_HEIGHTS.put(rl, resized.getHeight());
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(resized));
            REMOTE_REGISTERED.put(fileName, rl);
            BannerAnimation.invalidate(rl);
            return rl;
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load remote banner image {}", path, e);
            return null;
        }
    }

    private static ResourceLocation loadNativeAndCache(String ref, ResourceLocation sprite) {
        ResourceLocation source = nativeTexturePath(sprite);
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(source);
        if (resource.isEmpty()) {
            NATIVE_FAILED.add(ref);
            createaddonorganizer.LOGGER.warn("[CAO] no image behind the sprite {}, so its banner cannot be reused", sprite);
            return null;
        }
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                "native_banner/" + sanitizeStem(sprite.getNamespace() + "_" + sprite.getPath()));
        ResourceLocation registered = convertAndRegister(resource.get(), rl);
        if (registered == null) {
            NATIVE_FAILED.add(ref);
            createaddonorganizer.LOGGER.warn("[CAO] failed to convert the banner sprite {}", sprite);
            return null;
        }
        NATIVE_REGISTERED.put(ref, registered);
        return registered;
    }

    private static ResourceLocation loadShippedAndCache(String ref, ResourceLocation art) {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(art);
        if (resource.isEmpty()) {
            SHIPPED_FAILED.add(ref);
            createaddonorganizer.LOGGER.warn("[CAO] the banner image {} is gone, so it cannot be reused", art);
            return null;
        }
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID,
                "shipped_banner/" + sanitizeStem(art.getNamespace() + "_" + art.getPath()));
        ResourceLocation registered = convertAndRegister(resource.get(), rl);
        if (registered == null) {
            SHIPPED_FAILED.add(ref);
            createaddonorganizer.LOGGER.warn("[CAO] failed to convert the banner image {}", art);
            return null;
        }
        SHIPPED_REGISTERED.put(ref, registered);
        return registered;
    }

    private static ResourceLocation convertAndRegister(Resource resource, ResourceLocation rl) {
        try (InputStream in = resource.open()) {
            NativeImage converted = convertNative(NativeImage.read(in), resource);
            FILE_HEIGHTS.put(rl, converted.getHeight());
            Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(converted));
            BannerAnimation.invalidate(rl);
            return rl;
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not read a banner image for {}", rl, e);
            return null;
        }
    }

    private static int frameHeightOf(Resource resource, NativeImage src) {
        try {
            Optional<AnimationMetadataSection> anim = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER);
            if (anim.isPresent()) {
                int declared = anim.get().calculateFrameSize(src.getWidth(), src.getHeight()).height();
                if (declared > 0 && src.getHeight() % declared == 0) {
                    return declared;
                }
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] unreadable animation metadata on a banner sprite", e);
        }
        for (int candidate : FALLBACK_FRAME_HEIGHTS) {
            if (src.getHeight() % candidate == 0) {
                return candidate;
            }
        }
        return src.getHeight();
    }

    private static NativeImage convertNative(NativeImage src, Resource resource) {
        int frameHeight = Math.max(1, frameHeightOf(resource, src));
        int frames = Math.max(1, src.getHeight() / frameHeight);
        boolean croppable = src.getWidth() >= WIDTH && frameHeight >= CONTENT_HEIGHT;
        int insetX = croppable ? (src.getWidth() - WIDTH) / 2 : 0;
        int insetY = croppable ? Math.min(1, frameHeight - CONTENT_HEIGHT) : 0;

        NativeImage dst = new NativeImage(WIDTH, HEIGHT * frames, false);
        for (int frame = 0; frame < frames; frame++) {
            int srcTop = frame * frameHeight;
            int dstTop = frame * HEIGHT;
            for (int y = 0; y < CONTENT_HEIGHT; y++) {
                int sy = croppable ? srcTop + insetY + y : srcTop + y * frameHeight / CONTENT_HEIGHT;
                for (int x = 0; x < WIDTH; x++) {
                    int sx = croppable ? insetX + x : x * src.getWidth() / WIDTH;
                    dst.setPixelRGBA(x, dstTop + y, src.getPixelRGBA(sx, Math.min(sy, src.getHeight() - 1)));
                }
            }
            for (int x = 0; x < WIDTH; x++) {
                dst.setPixelRGBA(x, dstTop + HEIGHT - 1, dst.getPixelRGBA(x, dstTop + CONTENT_HEIGHT - 1));
            }
        }
        src.close();
        return dst;
    }

    private static NativeImage resizeForImport(NativeImage src) {
        int targetHeight = (src.getHeight() % HEIGHT == 0 && src.getHeight() > 0) ? src.getHeight() : HEIGHT;
        if (src.getWidth() == WIDTH && src.getHeight() == targetHeight) {
            return src;
        }
        NativeImage dst = new NativeImage(WIDTH, targetHeight, false);
        for (int y = 0; y < targetHeight; y++) {
            int sy = y * src.getHeight() / targetHeight;
            for (int x = 0; x < WIDTH; x++) {
                int sx = x * src.getWidth() / WIDTH;
                dst.setPixelRGBA(x, y, src.getPixelRGBA(sx, sy));
            }
        }
        src.close();
        return dst;
    }

    private static String sanitizeStem(String rawFileName) {
        String stem = rawFileName.replaceFirst("(?i)\\.png$", "");
        return stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    private static String dedupedName(String stem) {
        String name = stem + ".png";
        int suffix = 2;
        while (Files.exists(BANNERS_DIR.resolve(name))) {
            name = stem + "_" + suffix + ".png";
            suffix++;
        }
        return name;
    }
}

