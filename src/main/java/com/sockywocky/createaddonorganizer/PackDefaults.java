package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;

public final class PackDefaults {
    private static final Map<ResourceLocation, ResourceLocation> ROUTES = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> INCLUDE = ConcurrentHashMap.newKeySet();
    private static final Set<ResourceLocation> EXCLUDE = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, String> NAMES = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> COLORS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, String> BANNERS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> HUBS = ConcurrentHashMap.newKeySet();

    private static volatile List<ResourceLocation> order = List.of();

    private PackDefaults() {}

    public static void route(ResourceLocation id, ResourceLocation hub) {
        ROUTES.put(id, hub);
        HUBS.add(hub);
        INCLUDE.add(id);
        EXCLUDE.remove(id);
    }

    public static void include(ResourceLocation id) {
        INCLUDE.add(id);
        EXCLUDE.remove(id);
    }

    public static void exclude(ResourceLocation id) {
        EXCLUDE.add(id);
        INCLUDE.remove(id);
        ROUTES.remove(id);
    }

    public static void name(ResourceLocation id, String name) {
        NAMES.put(id, name);
    }

    public static void color(ResourceLocation id, String spec) {
        COLORS.put(id, spec);
    }

    public static void banner(ResourceLocation id, String ref) {
        BANNERS.put(id, ref);
    }

    public static void order(List<ResourceLocation> ids) {
        order = List.copyOf(ids);
    }

    public static ResourceLocation routeFor(ResourceLocation id) {
        return ROUTES.get(id);
    }

    public static boolean includes(ResourceLocation id) {
        return INCLUDE.contains(id);
    }

    public static boolean excludes(ResourceLocation id) {
        return EXCLUDE.contains(id);
    }

    public static String nameFor(ResourceLocation id) {
        return NAMES.get(id);
    }

    public static String colorFor(ResourceLocation id) {
        return COLORS.get(id);
    }

    public static String bannerFor(ResourceLocation id) {
        return BANNERS.get(id);
    }

    public static Set<ResourceLocation> routeTargets() {
        return Collections.unmodifiableSet(HUBS);
    }

    public static List<String> orderStrings() {
        List<ResourceLocation> ids = order;
        List<String> out = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            out.add(id.toString());
        }
        return out;
    }

    public static boolean hasOrder() {
        return !order.isEmpty();
    }

    public static boolean isActive() {
        return !ROUTES.isEmpty() || !INCLUDE.isEmpty() || !EXCLUDE.isEmpty() || !NAMES.isEmpty()
                || !COLORS.isEmpty() || !BANNERS.isEmpty() || !order.isEmpty();
    }

    public static boolean hasAnyFor(ResourceLocation id) {
        return ROUTES.containsKey(id) || INCLUDE.contains(id) || EXCLUDE.contains(id)
                || NAMES.containsKey(id) || COLORS.containsKey(id) || BANNERS.containsKey(id);
    }

    public static Set<ResourceLocation> touchedSections() {
        Set<ResourceLocation> out = new LinkedHashSet<>();
        out.addAll(ROUTES.keySet());
        out.addAll(INCLUDE);
        out.addAll(EXCLUDE);
        out.addAll(NAMES.keySet());
        out.addAll(COLORS.keySet());
        out.addAll(BANNERS.keySet());
        return out;
    }

    public static String summary() {
        return ROUTES.size() + " route(s), " + INCLUDE.size() + " include(s), " + EXCLUDE.size()
                + " exclude(s), " + NAMES.size() + " name(s), " + COLORS.size() + " colour(s), "
                + BANNERS.size() + " banner(s), " + order.size() + " ordered section(s)";
    }
}

