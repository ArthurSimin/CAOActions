package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

public final class RemoteBannerPools {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();
    private static final Path REMOTE_DIR = FMLPaths.CONFIGDIR.get().resolve("createaddonorganizer/remote_banner_pools");
    private static final Path MANIFEST_CACHE = REMOTE_DIR.resolve("pools.json");
    private static final Path ETAG_FILE = REMOTE_DIR.resolve("pools.etag");
    private static final String RAW_FALLBACK_URL =
            "https://raw.githubusercontent.com/SockyWocky7/createaddonorganizer/master/banners/pools.json";

    private static final AtomicBoolean SYNC_STARTED = new AtomicBoolean(false);

    private static volatile Map<String, List<String>> pools = Map.of();
    private static volatile boolean everCached = false;

    private RemoteBannerPools() {}

    public static void loadCacheFromDisk() {
        try {
            if (!Files.exists(MANIFEST_CACHE)) {
                everCached = false;
                return;
            }
            Map<String, List<String>> parsed = parseManifest(Files.readAllBytes(MANIFEST_CACHE));
            if (parsed == null) {
                pools = Map.of();
                everCached = false;
                return;
            }
            pools = parsed;
            everCached = true;
        } catch (IOException | RuntimeException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to load cached remote banner-pool manifest", e);
            pools = Map.of();
            everCached = false;
        }
    }

    public static int clearCache() {
        int removed = RemoteCache.wipe(REMOTE_DIR);
        pools = Map.of();
        everCached = false;
        SYNC_STARTED.set(false);
        return removed;
    }

    public static void syncAsync() {
        RemoteFetch.startDaemon("createaddonorganizer-banner-pools-sync", RemoteBannerPools::sync, SYNC_STARTED);
    }

    public static boolean hasEverCached() {
        return everCached;
    }

    public static List<String> poolFor(ResourceLocation tabId) {
        List<String> refs = pools.get(tabId.toString());
        return refs != null ? refs : List.of();
    }

    public static Map<String, List<String>> poolsSnapshot() {
        return pools;
    }

    public static void refreshLocal() {
        Path dir = localDir();
        if (dir == null) {
            createaddonorganizer.LOGGER.warn("[CAO] local testing: could not resolve project root (not running from source)");
            return;
        }
        Path manifest = dir.resolve("pools.json");
        if (!Files.exists(manifest)) {
            createaddonorganizer.LOGGER.warn("[CAO] local testing: no pools.json found at {}", manifest);
            pools = Map.of();
            everCached = true;
            return;
        }
        try {
            Map<String, List<String>> parsed = parseManifest(Files.readAllBytes(manifest));
            if (parsed == null) {
                return;
            }
            pools = parsed;
            everCached = true;
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] local testing: failed to read {}", manifest, e);
        }
    }

    private static Path localDir() {
        Path cwd = Path.of("").toAbsolutePath();
        Path projectRoot = cwd.getParent();
        return projectRoot == null ? null : projectRoot.resolve("banners");
    }

    private static void sync() {
        try {
            if (!Config.fetchOnlineBanners()) {
                return;
            }
            if (RemoteBanners.isLocalTesting()) {
                refreshLocal();
                return;
            }
            HttpClient client = RemoteFetch.newClient();
            String etag = RemoteFetch.readEtag(ETAG_FILE, "banner-pool manifest");

            RemoteFetch.FetchResult result = RemoteFetch.fetchWithFallback(
                    client, Config.bannerPoolsManifestUrl(), RAW_FALLBACK_URL, etag, "banner-pool manifest");
            if (result == null) {
                createaddonorganizer.LOGGER.warn("[CAO] remote banner-pool manifest fetch failed (primary and fallback)");
                return;
            }
            if (result.notModified()) {
                return;
            }

            Map<String, List<String>> parsed = parseManifest(result.body());
            if (parsed == null) {
                return;
            }

            RemoteFetch.writeAtomic(MANIFEST_CACHE, result.body());
            if (result.etag() != null) {
                RemoteFetch.writeAtomic(ETAG_FILE, result.etag().getBytes(StandardCharsets.UTF_8));
            }

            pools = parsed;
            everCached = true;
        } catch (Exception e) {
            createaddonorganizer.LOGGER.warn("[CAO] remote banner-pool sync failed", e);
        }
    }

    private static Map<String, List<String>> parseManifest(byte[] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        Map<String, List<String>> parsed;
        try {
            parsed = GSON.fromJson(json, MAP_TYPE);
        } catch (JsonSyntaxException e) {
            createaddonorganizer.LOGGER.warn("[CAO] remote banner-pool manifest is not valid JSON; ignoring it", e);
            return null;
        }
        if (parsed == null) {
            return Map.of();
        }
        Map<String, List<String>> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
            if (entry.getKey() == null || ResourceLocation.tryParse(entry.getKey()) == null || entry.getValue() == null) {
                createaddonorganizer.LOGGER.warn("[CAO] skipping malformed entry in banner-pool manifest: {}", entry.getKey());
                continue;
            }
            sanitized.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(sanitized);
    }
}
