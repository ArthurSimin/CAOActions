package com.sockywocky.createaddonorganizer;

import java.util.Set;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;

public final class AddonDetection {
    static final String CREATE = "create";
    private static final String MINECRAFT = "minecraft";

    private AddonDetection() {}

    public static boolean isAbsorbTarget(ResourceLocation tabId) {
        return skipReason(tabId) == null;
    }

    public static boolean isOffered(ResourceLocation tabId) {
        return isAbsorbTarget(tabId) || KubeJsSupport.isPackTab(tabId);
    }

    public static String skipReason(ResourceLocation tabId) {
        String ns = tabId.getNamespace();
        if (CREATE.equals(ns)) {
            return "Create-owned tab";
        }
        if (SimulatedSupport.isLoaded() && SimulatedSupport.isMainTab(tabId)) {
            return "Simulated's own main tab, never an absorb target";
        }
        if (TabLayout.slotOf(tabId) >= 0 && !Config.isForceIncluded(tabId)) {
            return "custom tab; only folded when you ask for it";
        }
        if (Config.isForceExcluded(tabId)) {
            return PackDefaults.excludes(tabId) ? "left alone by this pack's script" : "force-excluded in config";
        }
        if (Config.parentFor(tabId) == null) {
            return "target hub is excluded";
        }
        if (Config.isAskedForByName(tabId)) {
            return null;
        }
        if (createaddonorganizer.buildsItsOwnContents(tabId)) {
            return "builds its own tab contents, so it keeps its own tab";
        }
        if (FancyTabSections.REGISTERED_TABS.containsKey(tabId)) {
            return "already organized with Fancy Tab Sections";
        }
        if (KubeJsSupport.isPackTab(tabId)) {
            return "KubeJS pack tab -- fold it from this screen, or with CreateAddonOrganizer.fold('"
                    + tabId + "', 'create:base') in a startup script";
        }
        ModContainer container = ModList.get().getModContainerById(ns).orElse(null);
        if (container == null) {
            return "no loaded mod owns namespace '" + ns + "'";
        }
        boolean dependsOnSimulated = SimulatedSupport.isLoaded() && dependsOn(container, SimulatedSupport.MOD_ID);
        if (!dependsOn(container, CREATE) && !dependsOnSimulated) {
            return "mod '" + ns + "' declares no Create or Simulated dependency";
        }
        return null;
    }

    public static boolean dependsOn(ResourceLocation tabId, String modId) {
        ModContainer container = ModList.get().getModContainerById(tabId.getNamespace()).orElse(null);
        return container != null && dependsOn(container, modId);
    }

    public static boolean isSubSectionCandidate(ResourceLocation id) {
        return isSubSectionCandidate(id, SectionCatalog.knownHubs());
    }

    public static boolean isSubSectionCandidate(ResourceLocation id, Set<ResourceLocation> knownHubs) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        return tab != null && tab.getType() == CreativeModeTab.Type.CATEGORY
                && !CREATE.equals(id.getNamespace())
                && !MINECRAFT.equals(id.getNamespace())
                && !knownHubs.contains(id)
                && !Config.isBuiltinExcluded(id)
                && !isUnusedCustomSlot(id)
                && !SimulatedSupport.isMainTab(id);
    }

    public static boolean isUnusedCustomSlot(ResourceLocation id) {
        int slot = TabLayout.slotOf(id);
        return slot >= 0 && TabLayoutStore.bySlot(slot) == null;
    }

    public static boolean isHubPromotionCandidate(ResourceLocation id) {
        return isHubPromotionCandidate(id, SectionCatalog.knownHubs());
    }

    public static boolean isHubPromotionCandidate(ResourceLocation id, Set<ResourceLocation> knownHubs) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        return tab != null && tab.getType() == CreativeModeTab.Type.CATEGORY
                && !MINECRAFT.equals(id.getNamespace())
                && !knownHubs.contains(id)
                && !Config.isBuiltinExcluded(id)
                && !isUnusedCustomSlot(id);
    }

    public static boolean isPlaced(ResourceLocation id) {
        return AbsorbedTabs.IDS.contains(id) || isAbsorbTarget(id);
    }

    private static boolean dependsOn(ModContainer container, String modId) {
        for (IModInfo.ModVersion dependency : container.getModInfo().getDependencies()) {
            if (modId.equals(dependency.getModId())) {
                return true;
            }
        }
        return false;
    }
}

