package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

@Mixin(CreativeModeTab.class)
public interface CreativeModeTabAccessor {
    @Accessor("iconItemStack")
    void setIconItemStack(ItemStack stack);

    @Accessor("displayItemsGenerator")
    CreativeModeTab.DisplayItemsGenerator getDisplayItemsGenerator();
}
