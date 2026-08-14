package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

@Mixin(CreativeModeTab.class)
public interface CreativeModeTabDisplayNameAccessor {

    @Accessor("displayName")
    Component createaddonorganizer$rawDisplayName();
}
