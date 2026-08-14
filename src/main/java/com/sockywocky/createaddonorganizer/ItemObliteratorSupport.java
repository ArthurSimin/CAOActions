package com.sockywocky.createaddonorganizer;

import net.neoforged.fml.ModList;

public final class ItemObliteratorSupport {
    public static final String MOD_ID = "item_obliterator";

    private ItemObliteratorSupport() {}

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static String emptyReason() {
        return isLoaded()
                ? "all items removed by Item Obliterator"
                : "another mod removed them after the contents event";
    }

    public static String emptyNote() {
        return isLoaded() ? " (Item Obliterator is installed and blanks tabs after the contents event)" : "";
    }
}

