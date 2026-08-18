package com.sockywocky.createaddonorganizer;

import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;

import com.sockywocky.createaddonorganizer.mixin.CreativeModeTabAccessor;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

public final class TabSearchIndex {

    private static int repaired;

    private TabSearchIndex() {}

    public static void repair(CreativeModeTab tab) {
        if (!(tab instanceof CreativeModeTabAccessor accessor)) {
            return;
        }
        Set<ItemStack> current;
        try {
            current = accessor.getDisplayItemsSearchTab();
        } catch (Throwable t) {
            return;
        }
        if (current == null || current.isEmpty() || current instanceof ObjectLinkedOpenCustomHashSet) {
            return;
        }
        Set<ItemStack> rebuilt = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (ItemStack stack : current) {
            if (stack != null && !stack.isEmpty()) {
                rebuilt.add(stack);
            }
        }
        accessor.setDisplayItemsSearchTab(rebuilt);
        if (repaired++ == 0) {
            createaddonorganizer.LOGGER.info("[CAO] rebuilt a creative tab's search index with the vanilla "
                    + "item-and-components lookup, so \"is in group\" filters and tab search match it again");
        }
    }
}
