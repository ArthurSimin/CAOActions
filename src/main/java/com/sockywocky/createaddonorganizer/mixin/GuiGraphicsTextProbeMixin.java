package com.sockywocky.createaddonorganizer.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.sockywocky.createaddonorganizer.client.TextProbe;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsTextProbeMixin {

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
            at = @At("HEAD"))
    private void createaddonorganizer$pushComponent(Font font, Component text, int x, int y, int color,
            boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        TextProbe.push(text);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
            at = @At("RETURN"))
    private void createaddonorganizer$popComponent(Font font, Component text, int x, int y, int color,
            boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        TextProbe.pop();
    }

    @Inject(method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("HEAD"))
    private void createaddonorganizer$pushCentered(Font font, Component text, int x, int y, int color,
            CallbackInfo ci) {
        TextProbe.push(text);
    }

    @Inject(method = "drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("RETURN"))
    private void createaddonorganizer$popCentered(Font font, Component text, int x, int y, int color,
            CallbackInfo ci) {
        TextProbe.pop();
    }

    @Inject(method = "drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V",
            at = @At("HEAD"))
    private void createaddonorganizer$pushWrapped(Font font, FormattedText text, int x, int y, int lineWidth,
            int color, CallbackInfo ci) {
        TextProbe.push(text);
    }

    @Inject(method = "drawWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V",
            at = @At("RETURN"))
    private void createaddonorganizer$popWrapped(Font font, FormattedText text, int x, int y, int lineWidth,
            int color, CallbackInfo ci) {
        TextProbe.pop();
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I",
            at = @At("HEAD"))
    private void createaddonorganizer$recordSequence(Font font, FormattedCharSequence text, float x, float y,
            int color, boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        TextProbe.record(font, text, x, y, ((GuiGraphics) (Object) this).pose().last().pose());
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I", at = @At("HEAD"))
    private void createaddonorganizer$recordPlain(Font font, String text, float x, float y, int color,
            boolean dropShadow, CallbackInfoReturnable<Integer> cir) {
        TextProbe.record(font, text, x, y, ((GuiGraphics) (Object) this).pose().last().pose());
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V",
            at = @At("HEAD"))
    private void createaddonorganizer$recordTooltip(Font font, Component text, int mouseX, int mouseY,
            CallbackInfo ci) {
        TextProbe.recordTooltip(text);
    }

    @Inject(method = "renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V",
            at = @At("HEAD"))
    private void createaddonorganizer$recordComponentTooltip(Font font, List<Component> tooltipLines, int mouseX,
            int mouseY, CallbackInfo ci) {
        TextProbe.recordTooltip(tooltipLines);
    }

    @Inject(method = "renderComponentTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void createaddonorganizer$recordStackTooltip(Font font, List<? extends FormattedText> tooltipLines,
            int mouseX, int mouseY, ItemStack stack, CallbackInfo ci) {
        TextProbe.recordTooltip(tooltipLines);
    }
}
