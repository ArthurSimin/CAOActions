package com.sockywocky.createaddonorganizer.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.CondensedCreativeSupport;
import com.sockywocky.createaddonorganizer.TabOrder;
import com.sockywocky.createaddonorganizer.client.CollapseSync;
import com.sockywocky.createaddonorganizer.client.ItemGroupSlots;
import com.sockywocky.createaddonorganizer.client.SectionIndexPanel;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    protected abstract void refreshCurrentTabContents(Collection<ItemStack> items);

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"))
    private void createaddonorganizer$itemGroupTooltip(ItemStack stack,
            CallbackInfoReturnable<List<Component>> cir) {
        Slot hovered = ((AbstractContainerScreenAccessor) this).getHoveredSlot();
        Component line = ItemGroupSlots.tooltipFor((CreativeModeInventoryScreen) (Object) this, hovered);
        List<Component> lines = cir.getReturnValue();
        if (line == null || lines == null) {
            return;
        }
        List<Component> copy = new ArrayList<>(lines);
        copy.add(line);
        cir.setReturnValue(copy);
    }

    @WrapOperation(
            method = "init",
            at = @At(value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;"))
    private List<CreativeModeTab> createaddonorganizer$hideAbsorbedFromTabBar(
            Operation<List<CreativeModeTab>> original) {
        List<CreativeModeTab> all = original.call();
        List<CreativeModeTab> visible = all.stream().filter(tab -> {

            if (tab.getType() == CreativeModeTab.Type.CATEGORY
                    && (tab.getDisplayItems() == null || tab.getDisplayItems().isEmpty())) {
                return false;
            }
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            return id == null || !AbsorbedTabs.IDS.contains(id);
        }).toList();
        return TabOrder.apply(visible);
    }

    @ModifyArg(
            method = "renderLabels",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"),
            index = 2)
    private int createaddonorganizer$shiftTitleX(int x) {
        return SectionIndexPanel.active() ? SectionIndexPanel.titleX() : x;
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$itemGroupClick(Slot slot, int slotId, int button, ClickType type,
            CallbackInfo ci) {
        if (ItemGroupSlots.slotClicked((CreativeModeInventoryScreen) (Object) this, slot)) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void createaddonorganizer$renderSectionIndex(GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        ItemGroupSlots.render((CreativeModeInventoryScreen) (Object) this, guiGraphics);
        SectionIndexPanel.render((CreativeModeInventoryScreen) (Object) this, guiGraphics, mouseX, mouseY);
        CollapseSync.tick();
        CondensedCreativeSupport.consumeResync(this);

        ResourceLocation tabId = selectedTab == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(selectedTab);
        if (createaddonorganizer.reconcileOnTabView(tabId)) {
            this.refreshCurrentTabContents(selectedTab.getDisplayItems());
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$sectionIndexClick(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (SectionIndexPanel.mouseClicked((CreativeModeInventoryScreen) (Object) this, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$sectionIndexRelease(double mouseX, double mouseY, int button,
            CallbackInfoReturnable<Boolean> cir) {
        if (SectionIndexPanel.mouseReleased((CreativeModeInventoryScreen) (Object) this, mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void createaddonorganizer$sectionIndexScroll(double mouseX, double mouseY, double scrollX,
            double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (SectionIndexPanel.mouseScrolled((CreativeModeInventoryScreen) (Object) this, mouseX, mouseY, scrollY)) {
            cir.setReturnValue(true);
        }
    }

}
