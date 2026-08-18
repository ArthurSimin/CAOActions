package com.sockywocky.createaddonorganizer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class KnownAddonCatalog {

    private static final String RESOURCE = "/assets/createaddonorganizer/mod_banner_catalog.json";

    private static volatile Set<String> namespaces;

    private KnownAddonCatalog() {}

    public static boolean lists(String namespace) {
        return namespace != null && namespaces().contains(namespace);
    }

    private static Set<String> namespaces() {
        Set<String> snapshot = namespaces;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (KnownAddonCatalog.class) {
            if (namespaces == null) {
                namespaces = load();
            }
            return namespaces;
        }
    }

    private static Set<String> load() {
        Set<String> found = new HashSet<>();
        try (InputStream in = KnownAddonCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                createaddonorganizer.LOGGER.warn("[CAO] the shipped Create addon catalog is missing from the jar");
                return Set.of();
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (!root.isJsonArray()) {
                    return Set.of();
                }
                for (JsonElement modElement : root.getAsJsonArray()) {
                    if (!modElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject mod = modElement.getAsJsonObject();
                    JsonElement tabs = mod.get("tabs");
                    if (tabs == null || !tabs.isJsonArray()) {
                        continue;
                    }
                    collectNamespaces(tabs.getAsJsonArray(), found);
                }
            }
        } catch (Exception e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not read the shipped Create addon catalog", e);
            return Set.of();
        }
        return Set.copyOf(found);
    }

    private static void collectNamespaces(JsonArray tabs, Set<String> found) {
        for (JsonElement tabElement : tabs) {
            if (!tabElement.isJsonObject()) {
                continue;
            }
            JsonElement id = tabElement.getAsJsonObject().get("id");
            if (id == null || !id.isJsonPrimitive()) {
                continue;
            }
            String raw = id.getAsString();
            int colon = raw.indexOf(':');
            if (colon > 0) {
                found.add(raw.substring(0, colon));
            }
        }
    }
}
