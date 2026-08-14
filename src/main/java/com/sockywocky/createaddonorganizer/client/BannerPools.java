package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class BannerPools {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();
    private static final ResourceLocation RESOURCE =
            ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, "banner_pools.json");

    private static Map<String, List<String>> cached;
    private static Map<String, List<String>> remoteAtLoad;

    private BannerPools() {}

    public static List<String> poolFor(ResourceLocation tabId) {
        List<String> refs = load().get(tabId.toString());
        return refs != null ? refs : List.of();
    }

    public static void invalidate() {
        cached = null;
        remoteAtLoad = null;
    }

    private static Map<String, List<String>> load() {
        Map<String, List<String>> remote = RemoteBannerPools.hasEverCached()
                ? RemoteBannerPools.poolsSnapshot()
                : null;
        if (cached != null && remoteAtLoad == remote) {
            return cached;
        }
        remoteAtLoad = remote;
        cached = read();
        return cached;
    }

    private static Map<String, List<String>> read() {
        Map<String, List<String>> fromSource = loadFromDevFile();
        if (fromSource != null) {
            return fromSource;
        }
        if (RemoteBannerPools.hasEverCached()) {
            return RemoteBannerPools.poolsSnapshot();
        }
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(RESOURCE);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Map<String, List<String>> data = GSON.fromJson(reader, MAP_TYPE);
            return data != null ? data : Map.of();
        } catch (IOException | RuntimeException e) {
            return Map.of();
        }
    }

    private static Map<String, List<String>> loadFromDevFile() {
        Path devFile = resolveDevFile();
        if (devFile == null || !Files.exists(devFile)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(devFile, StandardCharsets.UTF_8)) {
            Map<String, List<String>> data = GSON.fromJson(reader, MAP_TYPE);
            return data != null ? data : Map.of();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    public static final class DevWriteException extends Exception {
        public DevWriteException(String message) {
            super(message);
        }
    }

    public static void setPool(ResourceLocation tabId, List<String> refs) throws IOException, DevWriteException {
        Path devFile = resolveDevFile();
        if (devFile == null) {
            throw new DevWriteException("not running from source (gradlew runClient)");
        }
        Map<String, List<String>> data = new LinkedHashMap<>();
        if (Files.exists(devFile)) {
            try (Reader reader = Files.newBufferedReader(devFile, StandardCharsets.UTF_8)) {
                Map<String, List<String>> existing = GSON.fromJson(reader, MAP_TYPE);
                if (existing != null) {
                    data.putAll(existing);
                }
            }
        }
        if (refs.isEmpty()) {
            data.remove(tabId.toString());
        } else {
            data.put(tabId.toString(), refs);
        }
        Files.createDirectories(devFile.getParent());
        try (Writer writer = Files.newBufferedWriter(devFile, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
        }
        invalidate();
    }

    private static Path resolveDevFile() {
        Path cwd = Path.of("").toAbsolutePath();
        Path projectRoot = cwd.getParent();
        if (projectRoot == null || !Files.isDirectory(projectRoot.resolve("src/main/resources"))) {
            return null;
        }
        return projectRoot.resolve("src/main/resources/assets/createaddonorganizer/banner_pools.json");
    }
}

