package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import net.neoforged.fml.loading.FMLPaths;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class ClearCacheCooldown {

    public static final long COOLDOWN_MS = 10 * 60_000L;

    private static final Path FILE = FMLPaths.CONFIGDIR.get()
            .resolve("createaddonorganizer").resolve("clear_cache_cooldown.txt");

    private static long lastUsedMillis;
    private static boolean loaded;

    private ClearCacheCooldown() {}

    public static boolean ready() {
        return remainingMillis() <= 0L;
    }

    public static long remainingMillis() {
        if (DevMode.isUnlocked()) {
            return 0L;
        }
        long last = lastUsed();
        if (last <= 0L) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed < 0L) {
            return 0L;
        }
        return Math.max(0L, COOLDOWN_MS - elapsed);
    }

    public static String remainingLabel() {
        long seconds = (remainingMillis() + 999L) / 1000L;
        return String.format("%d:%02d", seconds / 60L, seconds % 60L);
    }

    public static void markUsed() {
        lastUsedMillis = System.currentTimeMillis();
        loaded = true;
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, Long.toString(lastUsedMillis), StandardCharsets.UTF_8);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not record the clear-cache cooldown at {}", FILE, e);
        }
    }

    private static long lastUsed() {
        if (!loaded) {
            loaded = true;
            lastUsedMillis = read();
        }
        return lastUsedMillis;
    }

    private static long read() {
        try {
            if (!Files.isRegularFile(FILE)) {
                return 0L;
            }
            return Long.parseLong(Files.readString(FILE, StandardCharsets.UTF_8).trim());
        } catch (IOException | NumberFormatException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not read the clear-cache cooldown at {}", FILE, e);
            return 0L;
        }
    }
}
