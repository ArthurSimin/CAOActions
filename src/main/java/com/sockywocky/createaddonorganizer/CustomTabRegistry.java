package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.sockywocky.createaddonorganizer.mixin.CreativeModeTabAccessor;

public final class CustomTabRegistry {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, createaddonorganizer.MODID);

    private static final List<DeferredHolder<CreativeModeTab, CreativeModeTab>> HOLDERS = new ArrayList<>();

    private CustomTabRegistry() {}

    public static void register(IEventBus modEventBus) {
        TabLayoutStore.load();
        int count = TabLayoutStore.registeredSlotCount();
        for (int i = 0; i < count; i++) {
            final int slot = i;
            HOLDERS.add(TABS.register(TabLayout.SLOT_PREFIX + slot, () -> CreativeModeTab.builder()
                    .title(LiveTabName.of(TabLayout.idForSlot(slot)))
                    .icon(() -> iconFor(slot))
                    .displayItems((params, output) -> fill(slot, output))
                    .build()));
        }
        TABS.register(modEventBus);
        createaddonorganizer.LOGGER.info("[CAO] registered {} custom tab slots ({} in use)",
                count, TabLayoutStore.usedSlotCount());
    }

    public static boolean isCustomTab(ResourceLocation id) {
        return TabLayout.slotOf(id) >= 0;
    }

    public static CreativeModeTab tabForSlot(int slot) {
        return slot >= 0 && slot < HOLDERS.size() ? HOLDERS.get(slot).get() : null;
    }

    public static void invalidateIcon(ResourceLocation id) {
        CreativeModeTab tab = id == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        if (tab != null) {
            ((CreativeModeTabAccessor) tab).setIconItemStack(null);
        }
    }

    private static ItemStack iconFor(int slot) {
        TabLayout tab = TabLayoutStore.bySlot(slot);
        return tab == null ? new ItemStack(Items.BARRIER) : tab.iconStack();
    }

    private static void fill(int slot, CreativeModeTab.Output output) {
        TabLayout tab = TabLayoutStore.bySlot(slot);
        if (tab == null) {
            return;
        }
        for (ItemStack stack : tab.resolvedItems()) {
            output.accept(stack);
        }
    }
}
