package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public final class TabOrder {

    private TabOrder() {}

    public static List<CreativeModeTab> apply(List<CreativeModeTab> registryOrder) {
        List<ResourceLocation> saved = Config.tabOrder();
        if (saved.isEmpty()) {
            return registryOrder;
        }
        Map<ResourceLocation, CreativeModeTab> remaining = new LinkedHashMap<>();
        for (CreativeModeTab tab : registryOrder) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null) {
                remaining.put(id, tab);
            }
        }
        List<CreativeModeTab> out = new ArrayList<>(registryOrder.size());
        for (ResourceLocation id : saved) {
            CreativeModeTab tab = remaining.remove(id);
            if (tab != null) {
                out.add(tab);
            }
        }
        out.addAll(remaining.values());
        return out;
    }
}
