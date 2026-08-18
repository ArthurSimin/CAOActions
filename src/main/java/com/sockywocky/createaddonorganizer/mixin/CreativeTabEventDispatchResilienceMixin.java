package com.sockywocky.createaddonorganizer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.sockywocky.createaddonorganizer.GuardLog;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.EventHooks;

@Mixin(value = EventHooks.class, priority = 2000, remap = false)
public abstract class CreativeTabEventDispatchResilienceMixin {

    @WrapOperation(method = "onCreativeModeTabBuildContents", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/fml/ModLoader;postEvent(Lnet/neoforged/bus/api/Event;)V"))
    private static void createaddonorganizer$resilientPostEvent(Event event, Operation<Void> original) {
        try {
            original.call(event);
        } catch (Throwable t) {
            GuardLog.report("A creative tab's BuildCreativeModeTabContentsEvent listener (from some other "
                    + "mod) threw; keeping that tab's already-generated items instead of losing all of them", t);
            createaddonorganizer.recoverAbortedDispatch(event);
        }
        createaddonorganizer.applyLayoutAfterDispatch(event);
    }
}
