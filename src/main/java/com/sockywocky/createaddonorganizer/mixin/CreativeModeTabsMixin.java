package com.sockywocky.createaddonorganizer.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

@Mixin(CreativeModeTabs.class)
public class CreativeModeTabsMixin {

    @WrapOperation(
            method = "buildAllTabContents",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/CreativeModeTabs;streamAllTabs()Ljava/util/stream/Stream;"))
    private static Stream<CreativeModeTab> createaddonorganizer$buildCreateLast(
            Operation<Stream<CreativeModeTab>> original) {
        List<CreativeModeTab> ordered = new ArrayList<>();
        List<CreativeModeTab> deferred = new ArrayList<>();
        for (CreativeModeTab tab : original.call().toList()) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && createaddonorganizer.MANAGED_PARENTS.contains(id)) {
                deferred.add(tab);
            } else {
                ordered.add(tab);
            }
        }
        ordered.addAll(deferred);
        return ordered.stream();
    }

    @Inject(method = "tabs()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void createaddonorganizer$hideAbsorbed(CallbackInfoReturnable<List<CreativeModeTab>> cir) {
        if (AbsorbedTabs.IDS.isEmpty()) {
            return;
        }
        List<CreativeModeTab> original = cir.getReturnValue();
        List<CreativeModeTab> filtered = new ArrayList<>(original.size());
        boolean removedAny = false;
        for (CreativeModeTab tab : original) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id != null && AbsorbedTabs.IDS.contains(id)) {
                removedAny = true;
                continue;
            }
            filtered.add(tab);
        }
        if (removedAny) {
            cir.setReturnValue(filtered);
        }
    }
}
