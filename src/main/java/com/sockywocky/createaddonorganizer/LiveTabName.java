package com.sockywocky.createaddonorganizer;

import java.util.Optional;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

public final class LiveTabName implements ComponentContents {

    private static final ComponentContents.Type<LiveTabName> TYPE =
            new ComponentContents.Type<>(MapCodec.unit(new LiveTabName(null)), "createaddonorganizer:live_tab_name");

    private final ResourceLocation tabId;

    private LiveTabName(ResourceLocation tabId) {
        this.tabId = tabId;
    }

    public static Component of(ResourceLocation tabId) {
        return MutableComponent.create(new LiveTabName(tabId));
    }

    private String text() {
        TabLayout tab = TabLayoutStore.byId(tabId);
        return tab == null ? "" : tab.displayName();
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> consumer) {
        return consumer.accept(text());
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> consumer, Style style) {
        return consumer.accept(style, text());
    }

    @Override
    public ComponentContents.Type<?> type() {
        return TYPE;
    }

    @Override
    public String toString() {
        return text();
    }
}
