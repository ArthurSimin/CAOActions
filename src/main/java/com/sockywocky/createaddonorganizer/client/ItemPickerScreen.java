package com.sockywocky.createaddonorganizer.client;

import java.util.function.Consumer;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ItemPickerScreen extends Screen {

    private final Screen returnTo;
    private final Consumer<String> onPicked;
    private EditBox search;
    private ItemGridWidget grid;

    public ItemPickerScreen(Screen returnTo, Consumer<String> onPicked) {
        super(Component.translatable("createaddonorganizer.tabs.pickItem"));
        this.returnTo = returnTo;
        this.onPicked = onPicked;
    }

    @Override
    protected void init() {
        int w = MenuLayout.panelWidth(this.width);
        int x = MenuLayout.panelX(this.width);

        search = new EditBox(this.font, x, MenuLayout.ROW_1, w, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.tabs.searchItems"));
        search.setHint(Component.translatable("createaddonorganizer.tabs.searchItems"));
        search.setResponder(s -> grid.setEntries(ItemLibrary.search(s)));
        addRenderableWidget(search);

        int top = MenuLayout.nextRow(MenuLayout.ROW_1);
        int bottom = MenuLayout.listBottom(this.height, 0);
        grid = new ItemGridWidget(x, top, w, bottom - top);
        grid.setEntries(ItemLibrary.search(""));
        grid.setOnClick(entry -> {
            Sfx.snap();
            onPicked.accept(entry.id());
            onClose();
        });
        addRenderableWidget(grid);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(x, MenuLayout.doneY(this.height), w, MenuLayout.ROW_H).build());
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        Component tip = grid.tooltipAt(mouseX, mouseY);
        if (tip != null) {
            g.renderTooltip(this.font, tip, mouseX, mouseY);
        }
    }
}
