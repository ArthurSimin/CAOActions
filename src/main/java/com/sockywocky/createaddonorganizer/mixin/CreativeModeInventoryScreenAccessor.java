package com.sockywocky.createaddonorganizer.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeModeInventoryScreenAccessor {
    @Accessor("scrollOffs")
    float getScrollOffs();

    @Accessor("scrollOffs")
    void setScrollOffs(float value);

    @Accessor("selectedTab")
    static CreativeModeTab getSelectedTab() {
        throw new AssertionError();
    }

    @Accessor("CONTAINER")
    static SimpleContainer getContainer() {
        throw new AssertionError();
    }

    @Invoker("checkTabClicked")
    boolean invokeCheckTabClicked(CreativeModeTab tab, double relativeX, double relativeY);

    @Invoker("refreshCurrentTabContents")
    void invokeRefreshCurrentTabContents(Collection<ItemStack> items);
}
