package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import com.sockywocky.createaddonorganizer.Config;

import net.mcexpanded.fancytabsections.Section.Section;
import net.mcexpanded.fancytabsections.Section.SectionAnimatedTextured;
import net.mcexpanded.fancytabsections.Section.SectionColored;
import net.mcexpanded.fancytabsections.Section.SectionTextured;
import net.mcexpanded.fancytabsections.Section.StickySection;
import net.mcexpanded.fancytabsections.creativetab.ConglomerateOfItems;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public record CaoSection(ResourceLocation id, Component title, ColorSpec bannerColor, ResourceLocation texture,
        ColorSpec textColor, ConglomerateOfItems items) implements Section<CaoSection>, StickySection {

    static final int CONTENT_H = SectionBanner.HEIGHT;

    public CaoSection(ResourceLocation id, Component title, ColorSpec bannerColor, ColorSpec textColor, ConglomerateOfItems items) {
        this(id, title, bannerColor, null, textColor, items);
    }

    public CaoSection withBanner(ColorSpec spec) {
        return new CaoSection(id, title, spec, null, textColor, items);
    }

    public CaoSection withTexture(ResourceLocation newTexture) {
        return new CaoSection(id, title, bannerColor, newTexture, textColor, items);
    }

    public CaoSection withTextColor(ColorSpec spec) {
        return new CaoSection(id, title, bannerColor, texture, spec, items);
    }

    public CaoSection withTitle(Component newTitle) {
        return new CaoSection(id, newTitle, bannerColor, texture, textColor, items);
    }

    public static Component titleOf(Section<?> section) {
        if (section instanceof CaoSection s) {
            return s.title();
        }
        if (section instanceof SectionColored s) {
            return s.title;
        }
        if (section instanceof SectionTextured s) {
            return s.title;
        }
        if (section instanceof SectionAnimatedTextured s) {
            return s.title;
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(section.id());
        return tab != null ? tab.getDisplayName() : Component.literal(section.id().toString());
    }

    @Override
    public boolean collapsible() {
        return Config.showCollapseToggle();
    }

    @Override
    public boolean isSticky() {
        return Config.stickySectionBanners();
    }

    @Override
    public ItemStack icon() {
        ItemStack tabIcon = SafeIcon.of(BuiltInRegistries.CREATIVE_MODE_TAB.get(id));
        if (!tabIcon.isEmpty()) {
            return tabIcon;
        }
        List<ItemStack> stacks = items.getStacks();
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.get(0);
    }

    @Override
    public void render(GuiGraphics g, Font font, int topLeftX, int topLeftY) {
        SectionBanner.draw(g, font, topLeftX, topLeftY, id, title, bannerColor, texture, textColor, true);
    }
}
