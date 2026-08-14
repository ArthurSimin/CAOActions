package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.CondensedCreativeSupport;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public class ItemGroupsScreen extends Screen {
    private static final int ROW_H = 22;

    private final Screen returnTo;
    private final Consumer<TabLayout> onApply;

    private TabLayout layout;
    private GroupList list;
    private int listX;
    private int listW;
    private int listTop;
    private int listBottom;
    private Component status;

    public ItemGroupsScreen(Screen returnTo, TabLayout layout, Consumer<TabLayout> onApply) {
        super(Component.translatable("createaddonorganizer.groups.title"));
        this.returnTo = returnTo;
        this.layout = layout;
        this.onApply = onApply;
    }

    @Override
    protected void init() {
        listW = Math.min(400, this.width - 60);
        listX = (this.width - listW) / 2;
        listTop = 48;
        listBottom = this.height - 78;

        list = new GroupList(this.minecraft, listW, listBottom - listTop, listTop, ROW_H);
        list.setX(listX);
        addRenderableWidget(list);
        rebuild();

        int half = listW / 2 - 2;
        int y = this.height - 72;
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.groups.import"),
                        b -> importFromResources())
                .bounds(listX, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.groups.import.tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.groups.export"),
                        b -> exportToFile())
                .bounds(listX + listW - half, y, half, 20)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.groups.export.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(listX, y + 24, listW, 20).build());
    }

    private void rebuild() {
        if (list == null) {
            return;
        }
        list.reset();
        for (TabLayout.ItemGroup group : layout.safeItemGroups()) {
            list.addEntry(new Row(group));
        }
    }

    private void apply(TabLayout updated) {
        layout = updated;
        onApply.accept(updated);
        rebuild();
    }

    private void importFromResources() {
        List<CondensedEntryIo.Imported> found = CondensedEntryIo.scanResources();
        if (found.isEmpty()) {
            status = Component.translatable("createaddonorganizer.groups.import.none");
            Sfx.denied();
            return;
        }
        CondensedEntryIo.Result result = CondensedEntryIo.importInto(layout, found);
        if (result.count() == 0) {
            status = Component.translatable("createaddonorganizer.groups.import.noMatch", found.size());
            Sfx.denied();
            return;
        }
        apply(result.layout());
        status = Component.translatable("createaddonorganizer.groups.import.done", result.count());
        Sfx.snap();
    }

    private void exportToFile() {
        try {
            Path file = CondensedEntryIo.export(layout);
            if (file == null) {
                status = Component.translatable("createaddonorganizer.groups.export.empty");
                Sfx.denied();
                return;
            }
            status = Component.translatable("createaddonorganizer.groups.export.done", file.getFileName());
            Sfx.snap();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not write condensed entry export", e);
            status = Component.translatable("createaddonorganizer.groups.export.failed");
            Sfx.denied();
        }
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);

        Component sub = status != null
                ? status
                : Component.translatable("createaddonorganizer.groups.count", layout.itemGroupCount());
        g.drawCenteredString(this.font, sub, this.width / 2, 30, MenuSkin.bodyColor(0xFF8A9AA8));

        if (layout.itemGroupCount() == 0) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.groups.empty"),
                    this.width / 2, listTop + (listBottom - listTop) / 2 - 4, 0xFFAAAAAA);
        }
        if (CondensedCreativeSupport.isLoaded()) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.groups.ccNote"),
                    this.width / 2, this.height - 14, MenuSkin.bodyColor(0xFF6A737B));
        }
    }

    private class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final TabLayout.ItemGroup group;

        Row(TabLayout.ItemGroup group) {
            this.group = group;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            ItemGroupsScreen.this.minecraft.setScreen(new ItemGroupEditScreen(ItemGroupsScreen.this,
                    layout, group.id(), ItemGroupsScreen.this::apply));
            return true;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                g.fill(left, top, left + rowWidth, top + rowHeight, 0x30FFFFFF);
            }
            String iconId = layout.iconItemOf(group.id());
            ItemStack icon = iconId == null ? ItemStack.EMPTY : ItemLibrary.stackOf(iconId);
            SafeIcon.render(g, icon, left + 3, top + (rowHeight - 16) / 2);
            g.drawString(ItemGroupsScreen.this.font, group.displayTitle(), left + 25,
                    top + (rowHeight - 8) / 2, 0xFFFFFFFF);

            Component meta = Component.translatable("createaddonorganizer.groups.rowMeta",
                    layout.membersOf(group.id()).size());
            int w = ItemGroupsScreen.this.font.width(meta);
            g.drawString(ItemGroupsScreen.this.font, meta, left + rowWidth - w - 6,
                    top + (rowHeight - 8) / 2, 0xFFAAAAAA);
        }
    }

    private static class GroupList extends ContainerObjectSelectionList<Row> {

        private final ListGlide glide = new ListGlide();

        GroupList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void reset() {
            clearEntries();
        }

        @Override
        public int addEntry(Row entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return this.width - 12;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }
    }
}
