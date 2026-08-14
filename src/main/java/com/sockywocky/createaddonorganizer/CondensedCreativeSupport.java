package com.sockywocky.createaddonorganizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;

public final class CondensedCreativeSupport {
    public static final String MOD_ID = "condensed_creative";

    private static final String REGISTRY = "io.wispforest.condensed_creative.registry.CondensedEntryRegistry";
    private static final String DUCK = "cc$refreshCurrentTab";

    private static Boolean present;
    private static boolean wired;
    private static boolean broken;

    private static Field entrypointEntries;
    private static Field resourceEntries;
    private static Method groupOfHelper;

    private CondensedCreativeSupport() {}

    public static boolean isLoaded() {
        if (present == null) {
            present = ModList.get().isLoaded(MOD_ID);
        }
        return present;
    }

    public static boolean active() {
        return isLoaded() && !broken && wire();
    }

    private static synchronized boolean wire() {
        if (wired) {
            return !broken;
        }
        wired = true;
        try {
            Class<?> registry = Class.forName(REGISTRY);
            entrypointEntries = registry.getField("ENTRYPOINT_LOADED_ENTRIES");
            resourceEntries = registry.getField("RESOURCE_LOADED_ENTRIES");
            Class<?> helper = Class.forName("io.wispforest.condensed_creative.util.ItemGroupHelper");
            groupOfHelper = helper.getMethod("group");
            createaddonorganizer.LOGGER.info("[CAO] Condensed Creative is present; its entries will be kept "
                    + "off the tabs this mod lays out, and left alone everywhere else");
            return true;
        } catch (Throwable t) {
            broken = true;
            createaddonorganizer.LOGGER.warn("[CAO] Condensed Creative is installed but its registry does not "
                    + "look the way this mod expects, so the two will not be coordinated. Sections on a tab "
                    + "that Condensed Creative also folds may show the wrong rows.", t);
            return false;
        }
    }

    public static int suppressOn(Set<ResourceLocation> managedTabs) {
        if (managedTabs.isEmpty() || !active()) {
            return 0;
        }
        int removed = 0;
        try {
            removed += strip(entrypointEntries, managedTabs);
            removed += strip(resourceEntries, managedTabs);
        } catch (Throwable t) {
            broken = true;
            createaddonorganizer.LOGGER.warn("[CAO] could not take Condensed Creative's entries off this "
                    + "mod's tabs; their banners may sit a row out while both are folding the same tab", t);
            return removed;
        }
        if (removed > 0) {
            createaddonorganizer.LOGGER.info("[CAO] took {} Condensed Creative entr(ies) off tabs this mod "
                    + "lays out; use the Tab Studio's item groups on those tabs instead", removed);
        }
        return removed;
    }

    private static int strip(Field field, Set<ResourceLocation> managedTabs) throws Exception {
        Object raw = field.get(null);
        if (!(raw instanceof Map<?, ?> map)) {
            return 0;
        }
        int removed = 0;
        Iterator<? extends Map.Entry<?, ?>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<?, ?> entry = it.next();
            if (!managesTab(entry.getKey(), managedTabs)) {
                continue;
            }
            Object value = entry.getValue();
            removed += value instanceof Collection<?> c ? c.size() : 1;
            it.remove();
        }
        return removed;
    }

    private static boolean managesTab(Object helper, Set<ResourceLocation> managedTabs) {
        if (helper == null) {
            return false;
        }
        try {
            Object group = groupOfHelper.invoke(helper);
            if (!(group instanceof CreativeModeTab tab)) {
                return false;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            return id != null && managedTabs.contains(id);
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean resyncPending;

    public static void requestResync() {
        if (isLoaded()) {
            resyncPending = true;
        }
    }

    public static void consumeResync(Object creativeScreen) {
        if (!resyncPending) {
            return;
        }
        resyncPending = false;
        resyncScreen(creativeScreen);
    }

    public static void resyncScreen(Object creativeScreen) {
        if (creativeScreen == null || !active()) {
            return;
        }
        try {
            Method refresh = creativeScreen.getClass().getMethod(DUCK);
            refresh.invoke(creativeScreen);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable t) {
            broken = true;
            createaddonorganizer.LOGGER.warn("[CAO] could not ask Condensed Creative to re-read the creative "
                    + "grid; it may keep showing the rows from before the last change", t);
        }
    }

    public static List<String> describe() {
        List<String> out = new ArrayList<>(2);
        if (!isLoaded()) {
            return out;
        }
        out.add("Condensed Creative " + (active() ? "detected" : "detected but not coordinated"));
        return out;
    }
}
