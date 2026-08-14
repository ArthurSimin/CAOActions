package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.TabLayoutStore;

@Mixin(CreativeModeTab.class)
public abstract class CreativeModeTabDisplayMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$nameOverride(CallbackInfoReturnable<Component> cir) {
        if (!TabLayoutStore.hasOverrides()) {
            return;
        }
        Component override = TabLayoutStore.nameOverride(createaddonorganizer$id());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "getIconItem", at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$iconOverride(CallbackInfoReturnable<ItemStack> cir) {
        if (!TabLayoutStore.hasOverrides()) {
            return;
        }
        ItemStack override = TabLayoutStore.iconOverride(createaddonorganizer$id());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    private ResourceLocation createaddonorganizer$id() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.getKey((CreativeModeTab) (Object) this);
    }
}
