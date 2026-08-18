package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import com.sockywocky.createaddonorganizer.GuardLog;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class ClientRegistries {

    private static RegistryAccess fallback;
    private static HolderLookup.Provider fallbackHolders;
    private static CompletableFuture<HolderLookup.Provider> holdersFuture;
    private static boolean primedWithoutWorld;

    private static List<CreativeModeTab> primeQueue;
    private static CreativeModeTab.ItemDisplayParameters primeParams;
    private static int primeIndex;
    private static boolean primeOrganized;

    private ClientRegistries() {}

    public static void warmUp() {
        if (holdersFuture == null && fallbackHolders == null) {
            holdersFuture = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
        }
    }

    public static boolean needsPriming() {
        return !primedWithoutWorld && Minecraft.getInstance().level == null;
    }

    public static RegistryAccess access() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        }
        if (fallback == null) {
            fallback = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        }
        return fallback;
    }

    private static HolderLookup.Provider fallbackHolders() {
        if (fallbackHolders == null) {
            warmUp();
            try {
                fallbackHolders = holdersFuture.join();
            } catch (Throwable t) {
                createaddonorganizer.LOGGER.warn("[CAO] background registry build failed; building inline", t);
                fallbackHolders = VanillaRegistries.createLookup();
            }
            holdersFuture = null;
        }
        return fallbackHolders;
    }

    public static CreativeModeTab.ItemDisplayParameters displayParams() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            boolean hasPermissions = mc.player.canUseGameMasterBlocks() && mc.options.operatorItemsTab().get();
            return new CreativeModeTab.ItemDisplayParameters(mc.level.enabledFeatures(), hasPermissions,
                    mc.level.registryAccess());
        }
        return new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, false, fallbackHolders());
    }

    public static void ensureTabContents() {
        CreativeModeTab.ItemDisplayParameters params = displayParams();
        try {
            CreativeModeTabs.tryRebuildTabContents(params.enabledFeatures(), params.hasPermissions(),
                    params.holders());
        } catch (Throwable t) {
            GuardLog.report("could not build creative tab contents without a world", t);
        }
    }

    public static void primeWithoutWorld() {
        if (primedWithoutWorld || Minecraft.getInstance().level != null) {
            return;
        }
        primedWithoutWorld = true;
        ensureTabContents();
        createaddonorganizer.organize(displayParams());
    }

    public static boolean advancePrime(long budgetNanos) {
        if (primedWithoutWorld || Minecraft.getInstance().level != null) {
            return true;
        }
        warmUp();
        if (fallbackHolders == null && !holdersFuture.isDone()) {
            return false;
        }
        if (primeQueue == null) {
            primeParams = displayParams();
            primeQueue = orderedTabs();
            primeIndex = 0;
            primeOrganized = false;
            return false;
        }
        long deadline = System.nanoTime() + budgetNanos;
        while (primeIndex < primeQueue.size()) {
            CreativeModeTab tab = primeQueue.get(primeIndex++);
            try {
                tab.buildContents(primeParams);
            } catch (Throwable t) {
                GuardLog.report("could not build contents for a creative tab", t);
            }
            if (System.nanoTime() >= deadline) {
                return false;
            }
        }
        if (!primeOrganized) {
            primeOrganized = true;
            createaddonorganizer.organize(primeParams);
            return false;
        }
        primedWithoutWorld = true;
        primeQueue = null;
        primeParams = null;
        return true;
    }

    private static List<CreativeModeTab> orderedTabs() {
        List<CreativeModeTab> ordered = new ArrayList<>();
        List<CreativeModeTab> trailing = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() == CreativeModeTab.Type.CATEGORY) {
                ordered.add(tab);
            } else {
                trailing.add(tab);
            }
        }
        ordered.addAll(trailing);
        return ordered;
    }

    public static boolean canRenderItems() {
        return Minecraft.getInstance().player != null;
    }
}
