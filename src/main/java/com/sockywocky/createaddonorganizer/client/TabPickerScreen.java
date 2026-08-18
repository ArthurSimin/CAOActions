package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.TabLayout;

public class TabPickerScreen extends Screen {

    private final Screen returnTo;
    private final Consumer<CreativeModeTab> onPicked;

    private int panelW = MenuLayout.PANEL_W;
    private String searchQuery = "";
    private EditBox searchBox;
    private TabList list;

    public TabPickerScreen(Screen returnTo, Consumer<CreativeModeTab> onPicked) {
        super(Component.translatable("createaddonorganizer.tabs.pickTab"));
        this.returnTo = returnTo;
        this.onPicked = onPicked;
    }

    @Override
    protected void init() {
        panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);

        searchBox = new EditBox(this.font, panelX, MenuLayout.ROW_1, panelW, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.tabs.searchTabs"));
        searchBox.setHint(Component.translatable("createaddonorganizer.tabs.searchTabs"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(s -> {
            searchQuery = s;
            list.setEntries(candidates(s));
            list.setScrollAmount(0);
        });
        addRenderableWidget(searchBox);

        int listTop = MenuLayout.nextRow(MenuLayout.ROW_1);
        int listBottom = MenuLayout.listBottom(this.height, 0);
        list = new TabList(this.minecraft, panelW, listBottom - listTop, listTop, 22);
        list.setX(panelX);
        list.setEntries(candidates(searchQuery));
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(panelX, MenuLayout.doneY(this.height), panelW, MenuLayout.ROW_H).build());
    }

    private static int itemCount(CreativeModeTab tab) {
        int count = 0;
        for (ItemStack stack : tab.getDisplayItems()) {
            if (stack != null && !stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static List<CreativeModeTab> candidates(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<CreativeModeTab> out = new ArrayList<>();
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (TabLayout.slotOf(id) >= 0) {
                continue;
            }
            CreativeModeTab tab = entry.getValue();
            if (itemCount(tab) == 0) {
                continue;
            }
            if (!q.isEmpty()
                    && !tab.getDisplayName().getString().toLowerCase(Locale.ROOT).contains(q)
                    && !id.toString().toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            out.add(tab);
        }
        out.sort((a, b) -> a.getDisplayName().getString().compareToIgnoreCase(b.getDisplayName().getString()));
        return out;
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, MenuLayout.TITLE_Y, 0xFFFFFFFF);
    }

    private class TabList extends ContainerObjectSelectionList<TabList.Row> {

        private final ListGlide glide = new ListGlide();

        TabList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            glide.beginScroll(this);
            boolean handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            glide.endScroll(this);
            return handled;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            glide.beginScroll(this);
            boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            glide.endScroll(this);
            return handled;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            glide.beforeRender(this);
            super.renderWidget(g, mouseX, mouseY, partialTick);
        }

        void setEntries(List<CreativeModeTab> tabs) {
            replaceEntries(tabs.stream().map(Row::new).toList());
        }

        @Override
        public int getRowWidth() {
            return TabPickerScreen.this.panelW - 32;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {

            private final CreativeModeTab tab;
            private final Component name;
            private final Component meta;

            Row(CreativeModeTab tab) {
                this.tab = tab;
                this.name = tab.getDisplayName();
                this.meta = Component.translatable("createaddonorganizer.tabs.itemCount", itemCount(tab));
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return RowChildren.none();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return RowChildren.none();
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    Sfx.snap();
                    onPicked.accept(tab);
                    onClose();
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered) {
                    g.fill(left, top, left + rowWidth, top + rowHeight, 0x40FFFFFF);
                }
                SafeIcon.render(g, SafeIcon.of(tab), left + 4, top + (rowHeight - 16) / 2);
                g.drawString(TabPickerScreen.this.font, name, left + 26, top + (rowHeight - 8) / 2, 0xFFFFFFFF);
                g.drawString(TabPickerScreen.this.font, meta,
                        left + rowWidth - TabPickerScreen.this.font.width(meta) - 4,
                        top + (rowHeight - 8) / 2, 0xFFAAAAAA);
            }
        }
    }
}
