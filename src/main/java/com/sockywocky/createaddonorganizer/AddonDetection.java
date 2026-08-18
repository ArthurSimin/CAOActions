package com.sockywocky.createaddonorganizer;

import java.util.Locale;
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
        IModInfo.DependencyType onCreate = dependencyType(container, CREATE);
        IModInfo.DependencyType onSimulated = SimulatedSupport.isLoaded()
                ? dependencyType(container, SimulatedSupport.MOD_ID) : null;
        if (onCreate == null && onSimulated == null) {
            return "mod '" + ns + "' declares no Create or Simulated dependency";
        }
        if (onCreate != IModInfo.DependencyType.REQUIRED && onSimulated != IModInfo.DependencyType.REQUIRED
                && !looksLikeAddon(container, ns)) {
            String linkedTo = onCreate != null ? CREATE : SimulatedSupport.MOD_ID;
            return "mod '" + ns + "' lists " + linkedTo + " only as an optional dependency and nothing else marks "
                    + "it as an addon, so it is left alone as a standalone mod with " + linkedTo
                    + " compatibility -- force-include its tab if you want it folded in anyway";
        }
        return null;
    }

    private static boolean looksLikeAddon(ModContainer container, String namespace) {
        if (KnownAddonCatalog.lists(namespace)) {
            return true;
        }
        String id = namespace.toLowerCase(Locale.ROOT);
        String name = container.getModInfo().getDisplayName().toLowerCase(Locale.ROOT);
        if (id.contains(CREATE) || name.contains(CREATE)) {
            return true;
        }
        if (!SimulatedSupport.isLoaded()) {
            return false;
        }
        String simulated = SimulatedSupport.MOD_ID.toLowerCase(Locale.ROOT);
        return id.contains(simulated) || name.contains(simulated);
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
        return dependencyType(container, modId) != null;
    }

    private static IModInfo.DependencyType dependencyType(ModContainer container, String modId) {
        IModInfo.DependencyType best = null;
        for (IModInfo.ModVersion dependency : container.getModInfo().getDependencies()) {
            if (!modId.equals(dependency.getModId())) {
                continue;
            }
            IModInfo.DependencyType type = dependency.getType();
            if (type == IModInfo.DependencyType.REQUIRED) {
                return type;
            }
            if (type == IModInfo.DependencyType.OPTIONAL) {
                best = type;
            }
        }
        return best;
    }
}

