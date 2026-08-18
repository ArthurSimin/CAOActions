package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.SectionCatalog;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

public class BannerAssignmentScreen extends Screen {
    private record TabRow(ResourceLocation id, String label) {}

    private static final int PAD = 10;
    private static final int PREVIEW_W = 48;
    private static final float TITLE_SCALE = 1.6f;

    private final Screen parent;
    private String query = "";
    private double savedScroll;
    private int panelW = MenuLayout.PANEL_W;
    private Map<String, List<TabRow>> allTabs;
    private AssignList list;
    private EditBox search;

    private int chromeX;
    private int chromeY;
    private int chromeW;
    private int chromeH;
    private int searchX;
    private int searchY;
    private int searchW;

    public BannerAssignmentScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.bannerAssign.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        allTabs = groupedTabs();
        panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);

        chromeX = panelX - PAD;
        chromeW = panelW + PAD * 2;
        chromeY = MenuLayout.ROW_1 - PAD;
        chromeH = this.height - 6 - chromeY;

        searchX = panelX;
        searchY = MenuLayout.ROW_1;
        searchW = panelW;
        search = new EditBox(this.font, searchX + 18, searchY, searchW - 24, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.bannerAssign.search"));
        search.setHint(Component.translatable("createaddonorganizer.bannerAssign.search"));
        search.setMaxLength(64);
        search.setBordered(false);
        search.setValue(query);
        search.setResponder(this::onQueryChanged);
        addWidget(search);

        int doneY = chromeY + chromeH - PAD - MenuLayout.ROW_H;
        int listTop = MenuLayout.ROW_1 + MenuLayout.ROW_H + 8;
        int listBottom = doneY - 8;

        list = new AssignList(this.minecraft, panelW, listBottom - listTop, listTop, 24);
        list.setX(panelX);
        list.setRows(visibleTabs());
        addRenderableWidget(list);
        list.setScrollAmount(savedScroll);

        addRenderableWidget(new GlassButton(panelX, doneY, panelW, MenuLayout.ROW_H,
                Component.translatable("gui.done"), b -> onClose()).style(GlassButton.Style.ACCENT));
    }

    private void openPool(TabRow tab) {
        if (list != null) {
            savedScroll = list.scrollTarget();
        }
        ScreenSwoosh.drill(() -> new BannerPoolEditScreen(this, tab.id(), tab.label()),
                Config.SWOOSH_BANNER_EDITOR);
    }

    private void onQueryChanged(String value) {
        if (value.equals(query)) {
            return;
        }
        query = value;
        savedScroll = 0.0d;
        if (list != null) {
            list.setRows(visibleTabs());
        }
    }

    private Map<String, List<TabRow>> visibleTabs() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return allTabs;
        }
        Map<String, List<TabRow>> matches = new LinkedHashMap<>();
        for (Map.Entry<String, List<TabRow>> mod : allTabs.entrySet()) {
            boolean wholeMod = mod.getKey().toLowerCase(Locale.ROOT).contains(needle);
            List<TabRow> rows = new ArrayList<>();
            for (TabRow tab : mod.getValue()) {
                if (wholeMod
                        || tab.label().toLowerCase(Locale.ROOT).contains(needle)
                        || tab.id().toString().toLowerCase(Locale.ROOT).contains(needle)) {
                    rows.add(tab);
                }
            }
            if (!rows.isEmpty()) {
                matches.put(mod.getKey(), rows);
            }
        }
        return matches;
    }

    private static Map<String, List<TabRow>> groupedTabs() {
        Set<ResourceLocation> seen = new LinkedHashSet<>();
        Map<String, List<TabRow>> byMod = new LinkedHashMap<>();

        for (SectionCatalog.Entry entry : SectionCatalog.colorables()) {
            if (entry.readOnly() || entry.tabOwned() || !seen.add(entry.id())) {
                continue;
            }
            byMod.computeIfAbsent(modNameFor(entry.id()), k -> new ArrayList<>())
                    .add(new TabRow(entry.id(), entry.name().getString()));
        }

        for (ModBannerCatalog.ModEntry mod : ModBannerCatalog.entries()) {
            for (ModBannerCatalog.TabEntry tab : mod.tabs()) {
                if (!seen.add(tab.id())) {
                    continue;
                }
                byMod.computeIfAbsent(mod.modName(), k -> new ArrayList<>()).add(new TabRow(tab.id(), tab.label()));
            }
        }
        return byMod;
    }

    private static String modNameFor(ResourceLocation id) {
        return ModList.get().getModContainerById(id.getNamespace())
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(id.getNamespace());
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        GlassSkin.panel(g, chromeX, chromeY, chromeW, chromeH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        String titleText = this.title.getString();
        g.pose().pushPose();
        g.pose().scale(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE);
        g.drawString(this.font, titleText,
                Math.round(this.width / 2f / TITLE_SCALE) - this.font.width(titleText) / 2,
                Math.round(MenuLayout.TITLE_Y / TITLE_SCALE), GlassSkin.titleTextColor(), GlassSkin.shadow());
        g.pose().popPose();

        Component description = Component.translatable("createaddonorganizer.bannerAssign.description");
        g.drawString(this.font, description, this.width / 2 - this.font.width(description) / 2,
                MenuLayout.DESC_Y, GlassSkin.bodyTextColor(), GlassSkin.shadow());

        GlassSkin.widgetBox(g, searchX, searchY, searchW, MenuLayout.ROW_H, search.isFocused());
        GlassSidebar.magnifier(g, searchX + 6, searchY + 6, GlassSkin.bodyTextColor());
        g.pose().pushPose();
        g.pose().translate(0f, (MenuLayout.ROW_H - 8) / 2f, 0f);
        search.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();

        if (list != null && list.isEmpty()) {
            Component empty = Component.translatable("createaddonorganizer.bannerAssign.noMatches");
            g.drawString(this.font, empty, this.width / 2 - this.font.width(empty) / 2,
                    list.getY() + 20, GlassSkin.bodyTextColor(), GlassSkin.shadow());
        }
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    private class AssignList extends ContainerObjectSelectionList<AssignList.Row> {
        private final ListGlide glide = new ListGlide();

        AssignList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void setRows(Map<String, List<TabRow>> grouped) {
            clearEntries();
            for (Map.Entry<String, List<TabRow>> mod : grouped.entrySet()) {
                addEntry(new Row(mod.getKey(), null));
                for (TabRow tab : mod.getValue()) {
                    addEntry(new Row(null, tab));
                }
            }
            setScrollAmount(0.0d);
        }

        boolean isEmpty() {
            return getItemCount() == 0;
        }

        double scrollTarget() {
            return glide.target();
        }

        @Override
        public int getRowWidth() {
            return BannerAssignmentScreen.this.panelW - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final String modName;
            private final TabRow tab;
            private final Button edit;

            Row(String modName, TabRow tab) {
                this.modName = modName;
                this.tab = tab;
                this.edit = tab == null ? null
                        : MenuSkin.markEdit(Button.builder(Component.translatable("createaddonorganizer.colors.edit"),
                                        b -> openPool(tab))
                                .size(44, 20).build());
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return RowChildren.of(edit);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return RowChildren.of(edit);
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (tab == null) {
                    GlassSkin.header(g, font, Component.literal(modName), left,
                            top + (rowHeight - GlassSkin.headerHeight()) / 2 + 2, rowWidth, 1f);
                    return;
                }

                if (hovered) {
                    int accent = GlassSkin.accent();
                    g.fill(left, top, left + rowWidth, top + rowHeight, MenuSkin.fade(accent, 0.12f));
                    g.fill(left, top, left + 2, top + rowHeight, accent);
                }

                int textY = top + (rowHeight - 8) / 2;
                int contentLeft = left + 14;
                int th = BannerTextures.HEIGHT;
                int ty = top + (rowHeight - th) / 2;
                List<String> pool = BannerPools.poolFor(tab.id());
                String rightLabel;

                g.fill(contentLeft - 1, ty - 1, contentLeft + PREVIEW_W + 1, ty + th + 1, GlassSkin.borderColor());
                if (!pool.isEmpty()) {
                    ResourceLocation tex = BannerTextures.resolve(pool.get(0));
                    if (tex != null) {
                        BannerTextures.blitCropped(g, tex, contentLeft, ty, PREVIEW_W, th,
                                BannerAnimation.sheetHeight(tex));
                    }
                    rightLabel = Component.translatable("createaddonorganizer.bannerAssign.count", pool.size()).getString();
                } else {
                    g.fill(contentLeft, ty, contentLeft + PREVIEW_W, ty + th, 0xFF303030);
                    rightLabel = Component.translatable("createaddonorganizer.bannerAssign.unrestricted").getString();
                }

                edit.setX(left + rowWidth - edit.getWidth());
                edit.setY(top + (rowHeight - 20) / 2);
                int labelX = edit.getX() - 10 - font.width(rightLabel);
                int nameX = contentLeft + PREVIEW_W + 8;

                String fitted = font.plainSubstrByWidth(tab.label(), Math.max(0, labelX - 6 - nameX));
                g.drawString(font, fitted, nameX, textY,
                        hovered ? GlassSkin.titleTextColor() : GlassSkin.rowTextColor(), GlassSkin.shadow());
                g.drawString(font, rightLabel, labelX, textY, GlassSkin.bodyTextColor(), GlassSkin.shadow());
                edit.render(g, mouseX, mouseY, partialTick);
            }
        }
    }
}

