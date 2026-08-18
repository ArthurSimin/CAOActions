package com.sockywocky.createaddonorganizer.client;

import com.sockywocky.createaddonorganizer.Config;

import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public class RenameTabScreen extends Screen {

    private final Screen returnTo;
    private final ResourceLocation tabId;
    private EditBox nameBox;

    public RenameTabScreen(Screen returnTo, ResourceLocation tabId) {
        super(Component.translatable("createaddonorganizer.colors.tabName.rename"));
        this.returnTo = returnTo;
        this.tabId = tabId;
    }

    @Override
    protected void init() {
        int panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);
        int y = this.height / 2 - 20;

        nameBox = new EditBox(this.font, panelX, y, panelW, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.tabName.rename"));
        nameBox.setMaxLength(48);
        nameBox.setValue(currentName());
        nameBox.setHint(Component.literal(realName()));
        addRenderableWidget(nameBox);
        setInitialFocus(nameBox);

        int buttonY = MenuLayout.nextRow(y);
        int half = MenuLayout.split(panelW, 2);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> commit())
                .bounds(panelX, buttonY, half, MenuLayout.ROW_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(MenuLayout.splitX(panelX, panelW, 2, 1), buttonY, half, MenuLayout.ROW_H).build());
    }

    private String realName() {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(tabId);
        return tab == null ? tabId.toString() : tab.getDisplayName().getString();
    }

    private String currentName() {
        TabLayout layout = TabLayoutStore.byId(tabId);
        return layout != null && layout.nameOverride() != null ? layout.nameOverride() : realName();
    }

    private void commit() {
        String value = nameBox.getValue().trim();
        TabLayout layout = TabLayoutStore.byId(tabId);
        if (layout == null) {
            layout = TabLayout.empty(tabId, null, null);
        }
        TabLayoutStore.put(layout.withName(value.isBlank() || value.equals(realName()) ? null : value));
        createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams(), tabId);
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            commit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 36, 0xFFFFFFFF);
    }
}

