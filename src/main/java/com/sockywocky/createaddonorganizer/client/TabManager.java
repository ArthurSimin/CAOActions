package com.sockywocky.createaddonorganizer.client;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class TabManager {
    private TabManager() {}

    public static void deleteSectionConfig(ResourceLocation id) {
        AbsorbedTabs.IDS.remove(id);
        Config.addForceExclude(id);
        Config.clearRoute(id);
        Config.removeForceInclude(id);
        LiveColors.remove(id);
        createaddonorganizer.refreshTabLayout(currentParams());
    }

    public static void restoreSectionConfig(ResourceLocation id, boolean wasForceIncluded, ResourceLocation route) {
        if (wasForceIncluded) {
            Config.addForceInclude(id);
        }
        Config.removeForceExclude(id);
        ResourceLocation hub = route != null ? route : createaddonorganizer.defaultHub();
        if (hub == null) {
            return;
        }
        Config.routeTo(id, hub);
        AbsorbedTabs.IDS.add(id);
        if (SimulatedSupport.isMainTab(hub)) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
            SimulatedHub.inject(id, tab.getDisplayName());
            SimulatedHub.foldItems(id, tab.getDisplayItems());
        } else {
            Section<?> section = createaddonorganizer.sectionFromLiveTab(id);
            if (section != null) {
                FancyTabSections.addSection(hub, section);
            }
        }
        createaddonorganizer.refreshTabLayout(currentParams());
    }

    private static CreativeModeTab.ItemDisplayParameters currentParams() {
        return ClientRegistries.displayParams();
    }
}
