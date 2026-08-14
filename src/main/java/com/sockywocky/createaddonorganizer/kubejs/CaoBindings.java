package com.sockywocky.createaddonorganizer.kubejs;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.PackDefaults;

import net.minecraft.resources.ResourceLocation;

public final class CaoBindings {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CaoBindings() {}

    public static void fold(String tabId, String hubId) {
        ResourceLocation tab = id(tabId, "fold", "tab");
        ResourceLocation hub = id(hubId, "fold", "hub");
        if (tab == null || hub == null) {
            return;
        }
        if (tab.equals(hub)) {
            error("fold", "a tab cannot be folded into itself: " + tab);
            return;
        }
        PackDefaults.route(tab, hub);
        LOGGER.info("[CAO] pack script folds {} into {}", tab, hub);
    }

    public static void include(String tabId) {
        ResourceLocation tab = id(tabId, "include", "tab");
        if (tab == null) {
            return;
        }
        PackDefaults.include(tab);
        LOGGER.info("[CAO] pack script asks for {} by name", tab);
    }

    public static void exclude(String tabId) {
        ResourceLocation tab = id(tabId, "exclude", "tab");
        if (tab == null) {
            return;
        }
        PackDefaults.exclude(tab);
        LOGGER.info("[CAO] pack script leaves {} alone", tab);
    }

    public static void name(String tabId, String name) {
        ResourceLocation tab = id(tabId, "name", "tab");
        if (tab == null) {
            return;
        }
        if (name == null) {
            error("name", "no name given for " + tab);
            return;
        }
        PackDefaults.name(tab, name);
    }

    public static void color(String tabId, String spec) {
        ResourceLocation tab = id(tabId, "color", "tab");
        if (tab == null) {
            return;
        }
        if (spec == null || Config.parseColorSpecEntry(spec, true) == null) {
            error("color", "'" + spec + "' is not a colour we understand (try '#7A5FBF'), for " + tab);
            return;
        }
        PackDefaults.color(tab, spec);
    }

    public static void banner(String tabId, String ref) {
        ResourceLocation tab = id(tabId, "banner", "tab");
        if (tab == null) {
            return;
        }
        if (ref == null || ref.isBlank()) {
            error("banner", "no banner reference given for " + tab);
            return;
        }
        PackDefaults.banner(tab, ref);
    }

    public static void group(String name, String hubId, Object members) {
        ResourceLocation hub = id(hubId, "group", "hub");
        if (hub == null) {
            return;
        }
        List<ResourceLocation> ids = idList(members, "group");
        if (ids.isEmpty()) {
            error("group", "no members given for hub " + hub);
            return;
        }
        if (name != null && !name.isBlank()) {
            PackDefaults.name(hub, name);
        }
        for (ResourceLocation member : ids) {
            if (member.equals(hub)) {
                continue;
            }
            PackDefaults.route(member, hub);
        }
        LOGGER.info("[CAO] pack script groups {} section(s) under {}", ids.size(), hub);
    }

    public static void order(Object ids) {
        List<ResourceLocation> parsed = idList(ids, "order");
        if (parsed.isEmpty()) {
            error("order", "no section ids given");
            return;
        }
        PackDefaults.order(parsed);
        LOGGER.info("[CAO] pack script orders {} section(s)", parsed.size());
    }

    private static ResourceLocation id(String raw, String call, String what) {
        if (raw == null || raw.isBlank()) {
            error(call, "no " + what + " id given");
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(raw.trim());
        if (parsed == null) {
            error(call, "'" + raw + "' is not a valid " + what + " id (expected 'namespace:path')");
        }
        return parsed;
    }

    private static List<ResourceLocation> idList(Object raw, String call) {
        List<ResourceLocation> out = new ArrayList<>();
        collect(raw, call, out);
        return out;
    }

    private static void collect(Object raw, String call, List<ResourceLocation> out) {
        if (raw == null) {
            return;
        }
        if (raw instanceof CharSequence text) {
            for (String part : text.toString().split(",")) {
                if (!part.isBlank()) {
                    ResourceLocation parsed = id(part, call, "section");
                    if (parsed != null) {
                        out.add(parsed);
                    }
                }
            }
            return;
        }
        if (raw instanceof Object[] array) {
            for (Object element : array) {
                collect(element, call, out);
            }
            return;
        }
        if (raw instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                collect(element, call, out);
            }
            return;
        }
        error(call, "expected a list of section ids, got " + raw.getClass().getSimpleName());
    }

    private static void error(String call, String message) {
        LOGGER.error("[CAO] CreateAddonOrganizer.{}(): {}", call, message);
    }
}

