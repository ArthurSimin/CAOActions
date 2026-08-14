package com.sockywocky.createaddonorganizer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforgespi.language.IModInfo;

@Mixin(ModListScreen.class)
public abstract class ModListSearchMixin {

    private static final String SEARCH_ALIASES = "cao createaddonorganizer create addon organizer";

    @WrapOperation(method = "lambda$reloadMods$10",
            require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
    private static String createaddonorganizer$searchableDisplayName(IModInfo info, Operation<String> original) {
        String name = original.call(info);
        return createaddonorganizer.MODID.equals(info.getModId()) ? name + " " + SEARCH_ALIASES : name;
    }
}
