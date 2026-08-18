package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BannerEditor {

    private BannerEditor() {}

    public static void open(Screen parent, ResourceLocation sectionId, Component title, boolean isMainTab) {
        if (sectionId == null) {
            return;
        }
        ScreenSwoosh.drill(() -> new ColorPickerScreen(parent, sectionId, title, isMainTab),
                Config.SWOOSH_BANNER_EDITOR);
    }

    public static void openHighlight(Screen parent, ResourceLocation tabId, Component title) {
        if (tabId == null) {
            return;
        }
        ScreenSwoosh.drill(() -> new ColorPickerScreen(parent, tabId, title, true, true),
                Config.SWOOSH_BANNER_EDITOR);
    }

    public static ResourceLocation ownerTabOf(ResourceLocation sectionId) {
        if (sectionId == null) {
            return null;
        }
        ResourceLocation minted = TabLayout.ownerOfSectionId(sectionId);
        if (minted != null) {
            return minted;
        }
        if (!isRealTab(sectionId)) {
            return null;
        }
        ResourceLocation hub = LiveColors.findParent(sectionId);
        return hub != null && !hub.equals(sectionId) && isRealTab(hub) ? hub : sectionId;
    }

    public static boolean isEditableInTabCreator(ResourceLocation sectionId) {
        ResourceLocation owner = ownerTabOf(sectionId);
        return owner != null && isRealTab(owner);
    }

    public static boolean isRealTab(ResourceLocation id) {
        return BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id) || TabLayoutStore.hasLayout(id);
    }

    public static void openInTabCreator(Screen parent, ResourceLocation sectionId) {
        ResourceLocation owner = ownerTabOf(sectionId);
        if (owner == null) {
            return;
        }
        ScreenSwoosh.drill(() -> new TabEditorScreen(parent, owner), Config.SWOOSH_TAB_STUDIO);
    }

    public static List<String> poolFor(ResourceLocation sectionId) {
        List<String> direct = BannerPools.poolFor(sectionId);
        if (!direct.isEmpty()) {
            return direct;
        }
        ResourceLocation owner = TabLayout.ownerOfSectionId(sectionId);
        return owner == null ? List.of() : BannerPools.poolFor(owner);
    }
}

