package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.mojang.math.Axis;

import net.minecraft.ChatFormatting;
import net.neoforged.fml.ModList;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.CustomTabRegistry;
import com.sockywocky.createaddonorganizer.LayoutApplier;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public class TabEditorScreen extends Screen {

    private static final int CELL = ItemGridWidget.CELL;
    private static final int HEADER_H = 18;

    private static final long HOVER_TOOLTIP_MS = 1000;

    private static final int TIP_BASE = 0xFF8A9AA8;
    private static final long TIP_FADE_MS = 450;
    private static final long TIP_HOLD_MS = 3200;
    private static final long TIP_STEP_MS = TIP_FADE_MS * 2 + TIP_HOLD_MS;
    private static final String[] TIP_KEYS = {
            "createaddonorganizer.tabs.tip.bulk",
            "createaddonorganizer.tabs.tip.history",
            "createaddonorganizer.tabs.tip.divider",
            "createaddonorganizer.tabs.tip.trash",
            "createaddonorganizer.tabs.tip.banner",
            "createaddonorganizer.tabs.tip.group",
    };

    private static final int BANNER_BADGE_W = 14;

    private static final int AUTO_SCROLL_EDGE = 26;
    private static final int AUTO_SCROLL_REACH = 40;
    private static final double AUTO_SCROLL_MIN_SPEED = 60.0;
    private static final double AUTO_SCROLL_MAX_SPEED = 520.0;
    private static final int BANNER_W = SectionBanner.WIDTH;
    private static final int BANNER_H = SectionBanner.HEIGHT;

    private static final int BIN_W = 20;
    private static final int BIN_H = 22;
    private static final int BIN_PAD = 6;

    private static final int DRAG_THRESHOLD = 3;
    private static final int INSET = 1;
    private static final long SLIDE_MS = 130;
    private static final int SPLIT_W = 8;
    private static final int PANE_PAD = 8;
    private static final int MIN_COLS = 6;
    private static final int BANNER_COLS = (BANNER_W + INSET + CELL - 1) / CELL;
    private static final int MIN_PANE = CELL * MIN_COLS + PANE_PAD;

    private static float splitFraction = 0.62f;
    private static long resizeCursor;
    private static final long SETTLE_MS = 150;
    private static final long LID_MS = 160;
    private static final long POP_MS = 170;
    private static final long FLY_MS = 340;
    private static final long FLY_STAGGER = 90;
    private static final int FLY_MAX = 10;
    private static final float FLY_DROP = 28f;
    private static final float FLY_SHRINK = 0.35f;
    private static final long THUNK_MS = 130;

    private static final int ACCENT_BASE = GlassSkin.DEFAULT_ACCENT;

    private static final int WELL_TOP = 0x52000000;
    private static final int WELL_BOTTOM = 0x22000000;
    private static final int WELL_SHADE = 0x58000000;
    private static final int WELL_LIGHT = 0x22FFFFFF;
    private static final int WELL_TOP_ACCENT = 0x5A2B1F07;
    private static final int WELL_BOTTOM_ACCENT = 0x2A2B1F07;
    private static final int WELL_SHADE_ACCENT = 0x62120C01;
    private static final int WELL_LIGHT_ACCENT = 0x44C89A3E;

    private static int accent() {
        return MenuSkin.accent(ACCENT_BASE);
    }

    private static final int EDGE_FADE = 16;
    private static final int BANNER_FADE = 10;
    private static final int SECTION_GAP = 3;
    private static final int SEAM_LINE = 0x60FFFFFF;

    private final Screen returnTo;
    private final ResourceLocation tabId;
    private final CreativeModeTab realTab;

    private EditBox nameBox;
    private ItemGridWidget library;
    private EditBox librarySearch;

    private int contentsX;
    private int contentsY;
    private int contentsW;
    private int contentsH;
    private int contentsScroll;
    private long autoScrollNanos;
    private String hoverItem;
    private long hoverStart;
    private int tipIndex;
    private long tipStart = System.currentTimeMillis();
    private int tipLeft;
    private int tipRight;
    private boolean tipHovered;
    private final SmoothScroll contentsScrollGlide = new SmoothScroll();
    private int contentsHeightPx;
    private int contentsCols;
    private int gridW;
    private int libX;
    private int libY;
    private int libW;
    private int libH;
    private Integer activeSection;

    private boolean resetting;
    private boolean resetPainted;
    private long resetStart;

    private boolean resizing;
    private int lastLibraryCols;
    private boolean overSplit;
    private boolean settling;
    private int settleFromW;
    private int settleToW;
    private long settleStart;
    private boolean contentsScrollbarActive;
    private boolean libScrollbarActive;

    private final Map<TabLayout.Entry, Slide> slides = new IdentityHashMap<>();

    private static final class Slide {
        private float fromX;
        private float fromY;
        private int toX;
        private int toY;
        private long start;
    }

    private final UndoStack<TabLayout> history = new UndoStack<>();
    private Button undoButton;
    private Button redoButton;
    private Button resetSectionButton;
    private Button iconButton;
    private Button addSectionButton;
    private Button groupsButton;
    private final List<TabLayout.Entry> carried = new ArrayList<>();
    private final Set<TabLayout.Entry> carriedSet = identitySet();
    private boolean dragging;
    private boolean frozen;
    private double pressX;
    private double pressY;
    private int dropTarget = -1;

    private ItemLibrary.Entry libPress;
    private boolean libPressResolved;

    private boolean binHot;
    private boolean lidOpen;
    private long lidChangedMillis;
    private long thunkMillis;
    private int landedCount;

    private final List<Fly> flying = new ArrayList<>();
    private final Map<TabLayout.Entry, Long> popStart = new IdentityHashMap<>();

    private EditBox renameBox;
    private int renameEntry = -1;

    private final List<TabLayout.Entry> selection = new ArrayList<>();
    private final Set<TabLayout.Entry> selected = identitySet();
    private boolean ctrlSelecting;
    private boolean ctrlAdding;
    private Button groupButton;

    private static Set<TabLayout.Entry> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    private final List<Placed> placed = new ArrayList<>();
    private final Map<Long, Placed> cellAt = new HashMap<>();

    private record Placed(TabLayout.Entry entry, int index, int x, int y, boolean header, Integer section) {}

    private record HeaderDraw(Placed placed, int x, int y) {}

    private final List<HeaderDraw> pendingHeaders = new ArrayList<>();

    private static final class Fly {
        private final ItemStack stack;
        private final float sy;
        private final long start;
        private boolean landed;

        private Fly(ItemStack stack, float sy, long start) {
            this.stack = stack;
            this.sy = sy;
            this.start = start;
        }
    }

    public TabEditorScreen(Screen returnTo, ResourceLocation tabId) {
        super(Component.translatable("createaddonorganizer.tabs.editor"));
        this.returnTo = returnTo;
        this.tabId = tabId;
        this.realTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(tabId);
    }

    private final Map<String, Integer> containedCounts = new HashMap<>();
    private boolean pendingLive;
    private TabLayout working;

    private TabLayout tab() {
        return working;
    }

    private boolean isCustom() {
        return TabLayout.slotOf(tabId) >= 0;
    }

    private String realName() {
        return realTab == null ? tabId.toString() : realTab.getDisplayName().getString();
    }

    private TabLayout seed() {
        TabLayout stored = TabLayoutStore.byId(tabId);
        if (stored == null) {
            stored = TabLayout.empty(tabId, null, null);
        }
        if (stored.isCustom()) {
            return stored;
        }
        if (SimulatedSupport.isLoaded() && SimulatedSupport.isMainTab(tabId)) {
            return seedFromSimulated(stored);
        }
        List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(tabId);
        if (stored.seeded()) {
            TabLayout healed = healOwnSection(stored);
            return sections == null || sections.isEmpty() ? healed : healSections(healed, sections);
        }
        if (sections != null && !sections.isEmpty()) {
            return seedFromSections(stored, sections);
        }
        if (realTab == null) {
            return stored;
        }
        TabLayout reconciled = stored.reconciledWith(LayoutApplier.producedIds(realTab.getDisplayItems()));
        return reconciled.seeded() ? reconciled : reconciled.withSeeded(reconciled.safeEntries());
    }

    private TabLayout healOwnSection(TabLayout layout) {
        if (layout.sectionCount() == 0 || realTab == null) {
            return layout;
        }
        List<TabLayout.Entry> existing = layout.safeEntries();
        if (!existing.isEmpty() && existing.get(0).isItem()) {
            return layout;
        }
        for (TabLayout.Entry entry : existing) {
            if (entry.isSection() && tabId.equals(layout.sectionIdFor(entry))) {
                return healOwnItems(layout, entry);
            }
        }
        List<TabLayout.Entry> entries = new ArrayList<>(existing);
        TabLayout.Entry header = TabLayout.Entry.section(layout.nextSectionId(), realName(), tabId.toString());
        entries.add(0, header);
        createaddonorganizer.LOGGER.info("[CAO] {} had no section of its own; restoring it at the top", tabId);
        return healOwnItems(layout.withSeeded(entries, layout.nextSectionId() + 1), header);
    }

    private TabLayout healOwnItems(TabLayout layout, TabLayout.Entry header) {
        List<TabLayout.Entry> entries = layout.safeEntries();
        int headerAt = entries.indexOf(header);
        if (headerAt < 0) {
            return layout;
        }
        if (headerAt + 1 < entries.size() && entries.get(headerAt + 1).isItem()) {
            return layout;
        }
        Set<String> claimed = new LinkedHashSet<>(layout.removedSet());
        for (TabLayout.Entry entry : entries) {
            if (entry.isItem()) {
                claimed.add(entry.item());
            }
        }
        List<TabLayout.Entry> restored = new ArrayList<>();
        for (String id : createaddonorganizer.nativeItemsOf(tabId)) {
            if (claimed.add(id)) {
                restored.add(TabLayout.Entry.item(id));
            }
        }
        if (restored.isEmpty()) {
            return layout;
        }
        List<TabLayout.Entry> out = new ArrayList<>(entries);
        out.addAll(headerAt + 1, restored);
        createaddonorganizer.LOGGER.info("[CAO] {}'s own section had no items; restoring {} from the last "
                + "captured contents", tabId, restored.size());
        return layout.withSeeded(out, layout.nextSectionId());
    }

    private TabLayout healSections(TabLayout layout, List<Section<?>> sections) {
        Set<String> known = new LinkedHashSet<>();
        Set<String> claimed = new LinkedHashSet<>(layout.removedSet());
        for (TabLayout.Entry entry : layout.safeEntries()) {
            if (entry.isSection()) {
                ResourceLocation sectionId = layout.sectionIdFor(entry);
                if (sectionId != null) {
                    known.add(sectionId.toString());
                }
            } else if (entry.isItem()) {
                claimed.add(entry.item());
            }
        }

        List<TabLayout.Entry> added = new ArrayList<>();
        int next = layout.nextSectionId();
        int restored = 0;
        for (Section<?> section : sections) {
            ResourceLocation source = section.id();
            if (source == null || tabId.equals(source) || !known.add(source.toString())) {
                continue;
            }
            List<TabLayout.Entry> items = new ArrayList<>();
            for (ItemStack stack : stacksOf(section)) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null && claimed.add(key.toString())) {
                    items.add(TabLayout.Entry.item(key.toString()));
                }
            }
            if (items.isEmpty()) {
                continue;
            }
            added.add(TabLayout.Entry.section(next++, CaoSection.titleOf(section).getString(),
                    source.toString()));
            added.addAll(items);
            restored++;
        }
        if (added.isEmpty()) {
            return layout;
        }
        List<TabLayout.Entry> out = new ArrayList<>(layout.safeEntries());
        out.addAll(added);
        createaddonorganizer.LOGGER.info("[CAO] {} was seeded before its folded sections existed; restoring {} "
                + "of them", tabId, restored);
        return layout.withSeeded(out, next);
    }

    private TabLayout seedFromSimulated(TabLayout base) {
        Map<ResourceLocation, List<ItemStack>> itemsBySection = SimulatedHub.itemsBySection();
        List<TabLayout.Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int next = 0;
        for (SimulatedHub.IndexEntry section : SimulatedHub.allSectionsInOrder()) {
            entries.add(TabLayout.Entry.section(next++, section.title().getString(),
                    section.id().toString()));
            for (ItemStack stack : itemsBySection.getOrDefault(section.id(), List.of())) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null && seen.add(key.toString())) {
                    entries.add(TabLayout.Entry.item(key.toString()));
                }
            }
        }
        return entries.isEmpty() ? base : base.withSeeded(entries, next);
    }

    private TabLayout seedFromSections(TabLayout base, List<Section<?>> sections) {
        List<TabLayout.Entry> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int next = 0;
        for (Section<?> section : sections) {
            ResourceLocation source = section.id();
            entries.add(TabLayout.Entry.section(next++, CaoSection.titleOf(section).getString(),
                    source == null ? null : source.toString()));
            for (ItemStack stack : stacksOf(section)) {
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (key != null && seen.add(key.toString())) {
                    entries.add(TabLayout.Entry.item(key.toString()));
                }
            }
        }
        return base.withSeeded(entries, next);
    }

    private static List<ItemStack> stacksOf(Section<?> section) {
        try {
            if (section instanceof CaoSection cao) {
                List<ItemStack> stacks = cao.items().getStacks();
                if (stacks != null && !stacks.isEmpty()) {
                    return stacks;
                }
            }
        } catch (Throwable ignored) {
            return List.of();
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(section.id());
        if (tab != null && tab.getDisplayItems() != null) {
            return new ArrayList<>(tab.getDisplayItems());
        }
        return List.of();
    }

    private void mutate(TabLayout updated) {
        if (working != null && !working.equals(updated)) {
            history.push(working);
            refreshHistoryButtons();
        }
        applyLayout(updated);
    }

    private void applyLayout(TabLayout updated) {
        working = updated;
        TabLayoutStore.putQuiet(updated);
        pendingLive = true;
        rebuildContainedIndex(updated);
        Set<TabLayout.Entry> live = identitySet();
        live.addAll(updated.safeEntries());
        selected.retainAll(live);
        selection.removeIf(entry -> !selected.contains(entry));
        refreshGroupButton();
        layoutContents();
    }

    private void undo() {
        if (!history.canUndo()) {
            Sfx.denied();
            return;
        }
        applyLayout(history.undo(working));
        refreshHistoryButtons();
    }

    private void redo() {
        if (!history.canRedo()) {
            Sfx.denied();
            return;
        }
        applyLayout(history.redo(working));
        refreshHistoryButtons();
    }

    private void drawHistoryIcon(GuiGraphics g, Button button, float rotation, String fallback) {
        if (button == null) {
            return;
        }
        int argb = MenuSkin.buttonTextColor(button.active, button.isHoveredOrFocused());
        int cx = button.getX() + button.getWidth() / 2;
        int cy = button.getY() + button.getHeight() / 2;
        if (!MenuSkin.arrowIcon(g, cx, cy, rotation, argb)) {
            g.drawCenteredString(this.font, fallback, cx, cy - 4, argb);
        }
    }

    private void refreshHistoryButtons() {
        if (undoButton != null) {
            undoButton.active = history.canUndo();
        }
        if (redoButton != null) {
            redoButton.active = history.canRedo();
        }
        if (resetSectionButton != null) {
            resetSectionButton.active = canResetSection();
        }
        refreshGroupButton();
    }

    private void refreshGroupButton() {
        if (groupButton == null) {
            return;
        }
        boolean dissolve = selectionIsWholeGroup() != null;
        groupButton.active = dissolve || selectableSelection().size() >= 2;
        groupButton.setMessage(Component.translatable(dissolve
                ? "createaddonorganizer.tabs.ungroup"
                : "createaddonorganizer.tabs.group"));
    }

    private List<TabLayout.Entry> selectableSelection() {
        TabLayout tab = tab();
        if (tab == null || selected.isEmpty()) {
            return List.of();
        }
        Set<TabLayout.Entry> live = identitySet();
        live.addAll(tab.safeEntries());
        List<TabLayout.Entry> out = new ArrayList<>(selection.size());
        for (TabLayout.Entry entry : selection) {
            if (entry.isItem() && live.contains(entry)) {
                out.add(entry);
            }
        }
        return out;
    }

    private String selectionIsWholeGroup() {
        TabLayout tab = tab();
        if (tab == null || selected.isEmpty()) {
            return null;
        }
        String groupId = null;
        for (TabLayout.Entry entry : tab.safeEntries()) {
            if (!entry.isItem() || !selected.contains(entry)) {
                continue;
            }
            String id = entry.groupId();
            if (id == null) {
                return null;
            }
            if (groupId == null) {
                groupId = id;
            } else if (!groupId.equals(id)) {
                return null;
            }
        }
        return groupId != null && selected.size() == tab.membersOf(groupId).size() ? groupId : null;
    }

    private void select(TabLayout.Entry entry) {
        if (selected.add(entry)) {
            selection.add(entry);
        }
        refreshGroupButton();
    }

    private void deselect(TabLayout.Entry entry) {
        if (selected.remove(entry)) {
            selection.removeIf(held -> held == entry);
        }
        refreshGroupButton();
    }

    private void clearSelection() {
        selected.clear();
        selection.clear();
        refreshGroupButton();
    }

    private static long cellKey(int x, int y) {
        return ((long) x << 32) ^ (y & 0xFFFFFFFFL);
    }

    private Placed neighbour(Placed p, int dx, int dy) {
        return cellAt.get(cellKey(p.x() + dx * CELL, p.y() + dy * CELL));
    }

    private boolean isSelected(Placed p) {
        return p != null && selected.contains(p.entry());
    }

    private boolean touchesSelection(Placed p) {
        if (p == null) {
            return false;
        }
        return isSelected(neighbour(p, -1, 0)) || isSelected(neighbour(p, 1, 0))
                || isSelected(neighbour(p, 0, -1)) || isSelected(neighbour(p, 0, 1));
    }

    private void beginCtrlSelect(Placed p) {
        ctrlSelecting = true;
        if (selected.contains(p.entry())) {
            ctrlAdding = false;
            deselect(p.entry());
            Sfx.gridStep();
            return;
        }
        ctrlAdding = true;
        if (!selection.isEmpty() && !touchesSelection(p)) {
            clearSelection();
        }
        select(p.entry());
        Sfx.gridStep();
    }

    private void paintCtrlSelect(double mouseX, double mouseY) {
        Placed p = placedAt(mouseX, mouseY);
        if (p == null || p.header() || !p.entry().isItem()) {
            return;
        }
        if (ctrlAdding) {
            if (selected.contains(p.entry()) || !touchesSelection(p)) {
                return;
            }
            select(p.entry());
        } else {
            if (!selected.contains(p.entry())) {
                return;
            }
            deselect(p.entry());
        }
        Sfx.gridStep();
    }

    private void groupSelection() {
        TabLayout tab = tab();
        if (tab == null) {
            return;
        }
        String dissolve = selectionIsWholeGroup();
        if (dissolve != null) {
            mutate(tab.withGroupDissolved(dissolve));
            clearSelection();
            Sfx.snap();
            return;
        }
        List<TabLayout.Entry> members = selectableSelection();
        if (members.size() < 2) {
            Sfx.denied();
            return;
        }
        String title = Component.translatable("createaddonorganizer.tabs.defaultGroupName",
                tab.itemGroupCount() + 1).getString();
        TabLayout updated = tab.withEntriesGrouped(members, title, members.get(0).item());
        if (updated == tab) {
            Sfx.denied();
            return;
        }
        mutate(updated);
        clearSelection();
        Sfx.snap();
        Notice.show(Component.translatable("createaddonorganizer.tabs.group.made", title, members.size()),
                Notice.GREEN);
    }

    private String groupIdAt(Placed p) {
        return p == null || p.header() || !p.entry().isItem() ? null : p.entry().groupId();
    }

    private void rebuildContainedIndex(TabLayout tab) {
        containedCounts.clear();
        containedCounts.putAll(tab.itemCounts());
    }

    private void refreshLive() {
        TabLayoutStore.flush();
        if (!pendingLive) {
            return;
        }
        pendingLive = false;
        CustomTabRegistry.invalidateIcon(tabId);
        createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams(), tabId);
    }

    @Override
    protected void init() {
        if (working == null) {
            working = seed();
        }
        TabLayout tab = tab();
        if (tab == null) {
            onClose();
            return;
        }
        int m = 8;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> onClose())
                .bounds(m, 6, 20, 20).build());

        nameBox = new EditBox(this.font, m + 24, 7, 140, 18,
                Component.translatable("createaddonorganizer.tabs.name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(tab.nameOverride() == null ? realName() : tab.nameOverride());
        nameBox.setResponder(s -> {
            TabLayout current = tab();
            if (current != null) {
                mutate(current.withName(s));
            }
        });
        addRenderableWidget(nameBox);

        iconButton = addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.tabs.icon"),
                        b -> this.minecraft.setScreen(new ItemPickerScreen(this, id -> {
                            TabLayout current = tab();
                            if (current != null) {
                                mutate(current.withIcon(id));
                            }
                        })))
                .bounds(m + 168, 6, 44, 20).build());

        addSectionButton = addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.tabs.addSection"), b -> addSection())
                .bounds(this.width - m - 74, 6, 74, 20).build());

        groupsButton = addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.tabs.groups"), b -> openGroups())
                .bounds(this.width - m - 74 - 62, 6, 58, 20)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.tabs.groups.tooltip")))
                .build());

        layoutPanes();
        int top = contentsY;

        librarySearch = new EditBox(this.font, libX, top + 2, libW - 2, 16,
                Component.translatable("createaddonorganizer.tabs.searchItems"));
        librarySearch.setHint(Component.translatable("createaddonorganizer.tabs.searchItems"));
        librarySearch.setResponder(s -> library.setEntries(ItemLibrary.search(s)));
        addRenderableWidget(librarySearch);

        library = new ItemGridWidget(libX, libY, libW, libH);
        library.setEntries(ItemLibrary.search(""));
        rebuildContainedIndex(tab);
        library.setPlacedCount(entry -> containedCounts.getOrDefault(entry.id(), 0));
        addRenderableWidget(library);

        addRenderableWidget(Button.builder(Component.translatable(isCustom()
                                ? "createaddonorganizer.tabs.delete"
                                : "createaddonorganizer.tabs.reset"),
                        b -> confirmDelete())
                .bounds(m, this.height - 22, 74, 18).build());

        resetSectionButton = addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.tabs.resetSection"), b -> resetSection())
                .bounds(m + 78, this.height - 22, 92, 18)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.tabs.resetSection.tooltip")))
                .build());
        groupButton = addRenderableWidget(Button.builder(
                        Component.translatable("createaddonorganizer.tabs.group"), b -> groupSelection())
                .bounds(m + 174, this.height - 22, 64, 18)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.tabs.group.tooltip")))
                .build());
        undoButton = addRenderableWidget(Button.builder(Component.empty(), b -> undo())
                .bounds(m + 242, this.height - 22, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.undo.tooltip")))
                .build());
        redoButton = addRenderableWidget(Button.builder(Component.empty(), b -> redo())
                .bounds(m + 264, this.height - 22, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.redo.tooltip")))
                .build());
        refreshHistoryButtons();

        layoutContents();
    }

    private void layoutPanes() {
        int m = 8;
        int top = 32;
        int bottom = this.height - 28;
        int span = this.width - m * 2 - SPLIT_W;
        contentsX = m;
        contentsY = top;
        contentsH = bottom - top;

        int minContents = minContentsWidth();
        int maxContentsW = Math.max(minContents, span - MIN_PANE);
        int rawW = Mth.clamp(Math.round(span * splitFraction), minContents, maxContentsW);
        applyContentsWidth(rawW);
    }

    private int minContentsCols() {
        TabLayout current = tab();
        return current != null && current.sectionCount() > 0 ? BANNER_COLS : MIN_COLS;
    }

    private int minContentsWidth() {
        return minContentsCols() * CELL + PANE_PAD;
    }

    private void applyContentsWidth(int w) {
        int m = 8;
        int bottom = this.height - 28;
        contentsW = w;
        contentsCols = Math.max(1, (contentsW - PANE_PAD) / CELL);
        gridW = contentsCols * CELL;
        libX = contentsX + contentsW + SPLIT_W;
        libW = this.width - m - libX;
        libY = contentsY + 22;
        libH = bottom - libY;
        if (library != null) {
            library.setX(libX);
            library.setWidth(libW);
            int libraryCols = library.columns();
            if (resizing && lastLibraryCols > 0 && libraryCols != lastLibraryCols) {
                Sfx.snap();
            }
            lastLibraryCols = libraryCols;
        }
        if (librarySearch != null) {
            librarySearch.setX(libX);
            librarySearch.setWidth(libW - 2);
        }
        layoutContents();
    }

    private int snappedContentsWidth(int rawW) {
        int m = 8;
        int span = this.width - m * 2 - SPLIT_W;
        if (span <= 0) {
            return rawW;
        }
        int minCols = minContentsCols();
        int maxContentsW = Math.max(minCols * CELL + PANE_PAD, span - MIN_PANE);
        int maxCols = Math.max(minCols, (maxContentsW - PANE_PAD) / CELL);
        int cols = Mth.clamp(Math.round((rawW - PANE_PAD) / (float) CELL), minCols, maxCols);
        return cols * CELL + PANE_PAD;
    }

    private void beginSettle(int targetW) {
        if (targetW == contentsW) {
            slides.clear();
            if (library != null) {
                library.setAnimate(false);
            }
            return;
        }
        settling = true;
        settleFromW = contentsW;
        settleToW = targetW;
        settleStart = System.currentTimeMillis();
    }

    private void tickSettle() {
        if (!settling) {
            return;
        }
        long now = System.currentTimeMillis();
        float t = Config.animOn(Config.ANIM_PANE_RESIZE)
                ? Mth.clamp((now - settleStart) / (float) SETTLE_MS, 0f, 1f)
                : 1f;
        float inv = 1f - t;
        float eased = 1f - inv * inv * inv;
        applyContentsWidth(Math.round(Mth.lerp(eased, settleFromW, settleToW)));
        if (t >= 1f) {
            settling = false;
            int m = 8;
            int span = this.width - m * 2 - SPLIT_W;
            if (span > 0) {
                splitFraction = Mth.clamp(contentsW / (float) span, 0f, 1f);
            }
            slides.clear();
            if (library != null) {
                library.setAnimate(false);
            }
        }
    }

    private int maxContentsScroll() {
        return Math.max(0, contentsHeightPx - contentsH);
    }

    private void advanceContentsScroll() {
        contentsScroll = (int) Math.round(contentsScrollGlide.advance());
    }

    private void autoScrollWhileDragging(int mouseX, int mouseY) {
        long now = System.nanoTime();
        long previous = autoScrollNanos;
        autoScrollNanos = now;
        if (!dragging || carried.isEmpty() || previous == 0) {
            return;
        }
        int maxScroll = maxContentsScroll();
        if (maxScroll <= 0 || mouseX < contentsX || mouseX >= contentsX + contentsW) {
            return;
        }
        int top = contentsY;
        int bottom = contentsY + contentsH;
        if (mouseY < top - AUTO_SCROLL_REACH || mouseY >= bottom + AUTO_SCROLL_REACH) {
            return;
        }
        double speed;
        if (mouseY < top + AUTO_SCROLL_EDGE) {
            speed = -edgeRamp(top + AUTO_SCROLL_EDGE - mouseY);
        } else if (mouseY >= bottom - AUTO_SCROLL_EDGE) {
            speed = edgeRamp(mouseY - (bottom - AUTO_SCROLL_EDGE) + 1);
        } else {
            return;
        }

        double dt = Math.min(0.1, (now - previous) / 1_000_000_000.0);
        double target = Mth.clamp(contentsScrollGlide.target() + speed * dt, 0, maxScroll);
        if (target == contentsScrollGlide.target()) {
            return;
        }
        contentsScrollGlide.setTarget(target);
        contentsScroll = (int) Math.round(contentsScrollGlide.advance());
        Sfx.scroll();
        layoutContents();
        if (!frozen) {
            int next = dropTargetAt(mouseX, mouseY);
            if (next >= 0) {
                dropTarget = next;
            }
        }
    }

    private static double edgeRamp(int intoEdge) {
        double frac = Mth.clamp(intoEdge / (double) AUTO_SCROLL_EDGE, 0.0, 1.0);
        return AUTO_SCROLL_MIN_SPEED + (AUTO_SCROLL_MAX_SPEED - AUTO_SCROLL_MIN_SPEED) * frac * frac;
    }

    private void clampContentsScroll() {
        int maxScroll = maxContentsScroll();
        contentsScrollGlide.setTarget(Mth.clamp(contentsScrollGlide.target(), 0, maxScroll));
        contentsScroll = Mth.clamp(contentsScroll, 0, maxScroll);
    }

    private int splitX() {
        return contentsX + contentsW;
    }

    private boolean overSplitter(double mouseX, double mouseY) {
        return mouseX >= splitX() && mouseX < splitX() + SPLIT_W
                && mouseY >= contentsY && mouseY < contentsY + contentsH;
    }

    private void applySplit(double mouseX) {
        int m = 8;
        int span = this.width - m * 2 - SPLIT_W;
        if (span <= 0) {
            return;
        }
        splitFraction = Mth.clamp((float) (mouseX - m - SPLIT_W / 2f) / span, 0f, 1f);
        layoutPanes();
    }

    private void setCursor(boolean resize) {
        if (resize == overSplit) {
            return;
        }
        overSplit = resize;
        long window = this.minecraft.getWindow().getWindow();
        if (resize && resizeCursor == 0L) {
            resizeCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HRESIZE_CURSOR);
        }
        GLFW.glfwSetCursor(window, resize ? resizeCursor : 0L);
    }

    @Override
    public void removed() {
        setCursor(false);
        super.removed();
    }

    private String editorTitle() {
        TabLayout current = tab();
        String override = current == null ? null : current.nameOverride();
        return override == null ? realName() : override;
    }

    private void addSection() {
        TabLayout current = tab();
        if (current == null) {
            return;
        }
        int id = current.nextSectionId();
        mutate(current.withSectionAdded(Component.translatable(
                "createaddonorganizer.tabs.defaultSectionName", current.sectionCount() + 1).getString()));
        activeSection = id;
        layoutPanes();
    }

    private void confirmDelete() {
        TabLayout current = tab();
        if (current == null) {
            return;
        }
        boolean custom = isCustom();
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed && custom) {
                Config.purgeTabSectionConfig(current.id());
                Config.purgeSectionConfig(current.id());
                TabLayoutStore.delete(tabId);
                TabLayoutStore.flush();
                working = null;
                pendingLive = false;
                CustomTabRegistry.invalidateIcon(tabId);
                createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams(), tabId);
                ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
            } else {
                if (confirmed) {
                    resetting = true;
                    resetPainted = false;
                    resetStart = System.currentTimeMillis();
                }
                this.minecraft.setScreen(this);
            }
        }, Component.translatable(custom
                ? "createaddonorganizer.tabs.delete.title"
                : "createaddonorganizer.tabs.reset.title"),
                Component.translatable(custom
                        ? "createaddonorganizer.tabs.delete.message"
                        : "createaddonorganizer.tabs.reset.message", editorTitle())));
    }

    private void performReset() {
        TabLayout current = tab();
        if (current != null) {
            Config.purgeTabSectionConfig(current.id());
        }
        TabLayoutStore.delete(tabId);
        TabLayoutStore.flush();
        working = null;
        pendingLive = false;
        CustomTabRegistry.invalidateIcon(tabId);
        createaddonorganizer.dropParentSections(tabId);
        createaddonorganizer.organize(ClientRegistries.displayParams());
        history.clear();
        activeSection = null;
        renameBox = null;
        renameEntry = -1;
        resetting = false;
        rebuildWidgets();
    }

    private List<TabLayout.Entry> previewEntries() {
        TabLayout tab = tab();
        List<TabLayout.Entry> source = tab == null ? List.of() : tab.safeEntries();
        if (!dragging || carried.isEmpty() || dropTarget < 0) {
            return source;
        }
        List<TabLayout.Entry> out = new ArrayList<>(source.size());
        int insertAt = 0;
        for (int i = 0; i < source.size(); i++) {
            boolean held = carriedSet.contains(source.get(i));
            if (i < dropTarget && !held) {
                insertAt++;
            }
            if (!held) {
                out.add(source.get(i));
            }
        }
        out.addAll(Mth.clamp(insertAt, 0, out.size()), carried);
        return out;
    }

    private void layoutContents() {
        if (frozen) {
            return;
        }
        placed.clear();
        cellAt.clear();
        List<TabLayout.Entry> entries = previewEntries();
        int cols = Math.max(1, contentsCols);
        int y = 0;
        int col = 0;
        boolean firstRow = true;
        Integer section = null;
        for (int i = 0; i < entries.size(); i++) {
            TabLayout.Entry entry = entries.get(i);
            if (entry.isSection()) {
                if (col != 0) {
                    y += CELL;
                    col = 0;
                }
                if (!firstRow) {
                    y += SECTION_GAP;
                }
                section = entry.section();
                placed.add(new Placed(entry, i, 0, y, true, section));
                y += HEADER_H + SECTION_GAP;
            } else if (entry.isItem()) {
                Placed cell = new Placed(entry, i, col * CELL, y, false, section);
                placed.add(cell);
                cellAt.put(cellKey(cell.x(), cell.y()), cell);
                col++;
                if (col >= cols) {
                    col = 0;
                    y += CELL;
                }
            }
            firstRow = false;
        }
        if (col != 0) {
            y += CELL;
        }
        contentsHeightPx = y;
        clampContentsScroll();
        Set<TabLayout.Entry> live = identitySet();
        for (Placed p : placed) {
            live.add(p.entry());
        }
        slides.keySet().retainAll(live);
    }

    private Placed placedAt(double mouseX, double mouseY) {
        int localX = (int) mouseX - contentsX - INSET;
        int localY = (int) mouseY - contentsY + contentsScroll;
        for (Placed p : placed) {
            int h = p.header() ? HEADER_H : CELL;
            int w = p.header() ? gridW : CELL;
            if (localX >= p.x() && localX < p.x() + w && localY >= p.y() && localY < p.y() + h) {
                return p;
            }
        }
        return null;
    }

    private boolean inContents(double mouseX, double mouseY) {
        return mouseX >= contentsX && mouseX < contentsX + contentsW
                && mouseY >= contentsY && mouseY < contentsY + contentsH;
    }

    private boolean overContentsScrollbar(double mouseX, double mouseY) {
        int maxScroll = maxContentsScroll();
        if (maxScroll <= 0) {
            return false;
        }
        int trackX = contentsX + contentsW - 6;
        return mouseX >= trackX && mouseX < trackX + 6 && mouseY >= contentsY && mouseY < contentsY + contentsH;
    }

    private void dragContentsScrollbar(double mouseY) {
        int maxScroll = maxContentsScroll();
        if (maxScroll <= 0) {
            return;
        }
        int thumbH = Math.max(10, contentsH * contentsH / Math.max(1, contentsHeightPx));
        float usable = Math.max(1, contentsH - thumbH);
        float frac = (float) (mouseY - contentsY - thumbH / 2.0) / usable;
        double before = contentsScrollGlide.target();
        double target = Mth.clamp(Math.round(frac * maxScroll), 0, maxScroll);
        contentsScrollGlide.setTarget(target);
        if (before != target) {
            Sfx.scroll();
        }
    }

    private int binX() {
        return this.width - 8 - BIN_W;
    }

    private int binY() {
        return this.height - BIN_H - 3;
    }

    private int binZoneX() {
        return binX() - BIN_PAD;
    }

    private boolean overBin(double mouseX, double mouseY) {
        return mouseX >= binZoneX() && mouseX < this.width
                && mouseY >= binY() - 2 && mouseY < this.height;
    }

    private void setCarried(List<TabLayout.Entry> entries) {
        carried.clear();
        carriedSet.clear();
        carried.addAll(entries);
        carriedSet.addAll(entries);
    }

    private void addCarried(TabLayout.Entry entry) {
        carried.add(entry);
        carriedSet.add(entry);
        Sfx.pickup(carried.size() - 1);
    }

    private void clearCarried() {
        carried.clear();
        carriedSet.clear();
    }

    private List<TabLayout.Entry> sectionBlock(TabLayout.Entry header) {
        TabLayout tab = tab();
        List<TabLayout.Entry> block = new ArrayList<>();
        block.add(header);
        if (tab == null) {
            return block;
        }
        List<TabLayout.Entry> entries = tab.safeEntries();
        int start = TabLayout.indexOfSame(entries, header);
        if (start < 0) {
            return block;
        }
        for (int i = start + 1; i < entries.size() && !entries.get(i).isSection(); i++) {
            block.add(entries.get(i));
        }
        return block;
    }

    private TabLayout.Entry activeSectionHeader() {
        TabLayout tab = tab();
        if (tab == null || activeSection == null) {
            return null;
        }
        for (TabLayout.Entry entry : tab.safeEntries()) {
            if (entry.isSection() && entry.section().equals(activeSection)) {
                return entry;
            }
        }
        return null;
    }

    private boolean canResetSection() {
        TabLayout.Entry header = activeSectionHeader();
        return header != null && header.sourceId() != null;
    }

    private void resetSection() {
        TabLayout tab = tab();
        TabLayout.Entry header = activeSectionHeader();
        if (tab == null || header == null) {
            return;
        }
        List<String> original = sourceItemsOf(header);
        if (original.isEmpty()) {
            Notice.show(Component.translatable("createaddonorganizer.tabs.resetSection.empty"), Notice.RED);
            return;
        }

        Set<String> restored = new LinkedHashSet<>(original);
        List<TabLayout.Entry> out = new ArrayList<>();
        boolean inTarget = false;
        int insertAt = -1;
        for (TabLayout.Entry entry : tab.safeEntries()) {
            if (entry.isSection()) {
                inTarget = entry.equals(header);
                out.add(entry);
                if (inTarget) {
                    insertAt = out.size();
                }
                continue;
            }
            if (entry.isItem() && (inTarget || restored.contains(entry.item()))) {
                continue;
            }
            out.add(entry);
        }
        if (insertAt < 0) {
            return;
        }
        List<TabLayout.Entry> items = new ArrayList<>(restored.size());
        for (String id : restored) {
            items.add(TabLayout.Entry.item(id));
        }
        out.addAll(insertAt, items);

        List<String> stillRemoved = new ArrayList<>(tab.safeRemoved());
        stillRemoved.removeAll(restored);
        mutate(tab.withEntriesAndRemoved(out, stillRemoved));
        Notice.show(Component.translatable("createaddonorganizer.tabs.resetSection.done",
                header.title() == null ? "Section" : header.title(), items.size()), Notice.GREEN);
    }

    private List<String> sourceItemsOf(TabLayout.Entry header) {
        ResourceLocation source = header.sourceId();
        if (source == null) {
            return List.of();
        }
        if (source.equals(tabId)) {
            return createaddonorganizer.nativeItemsOf(tabId);
        }
        CreativeModeTab sourceTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(source);
        if (sourceTab == null || sourceTab.getDisplayItems() == null) {
            return List.of();
        }
        return LayoutApplier.producedIds(sourceTab.getDisplayItems());
    }

    private String activeSectionTitle() {
        TabLayout tab = tab();
        if (tab == null || activeSection == null) {
            return null;
        }
        for (TabLayout.Entry entry : tab.safeEntries()) {
            if (entry.isSection() && entry.section().equals(activeSection)) {
                return entry.title() == null || entry.title().isBlank() ? "Section" : entry.title();
            }
        }
        return null;
    }

    private int sectionEndIndex(int sectionId) {
        TabLayout tab = tab();
        if (tab == null) {
            return -1;
        }
        List<TabLayout.Entry> entries = tab.safeEntries();
        int start = -1;
        for (int i = 0; i < entries.size(); i++) {
            TabLayout.Entry entry = entries.get(i);
            if (entry.isSection() && entry.section() == sectionId) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return -1;
        }
        int end = start + 1;
        while (end < entries.size() && !entries.get(end).isSection()) {
            end++;
        }
        return end;
    }

    private int dropTargetAt(double mouseX, double mouseY) {
        if (!inContents(mouseX, mouseY)) {
            return -1;
        }
        TabLayout tab = tab();
        if (tab == null) {
            return -1;
        }
        List<TabLayout.Entry> source = tab.safeEntries();
        if (!carried.isEmpty() && carried.get(0).isSection()) {
            return sectionDropTarget(source, mouseX, mouseY);
        }
        Placed p = placedAt(mouseX, mouseY);
        if (p == null) {
            return trailingDropTarget(source, mouseX, mouseY);
        }
        if (carriedSet.contains(p.entry())) {
            return -1;
        }
        int index = TabLayout.indexOfSame(source, p.entry());
        if (index < 0) {
            return -1;
        }
        return p.header() ? index + 1 : index;
    }

    private int trailingDropTarget(List<TabLayout.Entry> source, double mouseX, double mouseY) {
        int localX = (int) mouseX - contentsX - INSET;
        int localY = (int) mouseY - contentsY + contentsScroll;
        Placed best = null;
        int bestScore = Integer.MAX_VALUE;
        boolean after = true;
        for (Placed p : placed) {
            int h = p.header() ? HEADER_H : CELL;
            int w = p.header() ? gridW : CELL;
            int dy = localY < p.y() ? p.y() - localY : Math.max(0, localY - (p.y() + h - 1));
            int dx = localX < p.x() ? p.x() - localX : Math.max(0, localX - (p.x() + w - 1));
            int score = dy * 1000 + dx;
            if (score < bestScore) {
                bestScore = score;
                best = p;
                after = localY >= p.y() + h || localX >= p.x() + w / 2;
            }
        }
        if (best == null) {
            return source.size();
        }
        if (carriedSet.contains(best.entry())) {
            return -1;
        }
        int index = TabLayout.indexOfSame(source, best.entry());
        if (index < 0) {
            return source.size();
        }
        if (best.header()) {
            return index + 1;
        }
        return after ? index + 1 : index;
    }

    private int sectionDropTarget(List<TabLayout.Entry> source, double mouseX, double mouseY) {
        Placed p = placedAt(mouseX, mouseY);
        if (p == null) {
            return source.size();
        }
        if (carriedSet.contains(p.entry())) {
            return -1;
        }
        int index = TabLayout.indexOfSame(source, p.entry());
        if (index < 0) {
            return -1;
        }
        while (index > 0 && !source.get(index).isSection()) {
            index--;
        }
        if (carriedSet.contains(source.get(index))) {
            return -1;
        }
        return source.get(index).isSection() ? index : 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (resetting) {
            return true;
        }
        if (button == 1) {
            handleRightClick(mouseX, mouseY);
            return true;
        }
        if (button != 0) {
            return true;
        }
        if (overTip(mouseX, mouseY)) {
            nextTip();
            return true;
        }
        if (overSplitter(mouseX, mouseY)) {
            resizing = true;
            Sfx.grab();
            settling = false;
            return true;
        }
        if (overContentsScrollbar(mouseX, mouseY)) {
            contentsScrollbarActive = true;
            dragContentsScrollbar(mouseY);
            return true;
        }
        if (library != null && library.overScrollbar(mouseX, mouseY)) {
            libScrollbarActive = true;
            library.dragScrollbar(mouseY);
            return true;
        }
        if (renameBox != null) {
            if (renameBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            commitRename();
        }
        if (library != null && library.isMouseOver(mouseX, mouseY)) {
            ItemLibrary.Entry entry = library.entryAt(mouseX, mouseY);
            if (entry != null) {
                libPress = entry;
                libPressResolved = false;
                pressX = mouseX;
                pressY = mouseY;
                return true;
            }
        }
        if (inContents(mouseX, mouseY) && !overBin(mouseX, mouseY)) {
            Placed p = placedAt(mouseX, mouseY);
            if (p != null && tab() != null) {
                if (p.header() && hasControlDown()) {
                    beginRename(p);
                    return true;
                }
                if (!p.header() && p.entry().isItem() && hasControlDown()) {
                    beginCtrlSelect(p);
                    return true;
                }
                if (overHeaderBadge(p, mouseX, mouseY)) {
                    openSectionAppearance(p);
                    return true;
                }
                setCarried(p.header() ? sectionBlock(p.entry()) : List.of(p.entry()));
                dropTarget = TabLayout.indexOfSame(tab().safeEntries(), p.entry());
                pressX = mouseX;
                pressY = mouseY;
                dragging = false;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleRightClick(double mouseX, double mouseY) {
        if (dragging || !inContents(mouseX, mouseY)) {
            return;
        }
        Placed p = placedAt(mouseX, mouseY);
        if (p == null) {
            return;
        }
        if (p.header()) {
            openSectionAppearance(p);
            return;
        }
        String groupId = groupIdAt(p);
        if (groupId != null) {
            openGroupSettings(groupId);
        }
    }

    private void openGroups() {
        TabLayout tab = tab();
        if (tab == null) {
            return;
        }
        this.minecraft.setScreen(new ItemGroupsScreen(this, tab, updated -> {
            if (updated != null) {
                mutate(updated);
                clearSelection();
            }
        }));
    }

    private void openGroupSettings(String groupId) {
        TabLayout tab = tab();
        if (tab == null || tab.itemGroup(groupId) == null) {
            return;
        }
        this.minecraft.setScreen(new ItemGroupEditScreen(this, tab, groupId, updated -> {
            if (updated != null) {
                mutate(updated);
                clearSelection();
            }
        }));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button != 0) {
            return true;
        }
        if (resizing) {
            applySplit(mouseX);
            return true;
        }
        if (contentsScrollbarActive) {
            dragContentsScrollbar(mouseY);
            return true;
        }
        if (libScrollbarActive) {
            if (library != null) {
                library.dragScrollbar(mouseY);
            }
            return true;
        }
        if (ctrlSelecting) {
            paintCtrlSelect(mouseX, mouseY);
            return true;
        }
        if (libPress != null && !libPressResolved && moved(mouseX, mouseY)) {
            beginLibraryDrag();
        }
        if (carried.isEmpty()) {
            return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        }
        if (!dragging && moved(mouseX, mouseY)) {
            dragging = true;
        }
        if (dragging) {
            frozen = hasShiftDown() && carried.get(0).isItem();
            boolean overBin = overBin(mouseX, mouseY);
            if (overBin && !binHot) {
                Sfx.binHover();
            }
            binHot = overBin;
            setLid(binHot);
            if (frozen) {
                if (!binHot) {
                    Placed hovered = placedAt(mouseX, mouseY);
                    if (hovered != null && !hovered.header() && hovered.entry().isItem()
                            && !carriedSet.contains(hovered.entry())) {
                        addCarried(hovered.entry());
                    }
                }
            } else {
                if (!binHot) {
                    int target = dropTargetAt(mouseX, mouseY);
                    if (target >= 0 && target != dropTarget) {
                        dropTarget = target;
                        Sfx.gridStep();
                    }
                }
                layoutContents();
            }
        }
        return true;
    }

    private boolean moved(double mouseX, double mouseY) {
        return Math.abs(mouseX - pressX) > DRAG_THRESHOLD || Math.abs(mouseY - pressY) > DRAG_THRESHOLD;
    }

    private void beginLibraryDrag() {
        libPressResolved = true;
        TabLayout current = tab();
        ItemLibrary.Entry pressed = libPress;
        libPress = null;
        if (current == null || pressed == null) {
            return;
        }
        TabLayout.Entry added = TabLayout.Entry.item(pressed.id());
        TabLayout updated = current.withEntryInserted(added, -1);
        mutate(updated);
        List<TabLayout.Entry> entries = updated.safeEntries();
        if (entries.isEmpty()) {
            return;
        }
        setCarried(List.of(added));
        dropTarget = entries.size() - 1;
        dragging = true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        if (resizing) {
            resizing = false;
            Sfx.release();
            beginSettle(snappedContentsWidth(contentsW));
            return true;
        }
        if (contentsScrollbarActive) {
            contentsScrollbarActive = false;
            return true;
        }
        if (libScrollbarActive) {
            libScrollbarActive = false;
            return true;
        }
        if (ctrlSelecting) {
            ctrlSelecting = false;
            return true;
        }
        if (libPress != null && !libPressResolved) {
            clickLibrary(libPress);
            libPress = null;
            return true;
        }
        libPress = null;
        if (carried.isEmpty()) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        frozen = false;
        if (dragging) {
            if (binHot) {
                binDrop();
            } else {
                layoutContents();
                TabLayout tab = tab();
                if (tab != null) {
                    int target = dropTargetAt(mouseX, mouseY);
                    if (target >= 0) {
                        dropTarget = target;
                    }
                    mutate(tab.withEntries(previewEntries()));
                }
            }
        } else {
            clickEntry(carried.get(0));
        }
        clearCarried();
        dragging = false;
        dropTarget = -1;
        binHot = false;
        setLid(!flying.isEmpty());
        layoutContents();
        return true;
    }

    private void clickEntry(TabLayout.Entry entry) {
        if (!entry.isSection()) {
            return;
        }
        TabLayout tab = tab();
        if (tab == null) {
            return;
        }
        if (hasShiftDown()) {
            if (entry.section().equals(activeSection)) {
                activeSection = null;
            }
            mutate(tab.withEntriesRemoved(List.of(entry)));
            return;
        }
        activeSection = entry.section().equals(activeSection) ? null : entry.section();
    }

    private void clickLibrary(ItemLibrary.Entry entry) {
        TabLayout current = tab();
        if (current == null) {
            return;
        }
        int index = activeSection == null ? -1 : sectionEndIndex(activeSection);
        TabLayout.Entry added = TabLayout.Entry.item(entry.id());
        mutate(current.withEntryInserted(added, index));
        popStart.put(added, System.currentTimeMillis());
    }

    private void binDrop() {
        TabLayout tab = tab();
        if (tab == null) {
            return;
        }
        long now = System.currentTimeMillis();
        int shown = 0;
        boolean animate = Config.animOn(Config.ANIM_BIN);
        long stagger = carried.size() > FLY_MAX ? FLY_STAGGER / 2 : FLY_STAGGER;
        float startY = binY() - FLY_DROP;
        for (TabLayout.Entry entry : carried) {
            if (entry.isItem()) {
                if (animate && shown < FLY_MAX) {
                    flying.add(new Fly(ItemLibrary.stackOf(entry.item()), startY, now + shown * stagger));
                    shown++;
                }
            } else if (entry.section() != null && entry.section().equals(activeSection)) {
                activeSection = null;
            }
        }
        mutate(tab.withEntriesRemoved(new ArrayList<>(carried)));
        setLid(true);
        landedCount = 0;
        if (shown == 0) {
            Sfx.bin();
        }
    }

    private void setLid(boolean open) {
        if (open != lidOpen) {
            lidOpen = open;
            lidChangedMillis = System.currentTimeMillis();
        }
    }

    private float lidProgress() {
        if (!Config.animOn(Config.ANIM_BIN)) {
            return lidOpen ? 1f : 0f;
        }
        float t = Mth.clamp((System.currentTimeMillis() - lidChangedMillis) / (float) LID_MS, 0f, 1f);
        float inv = 1f - t;
        float eased = 1f - inv * inv * inv;
        return lidOpen ? eased : 1f - eased;
    }

    private float[] slidePos(TabLayout.Entry entry, int x, int y) {
        long now = System.currentTimeMillis();
        Slide slide = slides.get(entry);
        if (slide == null) {
            slide = new Slide();
            slide.fromX = x;
            slide.fromY = y;
            slide.toX = x;
            slide.toY = y;
            slide.start = now - SLIDE_MS;
            slides.put(entry, slide);
        } else if (slide.toX != x || slide.toY != y) {
            float t = slideEase(slide.start, now);
            slide.fromX = Mth.lerp(t, slide.fromX, slide.toX);
            slide.fromY = Mth.lerp(t, slide.fromY, slide.toY);
            slide.toX = x;
            slide.toY = y;
            slide.start = now;
        }
        float t = slideEase(slide.start, now);
        return new float[] {Mth.lerp(t, slide.fromX, slide.toX), Mth.lerp(t, slide.fromY, slide.toY)};
    }

    private static float slideEase(long start, long now) {
        if (!Config.animOn(Config.ANIM_ITEM_SLIDE)) {
            return 1f;
        }
        float t = Mth.clamp((now - start) / (float) SLIDE_MS, 0f, 1f);
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private float popScale(TabLayout.Entry placement) {
        Long start = popStart.get(placement);
        if (start == null) {
            return 1f;
        }
        if (!Config.animOn(Config.ANIM_ITEM_POP)) {
            popStart.remove(placement);
            return 1f;
        }
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed >= POP_MS) {
            popStart.remove(placement);
            return 1f;
        }
        float t = elapsed / (float) POP_MS;
        float inv = 1f - t;
        return 0.4f + 0.6f * (1f - inv * inv * inv);
    }

    private void beginRename(Placed p) {
        renameEntry = p.index();
        renameBox = new EditBox(this.font, contentsX + 4, contentsY + p.y() - contentsScroll,
                contentsW - 16, 16, Component.empty());
        renameBox.setMaxLength(48);
        renameBox.setValue(p.entry().title() == null ? "" : p.entry().title());
        renameBox.setFocused(true);
        setFocused(renameBox);
    }

    private void commitRename() {
        if (renameBox == null) {
            return;
        }
        TabLayout tab = tab();
        String value = renameBox.getValue().trim();
        renameBox = null;
        if (tab == null || renameEntry < 0 || renameEntry >= tab.safeEntries().size() || value.isEmpty()) {
            renameEntry = -1;
            return;
        }
        List<TabLayout.Entry> entries = new ArrayList<>(tab.safeEntries());
        TabLayout.Entry old = entries.get(renameEntry);
        if (old.isSection()) {
            entries.set(renameEntry, TabLayout.Entry.section(old.section(), value, old.source()));
            mutate(tab.withEntries(entries));
        }
        renameEntry = -1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (resetting) {
            return true;
        }
        if (Screen.hasControlDown() && renameBox == null) {
            if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasShiftDown()) {
                redo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Z) {
                undo();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_Y) {
                redo();
                return true;
            }
        }
        if (renameBox != null) {
            if (keyCode == 257 || keyCode == 335) {
                commitRename();
                return true;
            }
            if (keyCode == 256) {
                renameBox = null;
                renameEntry = -1;
                return true;
            }
            if (renameBox.keyPressed(keyCode, scanCode, modifiers) || renameBox.canConsumeInput()) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (renameBox != null && renameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inContents(mouseX, mouseY)) {
            int maxScroll = maxContentsScroll();
            double before = contentsScrollGlide.target();
            double newTarget = Mth.clamp(before - scrollY * CELL, 0, maxScroll);
            contentsScrollGlide.setTarget(newTarget);
            Sfx.scrolled(before, newTarget, maxScroll > 0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        commitRename();
        refreshLive();
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        outline(g, RenderType.gui(), x1, y1, x2, y2, color);
    }

    private static void outline(GuiGraphics g, RenderType type, int x1, int y1, int x2, int y2, int color) {
        g.fill(type, x1, y1, x2, y1 + 1, color);
        g.fill(type, x1, y2 - 1, x2, y2, color);
        g.fill(type, x1, y1, x1 + 1, y2, color);
        g.fill(type, x2 - 1, y1, x2, y2, color);
    }

    private void renderContentSeams(GuiGraphics g) {
        RenderType overlay = RenderType.guiOverlay();
        int topY = contentsY - 4;
        g.fill(overlay, 0, topY, this.width, topY + 1, SEAM_LINE);
        g.fillGradient(overlay, 0, topY + 1, this.width, topY + 1 + EDGE_FADE, 0x90000000, 0x00000000, 0);

        int botY = contentsY + contentsH + 3;
        g.fillGradient(overlay, 0, botY - EDGE_FADE, this.width, botY, 0x00000000, 0x90000000, 0);
        g.fill(overlay, 0, botY, this.width, botY + 1, SEAM_LINE);
    }

    private void renderSplitter(GuiGraphics g) {
        int x = splitX();
        boolean hot = resizing || overSplit;
        int mid = contentsY + contentsH / 2;
        g.fill(x + 3, contentsY + 2, x + 5, contentsY + contentsH - 2,
                hot ? MenuSkin.accent(0x60C89A3E) : 0x18FFFFFF);
        for (int i = -2; i <= 2; i++) {
            int gy = mid + i * 5;
            g.fill(x + 2, gy, x + 6, gy + 2, hot ? MenuSkin.accent(GlassSkin.DEFAULT_ACCENT_LIT) : MenuSkin.ruleColor(0x66FFFFFF));
        }
    }

    static void drawCell(GuiGraphics g, int x, int y) {
        drawCell(g, x, y, false);
    }

    static void drawCell(GuiGraphics g, int x, int y, boolean accent) {
        g.fillGradient(x + 1, y + 1, x + CELL - 1, y + CELL - 1,
                accent ? WELL_TOP_ACCENT : WELL_TOP, accent ? WELL_BOTTOM_ACCENT : WELL_BOTTOM);
        int shade = accent ? WELL_SHADE_ACCENT : WELL_SHADE;
        int light = accent ? MenuSkin.accent(WELL_LIGHT_ACCENT) : WELL_LIGHT;
        g.fill(x, y, x + CELL, y + 1, shade);
        g.fill(x, y + 1, x + 1, y + CELL, shade);
        g.fill(x + CELL - 1, y + 1, x + CELL, y + CELL, light);
        g.fill(x + 1, y + CELL - 1, x + CELL - 1, y + CELL, light);
    }

    private static int withAlpha(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255f), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        RenderProfiler.begin(RenderProfiler.BG);
        try {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        } finally {
            RenderProfiler.end(RenderProfiler.BG);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (resetting) {
            renderBackground(g, mouseX, mouseY, partialTick);
            LoadingSpinner.renderCentered(g, 0, 0, this.width, this.height,
                    System.currentTimeMillis() - resetStart);
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.tabs.loading"),
                    this.width / 2, this.height / 2 + LoadingSpinner.labelOffset(),
                    MenuSkin.bodyColor(0xFF8A9AA8));
            if (resetPainted) {
                performReset();
            } else {
                resetPainted = true;
            }
            return;
        }
        tickSettle();
        advanceContentsScroll();
        autoScrollWhileDragging(mouseX, mouseY);
        if (library != null) {
            library.setAnimate(resizing || settling);
        }
        if (resetSectionButton != null) {
            resetSectionButton.active = canResetSection();
        }
        super.render(g, mouseX, mouseY, partialTick);
        TabLayout tab = tab();
        if (tab == null) {
            return;
        }

        setCursor(!dragging && overSplitter(mouseX, mouseY));

        RenderProfiler.begin(RenderProfiler.GRID);
        g.enableScissor(contentsX, contentsY, contentsX + contentsW, contentsY + contentsH);
        g.drawManaged(() -> renderContentsPane(g, tab));
        renderPendingHeaders(g, tab, mouseX, mouseY);
        g.disableScissor();
        RenderProfiler.end(RenderProfiler.GRID);

        g.drawManaged(() -> renderChrome(g));

        if (renameBox != null) {
            renameBox.render(g, mouseX, mouseY, partialTick);
        }

        g.pose().pushPose();
        g.pose().translate(0f, 0f, 200f);
        renderFlying(g);
        renderCarried(g, mouseX, mouseY);
        g.pose().popPose();

        if (tab.itemCount() == 0 && flying.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.tabs.emptyTab"),
                    contentsX + INSET + gridW / 2, contentsY + contentsH / 2 - 4, 0xFFAAAAAA);
        }

        drawHistoryIcon(g, undoButton, 180f, "<");
        drawHistoryIcon(g, redoButton, 0f, ">");

        Component meta = Component.translatable("createaddonorganizer.tabs.rowMeta",
                tab.itemCount(), tab.sectionCount());
        String target = activeSectionTitle();
        Component editing = target == null
                ? Component.translatable("createaddonorganizer.tabs.editingNone")
                : Component.translatable("createaddonorganizer.tabs.editing", target);
        int metaX = redoButton == null ? contentsX + 82 : redoButton.getX() + redoButton.getWidth() + 8;
        int available = binX() - 8 - metaX;
        int metaY = this.height - 18;
        Component inline = Notice.inlineMessage();
        Component shown = inline == null ? meta : inline;
        int shownW = this.font.width(shown);
        int editingX = metaX;
        boolean roomForBoth = shownW + 10 + this.font.width(editing) <= available;
        if (inline != null) {
            Notice.drawInline(g, this.font, metaX, metaY);
            editingX = metaX + shownW + 10;
        } else if (roomForBoth) {
            g.drawString(this.font, meta, metaX, metaY, 0xFFAAAAAA);
            editingX = metaX + shownW + 10;
        }
        if (inline == null || roomForBoth) {
            g.drawString(this.font, editing, editingX, metaY,
                    target == null ? 0xFF6A737B : accent());
        }

        tipHovered = !dragging && overTip(mouseX, mouseY);
        renderControlTip(g);

        if (!dragging) {
            Component tip = library.tooltipAt(mouseX, mouseY);
            if (tip != null) {
                g.renderTooltip(this.font, tip, mouseX, mouseY);
            } else if (carried.isEmpty() && inContents(mouseX, mouseY)
                    && overHeaderBadge(placedAt(mouseX, mouseY), mouseX, mouseY)) {
                g.renderTooltip(this.font, Component.translatable("createaddonorganizer.tabs.editBanner"),
                        mouseX, mouseY);
            } else {
                renderContentsTooltip(g, mouseX, mouseY);
            }
        }
    }

    private void renderControlTip(GuiGraphics g) {
        tipLeft = 0;
        tipRight = 0;
        if (iconButton == null || addSectionButton == null) {
            return;
        }
        int left = iconButton.getX() + iconButton.getWidth() + 8;
        int right = (groupsButton == null ? addSectionButton.getX() : groupsButton.getX()) - 8;
        if (right - left < 40) {
            return;
        }

        long now = System.currentTimeMillis();
        float fade = 1f;
        if (Config.animOn(Config.ANIM_CONTROL_TIPS)) {
            while (now - tipStart >= TIP_STEP_MS) {
                tipStart += TIP_STEP_MS;
                tipIndex = (tipIndex + 1) % TIP_KEYS.length;
            }
            long phase = now - tipStart;
            if (phase < TIP_FADE_MS) {
                fade = phase / (float) TIP_FADE_MS;
            } else if (phase > TIP_FADE_MS + TIP_HOLD_MS) {
                fade = (TIP_STEP_MS - phase) / (float) TIP_FADE_MS;
            }
        } else {
            tipStart = now;
        }

        Component tip = Component.translatable(TIP_KEYS[tipIndex]);
        int width = this.font.width(tip);
        if (width > right - left) {
            return;
        }
        int centerX = (left + right) / 2;
        tipLeft = centerX - width / 2 - 4;
        tipRight = centerX + width / 2 + 4;

        boolean hovered = tipHovered;
        int alpha = Math.round((hovered ? 0xFF : 0xC0) * Mth.clamp(fade, 0f, 1f));
        if (alpha < 8) {
            return;
        }
        int color = MenuSkin.mixColor(TIP_BASE, accent(), hovered ? 0.85f : 0.55f);
        g.drawCenteredString(this.font, tip, centerX, 12, (alpha << 24) | (color & 0x00FFFFFF));
    }

    private boolean overTip(double mouseX, double mouseY) {
        return tipRight > tipLeft && mouseX >= tipLeft && mouseX < tipRight && mouseY >= 6 && mouseY < 26;
    }

    private void nextTip() {
        tipIndex = (tipIndex + 1) % TIP_KEYS.length;
        tipStart = System.currentTimeMillis();
    }

    private void renderContentsTooltip(GuiGraphics g, int mouseX, int mouseY) {
        String hovered = null;
        if (inContents(mouseX, mouseY) && !overBin(mouseX, mouseY) && carried.isEmpty()) {
            Placed p = placedAt(mouseX, mouseY);
            if (p != null && !p.header() && p.entry().isItem()) {
                hovered = p.entry().item();
            }
        }
        if (hovered == null || !hovered.equals(hoverItem)) {
            hoverItem = hovered;
            hoverStart = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - hoverStart < HOVER_TOOLTIP_MS) {
            return;
        }
        ItemStack stack = ItemLibrary.stackOf(hovered);
        if (stack.isEmpty()) {
            return;
        }
        g.renderComponentTooltip(this.font, tooltipLines(stack), mouseX, mouseY);
    }

    private List<Component> tooltipLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>(3);
        lines.add(stack.getHoverName());
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return lines;
        }
        lines.add(Component.literal(ModList.get().getModContainerById(id.getNamespace())
                        .map(container -> container.getModInfo().getDisplayName())
                        .orElse(id.getNamespace()))
                .withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        if (this.minecraft.options.advancedItemTooltips) {
            lines.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    private void renderContentsPane(GuiGraphics g, TabLayout tab) {
        int cullTop = contentsScroll - CELL * 2;
        int cullBottom = contentsScroll + contentsH + CELL * 2;
        pendingHeaders.clear();
        SafeIcon.beginBatch();
        for (Placed p : placed) {
            if (p.y() + CELL < cullTop) {
                continue;
            }
            if (p.y() > cullBottom) {
                break;
            }
            if (dragging && carriedSet.contains(p.entry())) {
                continue;
            }
            float[] pos = slidePos(p.entry(), p.x(), p.y());
            int x = contentsX + INSET + Math.round(pos[0]);
            int y = contentsY + Math.round(pos[1]) - contentsScroll;
            if (y + HEADER_H < contentsY || y > contentsY + contentsH) {
                continue;
            }
            if (p.header()) {
                pendingHeaders.add(new HeaderDraw(p, x, y));
            } else {
                renderItemCell(g, p, x, y);
            }
        }
        SafeIcon.endBatch(g);
        IconAtlas.flushQuads(g);
    }

    private void renderPendingHeaders(GuiGraphics g, TabLayout tab, int mouseX, int mouseY) {
        for (HeaderDraw header : pendingHeaders) {
            renderHeader(g, tab, header.placed(), header.x(), header.y(), mouseX, mouseY);
        }
        pendingHeaders.clear();
    }

    private int headerBadgeX() {
        return contentsX + INSET + Math.max(CELL, gridW) - BANNER_BADGE_W;
    }

    private boolean overHeaderBadge(Placed p, double mouseX, double mouseY) {
        if (p == null || !p.header() || dragging) {
            return false;
        }
        int badgeX = headerBadgeX();
        int top = contentsY + p.y() - contentsScroll;
        return mouseX >= badgeX && mouseX < badgeX + BANNER_BADGE_W
                && mouseY >= top && mouseY < top + HEADER_H;
    }

    private void openSectionAppearance(Placed p) {
        TabLayout tab = tab();
        if (tab == null || p == null || !p.header()) {
            return;
        }
        String title = p.entry().title() == null ? "Section" : p.entry().title();
        activeSection = p.entry().section();
        BannerEditor.open(this, tab.sectionIdFor(p.entry()), Component.literal(title), false);
    }

    private void renderChrome(GuiGraphics g) {
        int maxScroll = maxContentsScroll();
        int fadeL = contentsX + INSET;
        int fadeR = contentsX + INSET + gridW;
        if (contentsScroll > 0) {
            g.fillGradient(RenderType.guiOverlay(), fadeL, contentsY, fadeR, contentsY + EDGE_FADE,
                    0x90000000, 0x00000000, 0);
        }
        if (contentsScroll < maxScroll) {
            g.fillGradient(RenderType.guiOverlay(), fadeL, contentsY + contentsH - EDGE_FADE, fadeR,
                    contentsY + contentsH, 0x00000000, 0x90000000, 0);
        }
        if (maxScroll > 0) {
            int trackX = contentsX + contentsW - 6;
            int thumbH = Math.max(16, Math.round((float) contentsH * contentsH / Math.max(1, contentsHeightPx)));
            float progress = Mth.clamp((float) (contentsScrollGlide.displayed() / maxScroll), 0f, 1f);
            int thumbY = contentsY + Math.round((contentsH - thumbH) * progress);
            Scrollbar.paint(g, trackX, contentsY, 6, contentsH, thumbY, thumbH, contentsScrollbarActive);
        }

        renderContentSeams(g);
        renderSplitter(g);
        renderBin(g);
    }

    private void renderHeader(GuiGraphics g, TabLayout tab, Placed p, int x, int y, int mouseX, int mouseY) {
        int rowW = resizing || settling ? contentsW - INSET - 6 : gridW;
        int rowEnd = x + Math.max(CELL, rowW);
        int bannerEnd = x + BANNER_W + 1;
        boolean active = p.entry().section().equals(activeSection);
        if (bannerEnd < rowEnd) {
            g.fill(bannerEnd, y, rowEnd, y + BANNER_H + 1, 0x40000000);
        }
        if (p.index() != renameEntry) {
            String title = p.entry().title() == null ? "Section" : p.entry().title();
            boolean clipped = bannerEnd > rowEnd;
            if (clipped) {
                g.enableScissor(x, y, rowEnd, y + BANNER_H + 2);
            }
            SectionBanner.drawResolved(g, this.font, x, y, tab.sectionIdFor(p.entry()),
                    Component.literal(title));
            if (clipped) {
                g.disableScissor();
                fadeRightEdge(g, x, y + 1, rowEnd, y + 1 + BANNER_H);
            }
        }
        if (active) {
            outline(g, x, y, rowEnd, y + BANNER_H + 2, accent());
        }
        renderBannerBadge(g, p, y, mouseX, mouseY);
    }

    private void renderBannerBadge(GuiGraphics g, Placed p, int y, int mouseX, int mouseY) {
        if (dragging || resizing || settling || p.index() == renameEntry) {
            return;
        }
        boolean rowHovered = mouseY >= y && mouseY < y + HEADER_H
                && mouseX >= contentsX && mouseX < contentsX + contentsW;
        if (!rowHovered) {
            return;
        }
        int badgeX = headerBadgeX();
        if (badgeX <= contentsX + INSET) {
            return;
        }
        boolean hot = overHeaderBadge(p, mouseX, mouseY);
        int badgeY = y + (HEADER_H - BANNER_BADGE_W) / 2;
        g.fill(badgeX, badgeY, badgeX + BANNER_BADGE_W, badgeY + BANNER_BADGE_W, hot ? 0xE0101418 : 0xA0101418);
        outline(g, badgeX, badgeY, badgeX + BANNER_BADGE_W, badgeY + BANNER_BADGE_W,
                hot ? accent() : withAlpha(accent(), 0.55f));
        g.drawString(this.font, "✎", badgeX + 4, badgeY + 3, hot ? 0xFFFFFFFF : 0xFFC8C8C8, false);
    }

    private static void fadeRightEdge(GuiGraphics g, int left, int top, int right, int bottom) {
        for (int i = 0; i < BANNER_FADE; i++) {
            int sx = right - BANNER_FADE + i;
            if (sx < left) {
                continue;
            }
            int a = Math.round((i + 1) / (float) BANNER_FADE * 0xC0);
            g.fill(sx, top, sx + 1, bottom, a << 24);
        }
    }

    private static final int SELECT_TINT = 0x30FFD86B;
    private static final int SELECT_EDGE = 0xFFFFD86B;
    private static final int SELECT_HEAD = 0xFFFFF3C6;

    private interface Membership {
        boolean covers(Placed p);
    }

    private static boolean sameGroup(Placed p, String groupId) {
        return p != null && !p.header() && p.entry().isItem() && groupId.equals(p.entry().groupId());
    }

    private void connectedFill(GuiGraphics g, RenderType type, Placed p, int x, int y, int color,
            Membership member) {
        int left = member.covers(neighbour(p, -1, 0)) ? x : x + 1;
        int right = member.covers(neighbour(p, 1, 0)) ? x + CELL : x + CELL - 1;
        int top = member.covers(neighbour(p, 0, -1)) ? y : y + 1;
        int bottom = member.covers(neighbour(p, 0, 1)) ? y + CELL : y + CELL - 1;
        g.fill(type, left, top, right, bottom, color);
    }

    private void connectedOutline(GuiGraphics g, RenderType type, Placed p, int x, int y, int inset,
            int color, Membership member) {
        boolean left = member.covers(neighbour(p, -1, 0));
        boolean right = member.covers(neighbour(p, 1, 0));
        boolean up = member.covers(neighbour(p, 0, -1));
        boolean down = member.covers(neighbour(p, 0, 1));
        int x1 = x + inset;
        int y1 = y + inset;
        int x2 = x + CELL - inset;
        int y2 = y + CELL - inset;
        int spanLeft = left ? x : x1;
        int spanRight = right ? x + CELL : x2;
        int spanTop = up ? y : y1;
        int spanBottom = down ? y + CELL : y2;
        if (!up) {
            g.fill(type, spanLeft, y1, spanRight, y1 + 1, color);
        }
        if (!down) {
            g.fill(type, spanLeft, y2 - 1, spanRight, y2, color);
        }
        if (!left) {
            g.fill(type, x1, spanTop, x1 + 1, spanBottom, color);
        }
        if (!right) {
            g.fill(type, x2 - 1, spanTop, x2, spanBottom, color);
        }
    }

    private static void cornerMarks(GuiGraphics g, int x, int y, int color) {
        RenderType over = RenderType.guiOverlay();
        int arm = 4;
        int x1 = x + 2;
        int y1 = y + 2;
        int x2 = x + CELL - 2;
        int y2 = y + CELL - 2;
        g.fill(over, x1, y1, x1 + arm, y1 + 1, color);
        g.fill(over, x1, y1, x1 + 1, y1 + arm, color);
        g.fill(over, x2 - arm, y1, x2, y1 + 1, color);
        g.fill(over, x2 - 1, y1, x2, y1 + arm, color);
        g.fill(over, x1, y2 - 1, x1 + arm, y2, color);
        g.fill(over, x1, y2 - arm, x1 + 1, y2, color);
        g.fill(over, x2 - arm, y2 - 1, x2, y2, color);
        g.fill(over, x2 - 1, y2 - arm, x2, y2, color);
    }

    private boolean isSelectionHead(TabLayout.Entry entry) {
        return !selection.isEmpty() && selection.get(0) == entry;
    }

    private void renderItemCell(GuiGraphics g, Placed p, int x, int y) {
        drawCell(g, x, y, activeSection != null && activeSection.equals(p.section()));

        TabLayout tab = tab();
        String groupId = p.entry().groupId();
        if (tab != null && groupId != null) {
            Membership member = other -> sameGroup(other, groupId);
            connectedFill(g, RenderType.gui(), p, x, y, ItemGroupColors.tint(tab, groupId), member);
            connectedOutline(g, RenderType.gui(), p, x, y, 0, ItemGroupColors.edge(tab, groupId), member);
            if (p.entry().item().equals(tab.iconItemOf(groupId))) {
                cornerMarks(g, x, y, ItemGroupColors.iconEdge(tab, groupId));
            }
        }
        if (selected.contains(p.entry())) {
            RenderType over = RenderType.guiOverlay();
            Membership member = this::isSelected;
            connectedFill(g, over, p, x, y, SELECT_TINT, member);
            connectedOutline(g, over, p, x, y, 1, SELECT_EDGE, member);
            if (isSelectionHead(p.entry())) {
                cornerMarks(g, x, y, SELECT_HEAD);
            }
        }

        ItemStack stack = ItemLibrary.stackOf(p.entry().item());
        float scale = popScale(p.entry());
        if (scale >= 0.999f) {
            if (!IconAtlas.queue(stack, x + 1, y + 1)) {
                SafeIcon.batched(g, stack, x + 1, y + 1);
            }
        } else {
            float cx = x + CELL / 2f;
            float cy = y + CELL / 2f;
            g.pose().pushPose();
            g.pose().translate(cx, cy, 0f);
            g.pose().scale(scale, scale, 1f);
            g.pose().translate(-cx, -cy, 0f);
            SafeIcon.render(g, stack, x + 1, y + 1);
            g.pose().popPose();
        }
    }

    private void renderBin(GuiGraphics g) {
        int x = binX();
        int y = binY();
        float open = lidProgress();
        boolean hot = open > 0.02f;
        boolean active = dragging || !flying.isEmpty();
        float alpha = active ? 1f : 0.45f;
        int edgeRgb = hot ? 0xFF7A70 : 0x9BA6AF;
        int edge = withAlpha(edgeRgb, alpha);
        int rim = withAlpha(hot ? 0xFFA79E : 0xC2CCD4, alpha);
        int body = withAlpha(hot ? 0x40201E : 0x333B42, alpha);
        int deep = withAlpha(hot ? 0x26100F : 0x222930, alpha);
        int slot = withAlpha(edgeRgb, alpha * 0.4f);

        if (active) {
            int zoneX = binZoneX();
            int zoneTop = y - 2;
            int wash = hot ? 0x40FF6B62 : 0x1AFFFFFF;
            g.fill(zoneX, zoneTop, this.width, this.height, wash);
            g.fill(zoneX, zoneTop, zoneX + 1, this.height, withAlpha(hot ? 0xFF6B62 : 0xFFFFFF, hot ? 0.7f : 0.2f));
        }

        long since = System.currentTimeMillis() - thunkMillis;
        float thunk = since < THUNK_MS ? 1f - since / (float) THUNK_MS : 0f;

        int bodyTop = y + 8;
        int bodyBottom = y + BIN_H;
        int rows = bodyBottom - bodyTop;
        float cx = x + BIN_W / 2f;

        g.pose().pushPose();
        float grow = 1f + 0.06f * open;
        g.pose().translate(cx, bodyBottom, 0f);
        g.pose().scale(grow * (1f + 0.12f * thunk), grow * (1f - 0.10f * thunk), 1f);
        g.pose().translate(-cx, -bodyBottom, 0f);

        if (hot) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 130.0);
            outline(g, x - 1, y + 1, x + BIN_W + 1, bodyBottom + 2,
                    withAlpha(0xFF6B62, open * (0.10f + 0.20f * pulse)));
        }

        g.fill(x + 3, bodyBottom, x + BIN_W - 3, bodyBottom + 1, withAlpha(0x000000, alpha * 0.5f));

        for (int i = 0; i < rows; i++) {
            int inset = 2 + Math.round(2f * i / (rows - 1));
            int ry = bodyTop + i;
            int left = x + inset;
            int right = x + BIN_W - inset;
            if (i == rows - 1) {
                g.fill(left, ry, right, ry + 1, edge);
                continue;
            }
            g.fill(left, ry, right, ry + 1, i >= rows - 4 ? deep : body);
            g.fill(left, ry, left + 1, ry + 1, edge);
            g.fill(right - 1, ry, right, ry + 1, edge);
        }
        for (int i = 0; i < 3; i++) {
            int sx = x + 6 + i * 4;
            g.fill(sx, bodyTop + 3, sx + 1, bodyBottom - 4, slot);
        }

        g.fill(x + 1, bodyTop - 2, x + BIN_W - 1, bodyTop, body);
        outline(g, x + 1, bodyTop - 2, x + BIN_W - 1, bodyTop, rim);
        g.pose().popPose();

        g.pose().pushPose();
        float hingeX = x + BIN_W - 2f;
        float hingeY = bodyTop - 2f;
        g.pose().translate(hingeX, hingeY, 0f);
        g.pose().mulPose(Axis.ZP.rotationDegrees(58f * open));
        g.pose().translate(-hingeX, -hingeY, 0f);
        g.fill(x, y + 3, x + BIN_W, bodyTop - 2, body);
        outline(g, x, y + 3, x + BIN_W, bodyTop - 2, rim);
        g.fill(x + BIN_W / 2 - 3, y + 1, x + BIN_W / 2 + 3, y + 3, body);
        outline(g, x + BIN_W / 2 - 3, y + 1, x + BIN_W / 2 + 3, y + 3, edge);
        g.pose().popPose();
    }

    private void renderFlying(GuiGraphics g) {
        if (flying.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        float x = binX() + BIN_W / 2f - 8f;
        float ty = binY() + 10f;
        int mouth = binY() + 7;
        for (Fly fly : flying) {
            long elapsed = now - fly.start;
            if (elapsed < 0) {
                continue;
            }
            float t = Mth.clamp(elapsed / (float) FLY_MS, 0f, 1f);
            float fall = t * t;
            float y = Mth.lerp(fall, fly.sy, ty);
            float scale = 1f - FLY_SHRINK * t;
            int rx = Math.round(x);
            int ry = Math.round(y);
            g.enableScissor(0, 0, this.width, mouth);
            g.pose().pushPose();
            g.pose().translate(x + 8f, y + 8f, 0f);
            g.pose().scale(scale, scale, 1f);
            g.pose().translate(-(x + 8f), -(y + 8f), 0f);
            SafeIcon.render(g, fly.stack, rx, ry);
            g.pose().popPose();
            g.disableScissor();
            if (!fly.landed && t >= 1f) {
                fly.landed = true;
                thunkMillis = now;
                Sfx.binItem(landedCount++);
            }
        }
        flying.removeIf(fly -> now - fly.start > FLY_MS + 60);
        if (flying.isEmpty() && !dragging) {
            if (lidOpen) {
                Sfx.bin();
            }
            setLid(false);
        }
    }

    private void renderCarried(GuiGraphics g, int mouseX, int mouseY) {
        if (!dragging || carried.isEmpty()) {
            return;
        }
        RenderType overlay = RenderType.guiOverlay();
        TabLayout.Entry primary = carried.get(0);
        int count;
        if (primary.isSection()) {
            count = carried.size() - 1;
            String title = primary.title() == null ? "Section" : primary.title();
            int w = this.font.width(title) + 12;
            g.fill(overlay, mouseX - 6, mouseY - 9, mouseX - 6 + w, mouseY + 9, 0xD01E2A36);
            outline(g, overlay, mouseX - 6, mouseY - 9, mouseX - 6 + w, mouseY + 9, accent());
            g.drawString(this.font, title, mouseX, mouseY - 4, 0xFFF2E6CC);
        } else {
            count = carried.size();
            int cards = Math.min(carried.size(), 4);
            for (int i = cards - 1; i >= 0; i--) {
                int x = mouseX - 8 + i * 3;
                int y = mouseY - 8 + i * 3;
                if (i > 0) {
                    g.fill(overlay, x, y, x + 16, y + 16, 0xD0202428);
                    outline(g, overlay, x, y, x + 16, y + 16, 0x66FFFFFF);
                }
                SafeIcon.render(g, ItemLibrary.stackOf(carried.get(i).item()), x, y);
            }
        }
        if (count > 1) {
            Component badge = Component.literal("x" + count);
            int bx = mouseX + 11;
            int by = mouseY - 14;
            g.pose().pushPose();
            g.pose().translate(0f, 0f, 300f);
            g.fill(overlay, bx - 2, by - 2, bx + this.font.width(badge) + 2, by + 10, accent());
            g.drawString(this.font, badge, bx, by, 0xFF241A06, false);
            g.pose().popPose();
        }
    }
}

