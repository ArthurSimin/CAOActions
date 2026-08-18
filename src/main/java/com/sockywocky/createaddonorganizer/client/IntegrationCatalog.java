package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

public final class IntegrationCatalog {

    public record Entry(String modId, String name, String descriptionKey, String modrinth, String curseforge,
            boolean required) {

        public boolean installed() {
            return ModList.get().isLoaded(modId);
        }

        public Component description() {
            return Component.translatable(descriptionKey);
        }
    }

    private static final List<Entry> ENTRIES = List.of(
            new Entry("fancytabsections", "Fancy Tab Sections",
                    "createaddonorganizer.integrations.fancytabsections",
                    "https://modrinth.com/mod/fancytabsections",
                    "https://www.curseforge.com/minecraft/mc-mods/fancy-tab-sections", true),
            new Entry("create", "Create",
                    "createaddonorganizer.integrations.create",
                    "https://modrinth.com/mod/create",
                    "https://www.curseforge.com/minecraft/mc-mods/create", true),
            new Entry("kubejs", "KubeJS",
                    "createaddonorganizer.integrations.kubejs",
                    "https://modrinth.com/mod/kubejs",
                    "https://www.curseforge.com/minecraft/mc-mods/kubejs", false),
            new Entry("item_obliterator", "Item Obliterator",
                    "createaddonorganizer.integrations.itemObliterator",
                    "https://modrinth.com/mod/item-obliterator",
                    "https://www.curseforge.com/minecraft/mc-mods/item-obliterator", false),
            new Entry("condensed_creative", "Condensed Creative",
                    "createaddonorganizer.integrations.condensedCreative",
                    "https://modrinth.com/mod/condensed-creative",
                    "https://www.curseforge.com/minecraft/mc-mods/condensed-creative", false),
            new Entry("simulated", "Create Simulated",
                    "createaddonorganizer.integrations.simulated",
                    "https://modrinth.com/mod/create-aeronautics",
                    "https://www.curseforge.com/minecraft/mc-mods/create-aeronautics", false));

    private IntegrationCatalog() {}

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static int installedCount() {
        int n = 0;
        for (Entry entry : ENTRIES) {
            if (entry.installed()) {
                n++;
            }
        }
        return n;
    }
}
