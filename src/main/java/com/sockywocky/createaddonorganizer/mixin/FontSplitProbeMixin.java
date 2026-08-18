package com.sockywocky.createaddonorganizer.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sockywocky.createaddonorganizer.client.TextProbe;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

@Mixin(Font.class)
public abstract class FontSplitProbeMixin {

    @Inject(method = "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;", at = @At("RETURN"))
    private void createaddonorganizer$rememberSplit(FormattedText text, int maxWidth,
            CallbackInfoReturnable<List<FormattedCharSequence>> cir) {
        TextProbe.rememberSplit(text, cir.getReturnValue());
    }
}
