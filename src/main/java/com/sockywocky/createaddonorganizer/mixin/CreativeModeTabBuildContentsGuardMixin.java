package com.sockywocky.createaddonorganizer.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;

import com.sockywocky.createaddonorganizer.GuardLog;
import com.sockywocky.createaddonorganizer.TabSearchIndex;

import net.minecraft.world.item.CreativeModeTab;

@Mixin(value = CreativeModeTab.class, priority = 2000)
public class CreativeModeTabBuildContentsGuardMixin {
    @WrapMethod(method = "buildContents")
    private void createaddonorganizer$guardBuildContents(CreativeModeTab.ItemDisplayParameters params,
            Operation<Void> original) {
        try {
            original.call(params);
        } catch (Throwable t) {
            GuardLog.report("Suppressed a crash while rebuilding a creative tab's contents (some addon's "
                    + "listener is broken); leaving that tab as-is instead of crashing the game", t);
        }
        try {
            TabSearchIndex.repair((CreativeModeTab) (Object) this);
        } catch (Throwable t) {
            GuardLog.report("could not rebuild a creative tab's search index", t);
        }
    }
}
