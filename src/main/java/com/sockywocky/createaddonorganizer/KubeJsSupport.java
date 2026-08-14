package com.sockywocky.createaddonorganizer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sockywocky.createaddonorganizer.mixin.CreativeModeTabAccessor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;

public final class KubeJsSupport {
    public static final String MOD_ID = "kubejs";

    private static final String GENERATOR_PACKAGE = "dev.latvian.mods.kubejs";
    private static final String GENERATOR_CLASS = "CreativeTabContentSupplier";

    private static final Map<ResourceLocation, Boolean> PACK_TABS = new ConcurrentHashMap<>();

    private KubeJsSupport() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static boolean isPackTab(ResourceLocation tabId) {
        if (tabId == null || !isLoaded()) {
            return false;
        }
        return PACK_TABS.computeIfAbsent(tabId, KubeJsSupport::detect);
    }

    private static boolean detect(ResourceLocation tabId) {
        if (MOD_ID.equals(tabId.getNamespace())) {
            return true;
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(tabId);
        if (tab == null) {
            return false;
        }
        try {
            CreativeModeTab.DisplayItemsGenerator generator =
                    ((CreativeModeTabAccessor) tab).getDisplayItemsGenerator();
            if (generator == null) {
                return false;
            }
            String name = generator.getClass().getName();
            return name.startsWith(GENERATOR_PACKAGE) || name.contains(GENERATOR_CLASS);
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.debug("[CAO] could not read the contents generator of {}", tabId, t);
            return false;
        }
    }
}

