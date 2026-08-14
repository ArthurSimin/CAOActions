package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.SharedConstants;
import net.neoforged.fml.ModList;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

public final class UpdateCheck {

    public static final String PAGE_URL = "https://modrinth.com/mod/create-addon-organizer";
    private static final String API_URL =
            "https://api.modrinth.com/v2/project/create-addon-organizer/version";
    private static final String LOADER = "neoforge";
    private static final String USER_AGENT = "SockyWocky7/createaddonorganizer";
    private static final String NO_VERSION = "NONE";

    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final Gson GSON = new Gson();

    private static volatile String latest;

    private UpdateCheck() {}

    private record RemoteVersion(String version_number, String[] game_versions, String[] loaders) {}

    public static boolean available() {
        return latest != null;
    }

    public static String latestVersion() {
        return latest;
    }

    public static void syncAsync() {
        if (!Config.checkForUpdates()) {
            return;
        }
        RemoteFetch.startDaemon("createaddonorganizer-update-check", UpdateCheck::sync, STARTED);
    }

    private static void sync() {
        String installed = installedVersion();
        if (installed == null) {
            return;
        }
        String body = fetch();
        if (body == null) {
            return;
        }
        String newest = newestCompatible(body);
        if (newest == null) {
            return;
        }
        ArtifactVersion here = new DefaultArtifactVersion(installed);
        ArtifactVersion there = new DefaultArtifactVersion(newest);
        if (there.compareTo(here) > 0) {
            latest = newest;
            createaddonorganizer.LOGGER.info("[CAO] update available on Modrinth: {} (installed {})",
                    newest, installed);
        }
    }

    private static String fetch() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", USER_AGENT + "/" + installedVersion())
                    .GET()
                    .build();
            HttpResponse<String> response =
                    RemoteFetch.newClient().send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 ? response.body() : null;
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            createaddonorganizer.LOGGER.warn("[CAO] update check failed", e);
            return null;
        }
    }

    private static String newestCompatible(String json) {
        RemoteVersion[] versions;
        try {
            versions = GSON.fromJson(json, RemoteVersion[].class);
        } catch (JsonSyntaxException e) {
            createaddonorganizer.LOGGER.warn("[CAO] update check: unreadable version list", e);
            return null;
        }
        if (versions == null) {
            return null;
        }
        String mcVersion = SharedConstants.getCurrentVersion().getName();
        for (RemoteVersion version : versions) {
            if (version == null || version.version_number() == null) {
                continue;
            }
            if (contains(version.game_versions(), mcVersion) && contains(version.loaders(), LOADER)) {
                return version.version_number();
            }
        }
        return null;
    }

    private static boolean contains(String[] values, String wanted) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (wanted.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private static String installedVersion() {
        return ModList.get().getModContainerById(createaddonorganizer.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .filter(version -> !version.isBlank()
                        && !NO_VERSION.equalsIgnoreCase(version)
                        && !version.toUpperCase(Locale.ROOT).endsWith(NO_VERSION))
                .orElse(null);
    }
}
