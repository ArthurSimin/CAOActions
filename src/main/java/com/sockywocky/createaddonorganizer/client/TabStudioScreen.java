package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.ItemObliteratorSupport;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;

public class TabStudioScreen extends Screen {

    private static final long PRIME_BUDGET_NANOS = 10_000_000L;

    private static final long FADE_MS = 300;
    private static final long TITLE_SLIDE_MS = 500;
    private static final long SUB_DELAY_MS = 260;
    private static final long ROWS_DELAY_MS = 340;
    private static final long ROW_STAGGER_MS = 45;
    private static final int ROW_STAGGER_CAP = 8;
    private static final long[] WIDGET_DELAYS = {300, 420, 540, 660, 780};
    private static final long ANIM_TOTAL_MS = 1400;

    private static boolean introShown;

    private enum Group {
        CUSTOM("createaddonorganizer.tabs.group.custom"),
        EDITED("createaddonorganizer.tabs.group.edited"),
        BAR("createaddonorganizer.tabs.group.bar");

        private final String key;

        Group(String key) {
            this.key = key;
        }

        Component title() {
            return Component.translatable(key);
        }
    }

    private final Screen returnTo;
    private TabList list;
    private EditBox filterBox;
    private Button newTabButton;
    private String filterQuery = "";
    private double listScroll;

    private boolean contentsPrimed;
    private boolean priming;
    private int rowCount;
    private long primeStart;
    private boolean primeWorkDone;

    private int listX;
    private int listW;
    private int listTop;
    private int listBottom;

    private long openedMillis = System.currentTimeMillis();
    private final List<AbstractWidget> fadingWidgets = new ArrayList<>();
    private final boolean playIntro;
    private final GlassArrow backArrow = new GlassArrow();
    private long frameNanos;
    private Component hoverTip;

    public TabStudioScreen(Screen returnTo) {
        super(Component.translatable("createaddonorganizer.tabs.title"));
        this.returnTo = returnTo;
        this.playIntro = !introShown;
        introShown = true;
    }

    @Override
    protected void init() {
        if (!contentsPrimed && !priming) {
            priming = true;
            primeWorkDone = false;
            primeStart = System.currentTimeMillis();
        }
        if (list != null) {
            listScroll = list.getScrollAmount();
        }

        fadingWidgets.clear();
        listW = Math.min(400, this.width - 60);
        listX = (this.width - listW) / 2;
        listTop = 62;
        listBottom = this.height - 54;

        filterBox = new EditBox(this.font, listX, 40, listW, 18,
                Component.translatable("createaddonorganizer.tabs.filter"));
        filterBox.setHint(Component.translatable("createaddonorganizer.tabs.filter"));
        filterBox.setMaxLength(64);
        filterBox.setValue(filterQuery);
        filterBox.setResponder(s -> {
            filterQuery = s;
            rebuildList();
        });
        addRenderableWidget(filterBox);
        fadingWidgets.add(filterBox);

        list = new TabList(this.minecraft, listW, listBottom - listTop, listTop, 24);
        list.setX(listX);
        addRenderableWidget(list);
        rebuildList();
        list.setScrollAmount(listScroll);

        int fy = this.height - 48;
        boolean disabler = ItemObliteratorSupport.isLoaded();
        int slots = disabler ? 3 : 2;
        int gap = 4;
        int each = (listW - gap * (slots - 1)) / slots;

        newTabButton = addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.tabs.newTab"), b -> createTab())
                .bounds(listX, fy, each, 20).build());
        fadingWidgets.add(newTabButton);
        fadingWidgets.add(addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.tabs.arranger"),
                        b -> ScreenSwoosh.drill(() -> new TabArrangerScreen(this), Config.SWOOSH_TAB_STUDIO))
                .bounds(disabler ? listX + each + gap : listX + listW - each, fy, each, 20).build()));
        if (disabler) {
            fadingWidgets.add(addRenderableWidget(Button.builder(
                            Component.translatable("createaddonorganizer.tabs.itemDisabler"),
                            b -> ScreenSwoosh.drill(() -> new ItemDisablerScreen(this), Config.SWOOSH_TAB_STUDIO))
                    .bounds(listX + listW - each, fy, each, 20)
                    .tooltip(Tooltip.create(
                            Component.translatable("createaddonorganizer.tabs.itemDisabler.tooltip")))
                    .build()));
        }

        fadingWidgets.add(addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(listX, fy + 24, listW, 20).build()));
    }

    private void renderBackArrow(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = GlassArrow.leftX();
        int y = GlassArrow.top(this.height);
        boolean hovered = GlassArrow.contains(mouseX, mouseY, x, y);
        backArrow.render(g, x, y, false, hovered, delta);
        if (hovered) {
            hoverTip = Component.translatable("createaddonorganizer.colors.title");
        }
    }

    private boolean animationDone() {
        return !playIntro || System.currentTimeMillis() - openedMillis >= ANIM_TOTAL_MS;
    }

    private float animProgress(long delayMs, long durationMs) {
        if (!playIntro || !Config.animOn(Config.ANIM_MENU_ENTRANCE)) {
            return 1f;
        }
        float t = Mth.clamp((System.currentTimeMillis() - openedMillis - delayMs) / (float) durationMs, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    float rowAlpha(int rowIndex) {
        if (animationDone()) {
            return 1f;
        }
        return animProgress(ROWS_DELAY_MS + Math.min(rowIndex, ROW_STAGGER_CAP) * ROW_STAGGER_MS, FADE_MS);
    }

    static int mulAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * factor);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private void advancePriming() {
        if (this.minecraft.level != null) {
            ClientRegistries.ensureTabContents();
            primeWorkDone = true;
        } else {
            primeWorkDone = ClientRegistries.advancePrime(PRIME_BUDGET_NANOS);
        }
        if (primeWorkDone) {
            rebuildList();
        }
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        list.reset();
        rowCount = 0;
        String query = filterBox == null ? "" : filterBox.getValue().trim().toLowerCase(Locale.ROOT);

        Map<Group, List<Row>> grouped = new LinkedHashMap<>();
        for (Group group : Group.values()) {
            grouped.put(group, new ArrayList<>());
        }

        for (CreativeModeTab tab : studioTabs()) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id == null) {
                continue;
            }
            TabLayout layout = TabLayoutStore.byId(id);
            String name = layout != null && layout.nameOverride() != null
                    ? layout.nameOverride()
                    : tab.getDisplayName().getString();
            if (!matches(query, name, id)) {
                continue;
            }
            Group group;
            if (TabLayout.slotOf(id) >= 0) {
                group = Group.CUSTOM;
            } else if (layout != null) {
                group = Group.EDITED;
            } else {
                group = Group.BAR;
            }
            grouped.get(group).add(new Row(id, tab, name, sectionCountOf(id)));
        }

        boolean flat = !query.isEmpty();
        for (Map.Entry<Group, List<Row>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            if (!flat) {
                list.addEntry(new HeaderRow(entry.getKey().title(), entry.getValue().size()));
            }
            for (Row row : entry.getValue()) {
                list.addEntry(row);
                rowCount++;
            }
        }
    }

    private static List<CreativeModeTab> studioTabs() {
        Set<ResourceLocation> folded = TabArrangerScreen.foldedAway();
        List<CreativeModeTab> out = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        for (CreativeModeTab tab : TabArrangerScreen.editableTabs()) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id == null || folded.contains(id)) {
                continue;
            }
            if (TabLayout.slotOf(id) >= 0 && TabLayoutStore.byId(id) == null) {
                continue;
            }
            if (seen.add(id)) {
                out.add(tab);
            }
        }
        return out;
    }

    static int sectionCountOf(ResourceLocation id) {
        List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(id);
        return sections == null ? 0 : sections.size();
    }

    private static boolean matches(String query, String name, ResourceLocation id) {
        if (query.isEmpty()) {
            return true;
        }
        if (query.startsWith("@")) {
            return id.getNamespace().contains(query.substring(1).trim());
        }
        return name.toLowerCase(Locale.ROOT).contains(query)
                || id.toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private void createTab() {
        TabLayout tab = TabLayoutStore.createCustom(defaultName(), "minecraft:chest");
        if (tab == null) {
            return;
        }
        ScreenSwoosh.drill(() -> new TabEditorScreen(this, tab.id()), Config.SWOOSH_TAB_STUDIO);
    }

    private static String defaultName() {
        return Component.translatable("createaddonorganizer.tabs.defaultTabName",
                TabLayoutStore.usedSlotCount() + 1).getString();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (priming) {
            return false;
        }
        if (button == 0
                && GlassArrow.contains(mouseX, mouseY, GlassArrow.leftX(), GlassArrow.top(this.height))) {
            Sfx.uiClick();
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ScreenSwoosh.pull(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (priming) {
            renderBackground(g, mouseX, mouseY, partialTick);
            g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
            long elapsed = System.currentTimeMillis() - primeStart;
            LoadingSpinner.renderCentered(g, listX, listTop, listW, listBottom - listTop, elapsed);
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.tabs.loading"),
                    this.width / 2, listTop + (listBottom - listTop) / 2 + LoadingSpinner.labelOffset(),
                    MenuSkin.bodyColor(0xFF8A9AA8));
            if (!primeWorkDone) {
                advancePriming();
            }
            if (primeWorkDone) {
                priming = false;
                contentsPrimed = true;
                openedMillis = System.currentTimeMillis();
            }
            return;
        }

        hoverTip = null;
        long now = System.nanoTime();
        float delta = frameNanos == 0 ? 0f : Math.min(0.25f, (now - frameNanos) / 1_000_000_000f);
        frameNanos = now;

        int free = TabLayoutStore.freeSlotCount();
        newTabButton.active = free > 0;
        if (!animationDone()) {
            for (int i = 0; i < fadingWidgets.size(); i++) {
                long delay = WIDGET_DELAYS[Math.min(i, WIDGET_DELAYS.length - 1)];
                fadingWidgets.get(i).setAlpha(Math.max(animProgress(delay, FADE_MS), 0.04f));
            }
        }
        super.render(g, mouseX, mouseY, partialTick);

        float slide = animProgress(0, TITLE_SLIDE_MS);
        int titleY = Math.round(-20 + (12 - -20) * slide);
        int titleAlpha = Math.round(255 * slide);
        if (titleAlpha >= 8) {
            g.drawCenteredString(this.font, this.title, this.width / 2, titleY,
                    (titleAlpha << 24) | 0x00FFFFFF);
        }

        Component sub = free > 0
                ? Component.translatable("createaddonorganizer.tabs.count",
                        rowCount, TabLayoutStore.usedSlotCount(), free)
                : Component.translatable("createaddonorganizer.tabs.full");
        int subAlpha = Math.round(255 * animProgress(SUB_DELAY_MS, FADE_MS));
        if (subAlpha >= 8) {
            g.drawCenteredString(this.font, sub, this.width / 2, 26,
                    mulAlpha(free > 0 ? 0xFFAAAAAA : 0xFFFFAA55, subAlpha / 255f));
        }

        if (rowCount == 0 && subAlpha >= 8) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.tabs.empty"),
                    this.width / 2, this.height / 2 - 4, mulAlpha(0xFFAAAAAA, subAlpha / 255f));
        }

        renderBackArrow(g, mouseX, mouseY, delta);
        if (hoverTip != null) {
            g.renderTooltip(this.font, this.font.split(hoverTip, 200), mouseX, mouseY);
        }
    }

    private abstract class Line extends ContainerObjectSelectionList.Entry<Line> {

        @Override
        public List<? extends GuiEventListener> children() {
            return RowChildren.none();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return RowChildren.none();
        }
    }

    private class HeaderRow extends Line {

        private final Component label;
        private final int count;

        HeaderRow(Component label, int count) {
            this.label = label;
            this.count = count;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            float alpha = TabStudioScreen.this.rowAlpha(index);
            if (alpha <= 0.03f) {
                return;
            }
            g.fill(left, top + rowHeight - 1, left + rowWidth, top + rowHeight,
                    mulAlpha(MenuSkin.accent(0x30FFFFFF), alpha));
            g.drawString(TabStudioScreen.this.font, label, left + 2, top + rowHeight - 11,
                    mulAlpha(MenuSkin.accent(0xFF8A9AA8), alpha));
            String n = String.valueOf(count);
            int w = TabStudioScreen.this.font.width(n);
            g.drawString(TabStudioScreen.this.font, n, left + rowWidth - w - 2, top + rowHeight - 11,
                    mulAlpha(0xFF5C656F, alpha));
        }
    }

    private class Row extends Line {

        private final ResourceLocation id;
        private final CreativeModeTab tab;
        private final String name;
        private final int sections;

        Row(ResourceLocation id, CreativeModeTab tab, String name, int sections) {
            this.id = id;
            this.tab = tab;
            this.name = name;
            this.sections = sections;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }
            Sfx.uiClick();
            ScreenSwoosh.drill(() -> new TabEditorScreen(TabStudioScreen.this, id), Config.SWOOSH_TAB_STUDIO);
            return true;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            float alpha = TabStudioScreen.this.rowAlpha(index);
            if (alpha <= 0.03f) {
                return;
            }
            if (hovered) {
                g.fill(left, top, left + rowWidth, top + rowHeight, mulAlpha(0x30FFFFFF, alpha));
            }
            TabLayout layout = TabLayoutStore.byId(id);
            boolean custom = layout != null && layout.isCustom();

            g.setColor(1f, 1f, 1f, alpha);
            SafeIcon.render(g, custom ? layout.iconStack() : SafeIcon.of(tab),
                    left + 4, top + (rowHeight - 16) / 2);
            g.setColor(1f, 1f, 1f, 1f);
            String label = custom ? layout.displayName() : name;
            g.drawString(TabStudioScreen.this.font, label, left + 26,
                    top + (rowHeight - 8) / 2,
                    mulAlpha(custom || layout != null ? 0xFFFFFFFF : 0xFFC8C8C8, alpha));

            Component meta;
            if (custom) {
                meta = Component.translatable("createaddonorganizer.tabs.rowMeta",
                        layout.itemCount(), layout.sectionCount());
            } else if (sections > 1) {
                meta = Component.translatable("createaddonorganizer.tabs.rowMeta",
                        tab.getDisplayItems() == null ? 0 : tab.getDisplayItems().size(), sections);
            } else {
                meta = Component.translatable("createaddonorganizer.tabs.rowVanilla",
                        tab.getDisplayItems() == null ? 0 : tab.getDisplayItems().size());
            }
            int metaW = TabStudioScreen.this.font.width(meta);
            g.drawString(TabStudioScreen.this.font, meta, left + rowWidth - metaW - 6,
                    top + (rowHeight - 8) / 2, mulAlpha(layout != null ? 0xFFAAAAAA : 0xFF808080, alpha));
        }

    }

    private class TabList extends ContainerObjectSelectionList<Line> {

        private final ListGlide glide = new ListGlide();

        TabList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        void reset() {
            clearEntries();
        }

        @Override
        public int addEntry(Line entry) {
            return super.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return this.width - 12;
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
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            glide.beforeRender(this);
            super.renderWidget(g, mouseX, mouseY, partialTick);
        }
    }
}
