package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

@Mixin(Tooltip.class)
public interface TooltipMessageAccessor {

    @Accessor("message")
    Component createaddonorganizer$message();
}
