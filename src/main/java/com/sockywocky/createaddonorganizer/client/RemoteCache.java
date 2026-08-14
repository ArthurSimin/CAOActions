package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.network.chat.Component;

public final class RemoteCache {

    private RemoteCache() {}

    public static int wipe(Path dir) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        int removed = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.toList()) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                try {
                    Files.delete(file);
                    removed++;
                } catch (IOException e) {
                    createaddonorganizer.LOGGER.warn("[CAO] could not delete cached file {}", file, e);
                }
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not list the cache at {}", dir, e);
        }
        return removed;
    }

    public static void clearAllReporting() {
        if (RemoteBanners.isLocalTesting()) {
            Notice.show(Component.translatable("createaddonorganizer.colors.credits.clearCache.localTesting"), Notice.RED);
            return;
        }
        if (!ClearCacheCooldown.ready()) {
            Notice.show(Component.translatable("createaddonorganizer.colors.credits.clearCache.cooldown",
                    ClearCacheCooldown.remainingLabel()), Notice.RED);
            return;
        }
        int removed = clearAll();
        ClearCacheCooldown.markUsed();
        if (!Config.fetchOnlineBanners()) {
            Notice.show(Component.translatable("createaddonorganizer.colors.credits.clearCache.offline", removed), Notice.RED);
            return;
        }
        Notice.show(Component.translatable("createaddonorganizer.colors.credits.clearCache.done", removed), Notice.GREEN);
    }

    public static int clearAll() {
        int removed = RemoteBanners.clearCache() + RemoteBannerPools.clearCache() + RemoteBoxTextures.clearCache();
        BannerTextures.invalidateRemoteCache();
        BoxTextures.invalidateRemoteCache();
        createaddonorganizer.LOGGER.info("[CAO] cleared {} cached remote file(s); fetching them again", removed);
        RemoteBanners.syncAsync();
        RemoteBannerPools.syncAsync();
        RemoteBoxTextures.syncAsync();
        return removed;
    }
}
