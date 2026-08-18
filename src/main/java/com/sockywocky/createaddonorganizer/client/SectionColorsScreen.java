package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.UnaryOperator;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.PackDefaults;
import com.sockywocky.createaddonorganizer.SectionCatalog;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;

public class SectionColorsScreen extends Screen {

    private static final int OUTER = 6;
    private static final int PANEL_GAP = 6;
    private static final int SEARCH_H = 18;
    private static final int SEARCH_MAX_W = 260;
    private static final int HELP_SIZE = 12;
    private static final int LIST_PAD = 8;
    private static final int INFO_PAD = 8;

    private static final int SIDE_GUTTER = 26;

    private static final float HEADER_TONE = 0.28f;

    private static final String[] TIP_KEYS = {
            "createaddonorganizer.colors.tip.select",
            "createaddonorganizer.colors.tip.restyle",
            "createaddonorganizer.colors.tip.reorder",
            "createaddonorganizer.colors.tip.rename",
            "createaddonorganizer.colors.tip.delete",
    };
    private static final int TIP_BASE = 0xFF8A9AA8;
    private static final long TIP_FADE_MS = 450;
    private static final long TIP_HOLD_MS = 3200;
    private static final long TIP_STEP_MS = TIP_FADE_MS * 2 + TIP_HOLD_MS;

    private static final int POPUP_PAD = 4;
    private static final int POPUP_W = BannerTextures.WIDTH + POPUP_PAD * 2;
    private static final int POPUP_H = BannerTextures.HEIGHT + POPUP_PAD * 2 + 22;

    private static final long DOUBLE_CLICK_MS = 300;
    private static final int CLASSIC_LIST_PADDING = 16;
    private static final int CLASSIC_PANEL_W = 400;
    private static final int CLASSIC_ROW_1 = 64;
    private static final int CLASSIC_BUTTON_H = 18;
    private static final int BUTTON_FACE_TOP = 2;
    private static final int BUTTON_FACE_BOTTOM = 3;
    private static final int CLASSIC_GAP = 4;

    private static final String UNDERTALE_LINE = "But Nobody Came.";
    private static final String KONAMI_LINE = "DONT put in the konami code";
    private static final String[] EMPTY_STATE_LINES = {
            "no tabs?",
            "You scared everything away :(",
            UNDERTALE_LINE,
            "cheeseburger",
            "nothing to see here...",
            KONAMI_LINE,
            "Add sections via \"Add Section...\" or \"Reset All\"",
    };

    private static final int[] KONAMI_SEQUENCE = {
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT,
    };
    private static final long KONAMI_CLOSE_DELAY_MS = 800;

    private final Screen parent;
    private final ModContainer container;

    private final String emptyStateText = EMPTY_STATE_LINES[new Random().nextInt(EMPTY_STATE_LINES.length)];

    private ColorList list;
    private boolean orderDirty;

    private static final long PRIME_BUDGET_NANOS = 10_000_000L;
    private boolean priming;
    private boolean primeWorkDone;
    private long primeStart;
    private List<SectionCatalog.Entry> pendingOrder;
    private boolean noSections;
    private int listAreaTop;
    private int listAreaBottom;
    private Runnable lastUndo;
    private int konamiProgress;
    private long konamiTriggeredMillis;
    private ResourceLocation renamingId;
    private EditBox renameBox;
    private RenameIconButton renameConfirm;
    private RenameIconButton renameCancel;
    private Component hoverPreviewTooltip;

    private static double lastScroll;
    private static ResourceLocation lastSelectedId;
    private static String lastSearch = "";
    private boolean classic;
    private int listRowWidth = 320;
    private int listCenterX;
    private EditBox searchBox;
    private String searchQuery = "";
    private ResourceLocation selectedId;
    private SectionCatalog.Entry selectedEntry;
    private List<SectionCatalog.Entry> allEntries = List.of();
    private int tipIndex;
    private long tipStart = System.currentTimeMillis();
    private int tipLeft;
    private int tipRight;
    private GlassSidebar sidebar;
    private final InfoPane infoPane = new InfoPane(this);
    private final PanelSlide slide = new PanelSlide();
    private InfoPane.Kind infoPage;
    private Screen pane;
    private ColorPickerScreen editor;
    private int sidebarX;
    private int sidebarW;
    private int contentX;
    private int contentW;
    private int panelTop;
    private int panelBottom;
    private int searchX;
    private int searchY;
    private int searchW;
    private int helpX;
    private final GlassArrow leftArrow = new GlassArrow();
    private final GlassArrow rightArrow = new GlassArrow();
    private final GlassArrow backArrow = new GlassArrow();
    private long frameNanos;
    private SectionCatalog.Entry popupEntry;
    private ResourceLocation lastClickId;
    private long lastClickMillis;

    public SectionColorsScreen(Screen parent, ModContainer container) {
        super(Component.translatable("createaddonorganizer.colors.title"));
        this.parent = parent;
        this.container = container;
        this.selectedId = lastSelectedId;
        this.searchQuery = lastSearch;
    }

    @Override
    protected void init() {
        if (ClientRegistries.needsPriming() && !priming) {
            priming = true;
            primeWorkDone = false;
            primeStart = System.currentTimeMillis();
        }
        searchBox = null;
        classic = Config.classicOrganizerLayout();
        if (classic) {
            infoPage = null;
            pane = null;
        }
        if (pane != null) {
            layoutPanels();
            buildSidebar();
            ((EmbeddedPane) pane).embedInto(contentX, panelTop, contentW, panelBottom - panelTop, this::closePane);
            ((EmbeddedPane) pane).onEmbeddedChanged(this::sectionsChanged);
            pane.init(this.minecraft, this.width, this.height);
            return;
        }
        if (editor != null) {
            if (classic) {
                editor = null;
            } else {
                layoutPanels();
                buildSidebar();
                editor.embedInto(contentX, panelTop, contentW, panelBottom - panelTop, this::closeEditor);
                editor.init(this.minecraft, this.width, this.height);
                return;
            }
        }
        double restoreScroll = list != null ? list.getScrollAmount() : lastScroll;

        int listTop;
        int listBottom;
        if (classic) {
            int panelW = Math.min(CLASSIC_PANEL_W, this.width - 60);
            int panelX = (this.width - panelW) / 2;
            int quarter = (panelW - CLASSIC_GAP * 3) / 4;
            listRowWidth = panelW - CLASSIC_LIST_PADDING * 2;
            listCenterX = this.width / 2;

            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.addSection"),
                            b -> ScreenSwoosh.drill(() -> new AddSectionScreen(this), Config.SWOOSH_ADD_SECTION))
                    .bounds(panelX, CLASSIC_ROW_1, quarter, CLASSIC_BUTTON_H).build());
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.presets"),
                            b -> ScreenSwoosh.drill(() -> new PresetsScreen(this), Config.SWOOSH_PRESETS))
                    .bounds(panelX + quarter + CLASSIC_GAP, CLASSIC_ROW_1, quarter, CLASSIC_BUTTON_H).build());
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.resetOrder"),
                            b -> resetOrder())
                    .bounds(panelX + (quarter + CLASSIC_GAP) * 2, CLASSIC_ROW_1, quarter, CLASSIC_BUTTON_H).build());
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.resetAll"),
                            b -> confirmResetAll())
                    .bounds(panelX + panelW - quarter, CLASSIC_ROW_1, quarter, CLASSIC_BUTTON_H).build());
            listTop = CLASSIC_ROW_1 + CLASSIC_BUTTON_H + 4;
            if (PackDefaults.isActive()) {
                addRenderableWidget(Button.builder(
                                Component.translatable("createaddonorganizer.colors.resetPack"),
                                b -> confirmResetToPack())
                        .bounds(panelX, listTop, panelW, CLASSIC_BUTTON_H)
                        .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.resetPack.tooltip")))
                        .build());
                listTop += CLASSIC_BUTTON_H + 4;
            }
            if (DevMode.isUnlocked()) {
                addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.assignBanners"),
                                b -> ScreenSwoosh.drill(() -> new BannerAssignmentScreen(this), Config.SWOOSH_BANNER_EDITOR))
                        .bounds(panelX, listTop, panelW, CLASSIC_BUTTON_H).build());
                listTop += CLASSIC_BUTTON_H + 4;
            }
            buildSearchBox(panelX, listTop, panelW, CLASSIC_BUTTON_H);
            listTop += CLASSIC_BUTTON_H + 4;
            listBottom = this.height - CLASSIC_BUTTON_H * 2 - 14;
        } else {
            layoutPanels();

            listTop = panelTop + SEARCH_H + 12;
            listBottom = panelBottom - 4;

            if (infoPage == null) {
                searchX = contentX + LIST_PAD;
                searchY = panelTop + 6;
                helpX = contentX + contentW - LIST_PAD - HELP_SIZE;
                searchW = Math.max(60, Math.min(SEARCH_MAX_W, contentW - LIST_PAD * 3 - HELP_SIZE - 60));
                buildSearchBox(searchX, searchY, searchW, SEARCH_H);
            } else {
                infoPane.setBounds(contentX, panelTop + INFO_PAD, contentW,
                        panelBottom - panelTop - INFO_PAD * 2);
                infoPane.build(infoPage, this.font);
            }

            buildSidebar();
        }
        listAreaTop = listTop;
        listAreaBottom = listBottom;

        if (classic) {
            int listW = Math.min(this.width, listRowWidth + CLASSIC_LIST_PADDING * 2);
            list = new ColorList(this.minecraft, listW, listBottom - listTop, listTop, 24);
            list.setX((this.width - listW) / 2);
        } else {
            int listW = contentW - 2;
            list = new ColorList(this.minecraft, listW, listBottom - listTop, listTop, 24);
            list.setX(contentX + 1);
        }

        List<SectionCatalog.Entry> source = orderDirty && pendingOrder != null ? pendingOrder : SectionCatalog.colorables();
        allEntries = new ArrayList<>(source);
        noSections = allEntries.isEmpty();
        list.setEntries(filterEntries(allEntries, searchQuery));
        if (infoPage == null) {
            addRenderableWidget(list);
        }
        list.setScrollAmount(restoreScroll);

        selectedEntry = null;
        if (selectedId != null) {
            for (SectionCatalog.Entry entry : allEntries) {
                if (entry.id().equals(selectedId)) {
                    selectedEntry = entry;
                    break;
                }
            }
            if (selectedEntry == null) {
                selectedId = null;
                lastSelectedId = null;
            }
        }

        if (classic) {
            int panelW = Math.min(CLASSIC_PANEL_W, this.width - 60);
            int panelX = (this.width - panelW) / 2;
            int pairW = (panelW - CLASSIC_BUTTON_H * 2 - CLASSIC_GAP * 3) / 2;
            int footerY = this.height - CLASSIC_BUTTON_H * 2 - 8;
            addRenderableWidget(new HeartButton(panelX, footerY, CLASSIC_BUTTON_H,
                    b -> ScreenSwoosh.drill(() -> new CreditsScreen(this), Config.SWOOSH_CREDITS)));
            addRenderableWidget(new BugButton(panelX + CLASSIC_BUTTON_H + CLASSIC_GAP, footerY,
                    CLASSIC_BUTTON_H, b -> ScreenSwoosh.drill(() -> new BugReportScreen(this), Config.SWOOSH_CREDITS)));
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.style"),
                            b -> ScreenSwoosh.drill(() -> new MenuStyleScreen(this), Config.SWOOSH_MENU_STYLE))
                    .bounds(panelX + (CLASSIC_BUTTON_H + CLASSIC_GAP) * 2, footerY, pairW, CLASSIC_BUTTON_H).build());
            addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.colors.save"),
                            b -> saveOrder())
                    .bounds(panelX + panelW - pairW, footerY, pairW, CLASSIC_BUTTON_H).build());
                addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                    .bounds(panelX, this.height - CLASSIC_BUTTON_H - 4, panelW, CLASSIC_BUTTON_H).build());
        }

    }

    private void buildSearchBox(int x, int y, int w, int h) {
        searchBox = new EditBox(this.font, x, y, w, h, Component.translatable("createaddonorganizer.colors.search.hint"));
        searchBox.setHint(Component.translatable("createaddonorganizer.colors.search.hint"));
        searchBox.setMaxLength(64);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(s -> {
            if (s.equals(searchQuery)) {
                return;
            }
            searchQuery = s;
            lastSearch = s;
            refreshListEntries(0);
        });
        if (classic) {
            addRenderableWidget(searchBox);
        } else {
            searchBox.setBordered(false);
            addWidget(searchBox);
        }
    }

    private void layoutPanels() {
        sidebarW = GlassSidebar.widthFor(this.width);
        boolean sidebarRight = Config.colorsSidebarSide() == Config.SidebarSide.RIGHT;
        panelTop = OUTER;
        panelBottom = this.height - OUTER;
        contentW = this.width - sidebarW - PANEL_GAP - SIDE_GUTTER * 2;
        sidebarX = sidebarRight ? this.width - SIDE_GUTTER - sidebarW : SIDE_GUTTER;
        contentX = sidebarRight ? SIDE_GUTTER : SIDE_GUTTER + sidebarW + PANEL_GAP;
        listRowWidth = contentW - LIST_PAD * 2 - 2;
        listCenterX = contentX + contentW / 2;
    }

    private int contentNav() {
        if (editor != null) {
            return 6;
        }
        if (infoPage == InfoPane.Kind.BUGS) {
            return 5;
        }
        if (infoPage == InfoPane.Kind.CREDITS) {
            return 4;
        }
        if (pane instanceof MenuStyleScreen) {
            return 3;
        }
        if (pane instanceof PresetsScreen) {
            return 2;
        }
        if (pane instanceof AddSectionScreen) {
            return 1;
        }
        return 0;
    }

    private void navTo(int target) {
        slide.play(contentNav(), target);
    }

    private void openEmbedded(ColorPickerScreen next) {
        if (classic) {
            ScreenSwoosh.drill(() -> next, Config.SWOOSH_BANNER_EDITOR);
            return;
        }
        lastScroll = list != null ? list.getScrollAmount() : lastScroll;
        navTo(6);
        infoPage = null;
        editor = next;
        rebuildWidgets();
    }

    private void closeEditor() {
        navTo(0);
        editor = null;
        rebuildWidgets();
    }

    private List<SectionCatalog.Entry> mainSections() {
        List<SectionCatalog.Entry> source = allEntries.isEmpty() ? SectionCatalog.colorables() : allEntries;
        List<SectionCatalog.Entry> out = new ArrayList<>();
        for (SectionCatalog.Entry entry : source) {
            if (entry.parent()) {
                out.add(entry);
            }
        }
        return out;
    }

    private void jumpToSection(ResourceLocation id) {
        if (infoPage != null || pane != null || editor != null) {
            infoPage = null;
            pane = null;
            if (editor != null) {
                editor.releaseEmbedded();
                editor = null;
            }
            rebuildWidgets();
        }
        clearSearch();
        if (list == null) {
            return;
        }
        list.jumpToSection(id);
        selectedId = id;
        lastSelectedId = id;
    }

    private void openPane(Screen next) {
        navTo(next instanceof AddSectionScreen ? 1 : next instanceof PresetsScreen ? 2 : 3);
        if (editor != null) {
            editor.releaseEmbedded();
            editor = null;
        }
        infoPage = null;
        pane = next;
        rebuildWidgets();
    }

    private void sectionsChanged() {
        allEntries = new ArrayList<>(SectionCatalog.colorables());
        noSections = allEntries.isEmpty();
        buildSidebar();
    }

    private boolean paneOpen() {
        return !classic && (pane != null || editor != null || infoPage != null);
    }

    private int backArrowX() {
        return contentX + 4;
    }

    private int backArrowY() {
        return panelTop + 4;
    }

    private void renderBackArrow(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = backArrowX();
        int y = backArrowY();
        boolean hovered = GlassArrow.contains(mouseX, mouseY, x, y);
        backArrow.render(g, x, y, false, hovered, delta);
        if (hovered) {
            hoverPreviewTooltip = Component.translatable("gui.back");
        }
    }

    private void closeOpenPane() {
        if (pane != null) {
            pane.onClose();
            return;
        }
        if (editor != null) {
            editor.onClose();
            return;
        }
        closeInfo();
    }

    private void closePane() {
        if (pane == null) {
            return;
        }
        navTo(0);
        pane = null;
        rebuildWidgets();
    }

    private void openInfo(InfoPane.Kind kind) {
        if (infoPage == kind) {
            return;
        }
        navTo(kind == InfoPane.Kind.BUGS ? 5 : 4);
        pane = null;
        if (editor != null) {
            editor.releaseEmbedded();
            editor = null;
        }
        infoPage = kind;
        infoPane.resetScroll();
        rebuildWidgets();
    }

    private void closeInfo() {
        if (infoPage == null) {
            return;
        }
        navTo(0);
        infoPage = null;
        rebuildWidgets();
    }

    private void buildSidebar() {
        if (sidebar == null) {
            sidebar = new GlassSidebar(container, this::onClose)
                    .title(Component.translatable("createaddonorganizer.colors.sidebar.title"))
                    .shine(true)
                    .onUpdate(() -> Util.getPlatform().openUri(UpdateCheck.PAGE_URL))
                    .onFolder(() -> Util.getPlatform().openPath(FMLPaths.CONFIGDIR.get()),
                            Component.translatable("createaddonorganizer.settings.openFolder"));
        }
        List<GlassSidebar.Row> jump = sidebar.jumpRows();
        jump.clear();
        for (SectionCatalog.Entry entry : mainSections()) {
            ResourceLocation target = entry.id();
            jump.add(GlassSidebar.Row.of(entry.name(), () -> jumpToSection(target))
                    .active(() -> infoPage == null && pane == null && target.equals(selectedId)));
        }
        List<GlassSidebar.Row> rows = sidebar.rows();
        rows.clear();
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.addSection"),
                        () -> openPane(new AddSectionScreen(this)))
                .active(() -> pane instanceof AddSectionScreen));
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.presets"),
                        () -> openPane(new PresetsScreen(this)))
                .active(() -> pane instanceof PresetsScreen));
        rows.add(GlassSidebar.Row.gap());
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.resetOrder"),
                this::resetOrder).tone(GlassSidebar.Tone.DANGER));
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.resetAll"),
                this::confirmResetAll).tone(GlassSidebar.Tone.DANGER));
        if (PackDefaults.isActive()) {
            rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.resetPack"),
                            this::confirmResetToPack).tone(GlassSidebar.Tone.DANGER)
                    .tooltip(Component.translatable("createaddonorganizer.colors.resetPack.tooltip")));
        }
        rows.add(GlassSidebar.Row.gap());
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.style"),
                        () -> openPane(new MenuStyleScreen(this)))
                .active(() -> pane instanceof MenuStyleScreen));
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.credits"),
                        () -> openInfo(InfoPane.Kind.CREDITS))
                .active(() -> infoPage == InfoPane.Kind.CREDITS));
        rows.add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.bugReport"),
                        () -> openInfo(InfoPane.Kind.BUGS))
                .active(() -> infoPage == InfoPane.Kind.BUGS));
        if (DevMode.isUnlocked()) {
            rows.add(GlassSidebar.Row.of(
                            Component.translatable("createaddonorganizer.colors.sidebar.assignBanners"),
                            () -> ScreenSwoosh.drill(() -> new BannerAssignmentScreen(this),
                                    Config.SWOOSH_BANNER_EDITOR))
                    .trailing(() -> Component.translatable("createaddonorganizer.colors.sidebar.dev")));
        }
        sidebar.setBounds(sidebarX, panelTop, sidebarW, panelBottom - panelTop);
    }

    private void refreshListEntries(double scroll) {
        if (list == null) {
            return;
        }
        list.setEntries(filterEntries(allEntries, searchQuery));
        list.setScrollAmount(scroll);
    }

    private static boolean keeps(SectionCatalog.Entry entry, String q) {
        return q.isEmpty() || matchesQuery(entry, q);
    }

    private static List<SectionCatalog.Entry> filterEntries(List<SectionCatalog.Entry> source, String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return source;
        }
        List<SectionCatalog.Entry> out = new ArrayList<>();
        int i = 0;
        while (i < source.size()) {
            SectionCatalog.Entry entry = source.get(i);
            if (!entry.parent()) {
                if (keeps(entry, q)) {
                    out.add(entry);
                }
                i++;
                continue;
            }
            int end = i + 1;
            while (end < source.size() && !source.get(end).parent()) {
                end++;
            }
            boolean hubMatch = keeps(entry, q);
            List<SectionCatalog.Entry> kids = new ArrayList<>();
            for (int k = i + 1; k < end; k++) {
                if (hubMatch || keeps(source.get(k), q)) {
                    kids.add(source.get(k));
                }
            }
            if (hubMatch || !kids.isEmpty()) {
                out.add(entry);
                out.addAll(kids);
            }
            i = end;
        }
        return out;
    }

    private static boolean matchesQuery(SectionCatalog.Entry entry, String q) {
        return entry.name().getString().toLowerCase(Locale.ROOT).contains(q);
    }


    private void openTabRename(SectionCatalog.Entry entry) {
        if (!BannerEditor.isRealTab(entry.id())) {
            Notice.show(Component.translatable("createaddonorganizer.colors.tabName.notEditable", entry.name()),
                    Notice.RED);
            return;
        }
        this.minecraft.setScreen(new RenameTabScreen(this, entry.id()));
    }

    private void openEditor(SectionCatalog.Entry entry) {
        if (classic) {
            BannerEditor.open(this, entry.id(), entry.name(), entry.parent());
            return;
        }
        openEmbedded(new ColorPickerScreen(this, entry.id(), entry.name(), entry.parent()));
    }

    private void openHighlightEditor(ResourceLocation tabId, Component title) {
        if (classic) {
            BannerEditor.openHighlight(this, tabId, title);
            return;
        }
        openEmbedded(new ColorPickerScreen(this, tabId, title, true, true));
    }

    private void clearSearch() {
        if (searchBox != null && !searchQuery.isEmpty()) {
            searchBox.setValue("");
        }
    }

    @Override
    public void tick() {
        if (pane != null) {
            pane.tick();
            return;
        }
        super.tick();
    }

    @Override
    public void removed() {
        if (editor != null) {
            editor.releaseEmbedded();
        }
        if (list != null) {
            lastScroll = list.getScrollAmount();
        }
        lastSelectedId = selectedId;
        lastSearch = searchQuery;
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        if (!classic && !priming) {
            GlassSkin.panel(g, contentX, panelTop, contentW, panelBottom - panelTop);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (priming) {
            renderBackground(g, mouseX, mouseY, partialTick);
            g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFFFF);
            long elapsed = System.currentTimeMillis() - primeStart;
            LoadingSpinner.renderCentered(g, 0, listAreaTop, this.width, listAreaBottom - listAreaTop, elapsed);
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.tabs.loading"),
                    this.width / 2, listAreaTop + (listAreaBottom - listAreaTop) / 2 + LoadingSpinner.labelOffset(),
                    MenuSkin.bodyColor(0xFF8A9AA8));
            if (!primeWorkDone) {
                primeWorkDone = ClientRegistries.advancePrime(PRIME_BUDGET_NANOS);
            }
            if (primeWorkDone) {
                priming = false;
                rebuildWidgets();
            }
            return;
        }

        long now = System.nanoTime();
        float delta = frameNanos == 0L ? 0f : Math.min(0.25f, (now - frameNanos) / 1_000_000_000f);
        frameNanos = now;

        hoverPreviewTooltip = null;
        popupEntry = null;
        super.render(g, mouseX, mouseY, partialTick);

        if (pane != null || editor != null) {
            sidebar.layout();
            sidebar.render(g, this.font, mouseX, mouseY);
            slide.begin(g);
            if (pane != null) {
                pane.render(g, mouseX, mouseY, partialTick);
            } else {
                editor.render(g, mouseX, mouseY, partialTick);
            }
            slide.end(g);
            renderArrows(g, mouseX, mouseY, delta);
            renderBackArrow(g, mouseX, mouseY, delta);
            Component paneHover = hoverPreviewTooltip != null ? hoverPreviewTooltip : sidebar.hoverTip();
            if (paneHover != null) {
                g.renderTooltip(this.font, this.font.split(paneHover, 200), mouseX, mouseY);
            }
            return;
        }

        if (classic) {
            g.drawCenteredString(this.font, Component.literal(container.getModInfo().getDisplayName()),
                    this.width / 2, 16, MenuSkin.titleColor(0xFFE4E4E4));
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.hint"),
                    this.width / 2, 38, 0xFFAAAAAA);
        } else if (infoPage != null) {
            sidebar.layout();
            sidebar.render(g, this.font, mouseX, mouseY);
            slide.begin(g);
            infoPane.render(g, this.font, mouseX, mouseY);
            slide.end(g);
            hoverPreviewTooltip = infoPane.hoverTip();
        } else {
            renderContentChrome(g, mouseX, mouseY);
            sidebar.layout();
            sidebar.render(g, this.font, mouseX, mouseY);
        }

        if (infoPage != null) {
            renderArrows(g, mouseX, mouseY, delta);
            renderBackArrow(g, mouseX, mouseY, delta);
            Component paneTip = hoverPreviewTooltip != null ? hoverPreviewTooltip : sidebar.hoverTip();
            if (paneTip != null) {
                g.renderTooltip(this.font, this.font.split(paneTip, 200), mouseX, mouseY);
            }
            return;
        }

        if (noSections) {
            renderEmptyState(g);
        } else if (list.children().isEmpty()) {
            int y = listAreaTop + (listAreaBottom - listAreaTop) / 2 - 4;
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.search.none"),
                    listCenterX, y, 0xFFAAAAAA);
        }

        renderArrows(g, mouseX, mouseY, delta);

        if (popupEntry != null) {
            renderBannerPopup(g, mouseX, mouseY);
            return;
        }
        Component tip = hoverPreviewTooltip;
        if (tip == null && !classic && sidebar != null) {
            tip = sidebar.hoverTip();
        }
        if (tip != null) {
            g.renderTooltip(this.font, this.font.split(tip, 200), mouseX, mouseY);
        }
    }

    private void renderContentChrome(GuiGraphics g, int mouseX, int mouseY) {
        GlassSkin.widgetBox(g, searchX, searchY, searchW, SEARCH_H, searchBox.isFocused());
        GlassSidebar.magnifier(g, searchX + 6, searchY + 6, GlassSkin.bodyTextColor());
        searchBox.setX(searchX + 18);
        searchBox.setY(searchY + (SEARCH_H - 8) / 2);
        searchBox.setWidth(searchW - 24);
        searchBox.render(g, mouseX, mouseY, 0f);

        int textY = searchY + (SEARCH_H - 8) / 2;
        if (!noSections) {
            renderTip(g, mouseX, mouseY, textY);
        } else {
            tipLeft = 0;
            tipRight = 0;
        }
        int helpY = searchY + (SEARCH_H - HELP_SIZE) / 2;
        boolean helpHovered = GlassSidebar.inside(mouseX, mouseY, helpX, helpY, HELP_SIZE, HELP_SIZE);
        GlassSkin.widgetBox(g, helpX, helpY, HELP_SIZE, HELP_SIZE, helpHovered);
        String mark = "?";
        g.drawString(this.font, mark, helpX + (HELP_SIZE - this.font.width(mark)) / 2, textY,
                helpHovered ? GlassSkin.titleTextColor() : GlassSkin.bodyTextColor(), GlassSkin.shadow());
        if (helpHovered) {
            hoverPreviewTooltip = Component.translatable("createaddonorganizer.colors.hint2");
        }
    }

    private void renderTip(GuiGraphics g, int mouseX, int mouseY, int textY) {
        int left = searchX + searchW + 8;
        int right = helpX - 6;
        if (right - left < 40) {
            tipLeft = 0;
            tipRight = 0;
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
            tipLeft = 0;
            tipRight = 0;
            return;
        }
        int centerX = (left + right) / 2;
        tipLeft = centerX - width / 2 - 4;
        tipRight = centerX + width / 2 + 4;

        boolean hovered = overTip(mouseX, mouseY);
        int alpha = Math.round((hovered ? 0xFF : 0xC0) * Mth.clamp(fade, 0f, 1f));
        if (alpha < 8) {
            return;
        }
        int color = MenuSkin.mixColor(TIP_BASE, GlassSkin.accent(), hovered ? 0.85f : 0.55f);
        g.drawCenteredString(this.font, tip, centerX, textY, (alpha << 24) | (color & 0x00FFFFFF));
    }

    private boolean overTip(double mouseX, double mouseY) {
        return tipRight > tipLeft && mouseX >= tipLeft && mouseX < tipRight
                && mouseY >= searchY && mouseY < searchY + SEARCH_H;
    }

    private void nextTip() {
        tipIndex = (tipIndex + 1) % TIP_KEYS.length;
        tipStart = System.currentTimeMillis();
    }

    private void renderArrows(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int y = GlassArrow.top(this.height);
        int leftX = GlassArrow.leftX();
        int rightX = GlassArrow.rightX(this.width);

        boolean leftHovered = GlassArrow.contains(mouseX, mouseY, leftX, y);
        boolean rightHovered = GlassArrow.contains(mouseX, mouseY, rightX, y);

        leftArrow.render(g, leftX, y, false, leftHovered, delta);
        rightArrow.render(g, rightX, y, true, rightHovered, delta);

        if (leftHovered) {
            hoverPreviewTooltip = Component.translatable("createaddonorganizer.colors.allSettings");
        } else if (rightHovered) {
            hoverPreviewTooltip = Component.translatable("createaddonorganizer.tabs.title");
        }
    }

    private List<ResourceLocation> currentRainbowOrder() {
        List<SectionCatalog.Entry> source = orderDirty && pendingOrder != null ? pendingOrder : allEntries;
        List<ResourceLocation> ids = new ArrayList<>(source.size());
        for (SectionCatalog.Entry entry : source) {
            if (!entry.readOnly()) {
                ids.add(entry.id());
            }
        }
        return ids;
    }

    private ColorSpec previewBannerColor(ResourceLocation id) {
        if (!Config.rainbowMode()) {
            return Config.bannerColorFor(id);
        }
        List<ResourceLocation> order = currentRainbowOrder();
        return ColorSpec.solid(Config.rainbowBannerColor(order.indexOf(id), order.size()));
    }

    private ColorSpec previewTextColor(ResourceLocation id) {
        if (!Config.rainbowMode()) {
            return Config.textColorFor(id);
        }
        List<ResourceLocation> order = currentRainbowOrder();
        return ColorSpec.solid(Config.rainbowTextColor(order.indexOf(id), order.size()));
    }

    private ColorSpec previewTextSecondaryColor(ResourceLocation id) {
        ColorSpec normal = Config.textSecondaryColorFor(id);
        if (normal == null || !Config.rainbowMode()) {
            return normal;
        }
        List<ResourceLocation> order = currentRainbowOrder();
        return ColorSpec.solid(Config.rainbowTextSecondaryColor(order.indexOf(id), order.size()));
    }

    private static ColorSpec opaqueSpec(ColorSpec spec) {
        if (!spec.isGradient()) {
            return ColorSpec.solid(0xFF000000 | (spec.color1() & 0x00FFFFFF));
        }
        return new ColorSpec(0xFF000000 | (spec.color1() & 0x00FFFFFF), 0xFF000000 | (spec.color2() & 0x00FFFFFF),
                spec.direction(), spec.style());
    }

    private static ColorSpec mulAlphaSpec(ColorSpec spec, float factor) {
        if (!spec.isGradient()) {
            return ColorSpec.solid(mulAlpha(spec.color1(), factor));
        }
        return new ColorSpec(mulAlpha(spec.color1(), factor), mulAlpha(spec.color2(), factor), spec.direction(), spec.style());
    }

    private void renderBannerPopup(GuiGraphics g, int mouseX, int mouseY) {
        SectionCatalog.Entry entry = popupEntry;
        int px = mouseX + 12;
        int py = mouseY + 12;
        if (px + POPUP_W > this.width - 2) {
            px = Math.max(2, mouseX - 12 - POPUP_W);
        }
        if (py + POPUP_H > this.height - 2) {
            py = Math.max(2, this.height - 2 - POPUP_H);
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        GlassSkin.popupPanel(g, px, py, POPUP_W, POPUP_H);

        int x = px + POPUP_PAD;
        int y = py + POPUP_PAD;
        int w = BannerTextures.WIDTH;
        int h = BannerTextures.HEIGHT;

        String bannerRef = Config.bannerRefFor(entry.id());
        final ResourceLocation tex = bannerRef != null ? BannerTextures.resolve(bannerRef) : null;
        if (tex != null) {
            Optional<BannerAnimation.AnimInfo> anim = BannerAnimation.get(tex);
            int frameCount = anim.map(BannerAnimation.AnimInfo::frameCount).orElse(1);
            int frame = anim.map(info -> BannerAnimation.currentFrame(tex, info, true)).orElse(0);
            g.blit(tex, x, y, w, h, 0.0F, frame * (float) BannerTextures.HEIGHT, BannerTextures.WIDTH,
                    BannerTextures.HEIGHT, BannerTextures.WIDTH, frameCount * BannerTextures.HEIGHT);
        } else {
            ColorSpec bannerSpec = opaqueSpec(previewBannerColor(entry.id()));
            BannerFill.draw(g, x, y, x + w, y + h, bannerSpec);
            g.fill(x, y, x + w, y + 1, ColorUtil.brighten(bannerSpec.color1(), 0.4f));
        }
        g.fill(x, y + h, x + w, y + h + 1, 0x80000000);

        String full = entry.name().getString();
        String clipped = font.width(full) <= w - 8 ? full : font.plainSubstrByWidth(full, w - 8);
        int textY = y + (h - 8) / 2 + 1;
        ColorSpec secondary = previewTextSecondaryColor(entry.id());
        boolean shadowOn = Config.titleTextShadow(entry.id());
        Integer shadowOverride = shadowOn ? Config.textShadowColorFor(entry.id()) : null;
        boolean vanillaShadow = shadowOn && shadowOverride == null;
        ColorSpec outline = Config.textOutlineColorFor(entry.id());
        TwoToneText.draw(g, font, Component.literal(clipped), x + 4, textY, x + w - 4,
                previewTextColor(entry.id()), secondary, Config.twoToneSplitFor(entry.id()), vanillaShadow,
                shadowOverride != null ? shadowOverride : 0, outline);

        Component context;
        if (entry.parent()) {
            context = Component.translatable("createaddonorganizer.colors.panel.hub");
        } else if (entry.tabOwned()) {
            context = Component.translatable("createaddonorganizer.colors.panel.ofTab",
                    nameOfTab(TabLayout.ownerOfSectionId(entry.id())));
        } else {
            context = Component.translatable("createaddonorganizer.colors.panel.in",
                    nameOfTab(Config.parentFor(entry.id())));
        }
        int lineY = y + h + 5;
        g.drawString(this.font, font.plainSubstrByWidth(context.getString(), w), x, lineY,
                GlassSkin.bodyTextColor(), GlassSkin.shadow());
        String detail = tex != null
                ? Component.translatable("createaddonorganizer.banner.mode.image").getString()
                : Config.formatColorSpec(opaqueSpec(previewBannerColor(entry.id())), true);
        g.drawString(this.font, font.plainSubstrByWidth(detail, w), x, lineY + 10,
                GlassSkin.bodyTextColor(), GlassSkin.shadow());
        g.pose().popPose();
    }

    private Component nameOfTab(ResourceLocation id) {
        if (id == null) {
            return Component.literal("?");
        }
        for (SectionCatalog.Entry entry : allEntries) {
            if (entry.id().equals(id)) {
                return entry.name();
            }
        }
        TabLayout layout = TabLayoutStore.byId(id);
        if (layout != null && layout.nameOverride() != null) {
            return Component.literal(layout.nameOverride());
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        return tab != null ? tab.getDisplayName() : Component.literal(id.toString());
    }

    private void renderEmptyState(GuiGraphics g) {
        int y = listAreaTop + (listAreaBottom - listAreaTop) / 2 - 4;
        if (konamiTriggeredMillis != 0) {
            if (System.currentTimeMillis() - konamiTriggeredMillis >= KONAMI_CLOSE_DELAY_MS) {
                ScreenSwoosh.pull(() -> parent, Config.SWOOSH_BACK);
                return;
            }
            g.drawCenteredString(this.font, Component.literal("told you not to"), this.width / 2, y, 0xFFFF5555);
            return;
        }
        if (UNDERTALE_LINE.equals(emptyStateText)) {
            Component line = Component.literal(emptyStateText)
                    .setStyle(Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, "undertale")));
            float scale = 1.5f;
            int width = this.font.width(line);
            g.pose().pushPose();
            g.pose().scale(scale, scale, scale);
            g.drawString(this.font, line, Math.round(this.width / 2 / scale - width / 2f), Math.round(y / scale),
                    0xFFAAAAAA, false);
            g.pose().popPose();
            return;
        }
        g.drawCenteredString(this.font, Component.literal(emptyStateText), this.width / 2, y, 0xFFAAAAAA);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (priming) {
            return keyCode == GLFW.GLFW_KEY_ESCAPE && super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (pane != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closePane();
                return true;
            }
            return pane.keyPressed(keyCode, scanCode, modifiers);
        }
        if (editor != null) {
            return editor.keyPressed(keyCode, scanCode, modifiers);
        }
        if (infoPage != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeInfo();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (renamingId != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                confirmRename();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                cancelRename();
                return true;
            }
            if (renameBox.keyPressed(keyCode, scanCode, modifiers) || renameBox.canConsumeInput()) {
                return true;
            }
        }
        if (noSections && KONAMI_LINE.equals(emptyStateText) && konamiTriggeredMillis == 0) {
            if (keyCode == KONAMI_SEQUENCE[konamiProgress]) {
                konamiProgress++;
                if (konamiProgress == KONAMI_SEQUENCE.length) {
                    konamiTriggeredMillis = System.currentTimeMillis();
                }
            } else {
                konamiProgress = keyCode == KONAMI_SEQUENCE[0] ? 1 : 0;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown()
                && (searchBox == null || !searchBox.isFocused())) {
            if (lastUndo == null) {
                Sfx.denied();
                return true;
            }
            Runnable undo = lastUndo;
            lastUndo = null;
            undo.run();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (priming) {
            return false;
        }
        if (pane != null) {
            return pane.charTyped(codePoint, modifiers);
        }
        if (editor != null) {
            return editor.charTyped(codePoint, modifiers);
        }
        if (renameBox != null && renameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!classic && sidebar != null && sidebar.mouseDragged(mouseY)) {
            return true;
        }
        if (pane != null) {
            return pane.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (editor != null) {
            return editor.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (infoPage != null) {
            return infoPane.mouseDragged(mouseY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (sidebar != null) {
            sidebar.mouseReleased();
        }
        infoPane.mouseReleased();
        if (pane != null) {
            return pane.mouseReleased(mouseX, mouseY, button);
        }
        if (editor != null) {
            return editor.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (pane != null) {
            return (sidebar != null && sidebar.mouseScrolled(mouseX, mouseY, scrollY))
                    || pane.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (editor != null) {
            return editor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                    || (sidebar != null && sidebar.mouseScrolled(mouseX, mouseY, scrollY));
        }
        if (!classic && !priming && sidebar != null && sidebar.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (infoPage != null) {
            return infoPane.mouseScrolled(mouseX, mouseY, scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (priming) {
            return false;
        }
        if (button == 0 && paneOpen()
                && GlassArrow.contains(mouseX, mouseY, backArrowX(), backArrowY())) {
            Sfx.uiClick();
            closeOpenPane();
            return true;
        }
        if (button == 0 && !classic && sideArrowClicked(mouseX, mouseY)) {
            return true;
        }
        if (pane != null) {
            if (button == 0 && sidebar != null && sidebar.mouseClicked(mouseX, mouseY)) {
                return true;
            }
            return pane.mouseClicked(mouseX, mouseY, button);
        }
        if (editor != null) {
            if (editor.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return button == 0 && sidebar != null && sidebar.mouseClicked(mouseX, mouseY);
        }
        if (renamingId != null) {
            if (renameConfirm.mouseClicked(mouseX, mouseY, button)
                    || renameCancel.mouseClicked(mouseX, mouseY, button)
                    || renameBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            cancelRename();
        }
        if (button == 0 && classic && sideArrowClicked(mouseX, mouseY)) {
            return true;
        }
        if (!classic && button == 0 && sidebar != null && sidebar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (!classic && button == 0 && infoPage == null && pane == null && editor == null
                && overTip(mouseX, mouseY)) {
            nextTip();
            return true;
        }
        if (infoPage != null) {
            return button == 0 && infoPane.mouseClicked(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean sideArrowClicked(double mouseX, double mouseY) {
        int arrowY = GlassArrow.top(this.height);
        if (GlassArrow.contains(mouseX, mouseY, GlassArrow.leftX(), arrowY)) {
            Sfx.uiClick();
            ScreenSwoosh.pull(() -> new AllSettingsScreen(this, container), Config.SWOOSH_ARROW_LEFT);
            return true;
        }
        if (GlassArrow.contains(mouseX, mouseY, GlassArrow.rightX(this.width), arrowY)) {
            Sfx.uiClick();
            ScreenSwoosh.push(() -> new TabStudioScreen(this), Config.SWOOSH_ARROW_RIGHT);
            return true;
        }
        return false;
    }

    private void startRename(SectionCatalog.Entry entry) {
        renamingId = entry.id();
        renameBox = new EditBox(this.font, 0, 0, 100, 20, Component.empty());
        renameBox.setMaxLength(64);
        String stored = TabLayout.ownerOfSectionId(entry.id()) == null
                ? Config.sectionNameOverride(entry.id()) : null;
        renameBox.setValue(stored != null ? stored : entry.name().getString());
        renameBox.setHighlightPos(0);
        renameBox.setFocused(true);
        renameBox.setTooltip(Tooltip.create(Component.translatable("createaddonorganizer.rename.hint")));
        renameConfirm = new RenameIconButton(true, Component.translatable("createaddonorganizer.colors.ok"),
                b -> confirmRename());
        renameCancel = new RenameIconButton(false, Component.translatable("createaddonorganizer.colors.cancel"),
                b -> cancelRename());
    }

    private void confirmRename() {
        if (renamingId == null) {
            return;
        }
        ResourceLocation id = renamingId;
        String typed = renameBox.getValue();
        String name = typed.trim();
        boolean blankOnPurpose = name.isEmpty() && !typed.isEmpty();
        if (blankOnPurpose) {
            name = "";
        }
        Component title;
        if (TabLayout.ownerOfSectionId(id) != null) {
            if (name.isEmpty() && !blankOnPurpose) {
                cancelRename();
                return;
            }
            renameInOwningLayout(id, name);
            title = Component.literal(name);
        } else if (name.isEmpty() && !blankOnPurpose) {
            Config.clearSectionName(id);
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
            Component nativeTitle = LiveColors.nativeTitle(id);
            title = tab != null ? tab.getDisplayName()
                    : nativeTitle != null ? nativeTitle : Component.literal(id.toString());
        } else {
            Config.setSectionName(id, name);
            title = Component.literal(name);
        }
        LiveColors.applyTitle(id, title);
        list.updateName(id, title);
        UnaryOperator<SectionCatalog.Entry> retitle = e -> e.id().equals(id)
                ? new SectionCatalog.Entry(id, title, e.parent(), e.readOnly(), e.nativeTextColor(),
                        e.nativeSecondaryTextColor(), e.tabOwned())
                : e;
        allEntries.replaceAll(retitle);
        if (orderDirty && pendingOrder != null) {
            pendingOrder.replaceAll(retitle);
        }
        if (id.equals(selectedId) && selectedEntry != null) {
            selectedEntry = retitle.apply(selectedEntry);
        }
        cancelRename();
    }

    private void renameInOwningLayout(ResourceLocation id, String name) {
        ResourceLocation owner = TabLayout.ownerOfSectionId(id);
        if (owner == null) {
            return;
        }
        TabLayout layout = TabLayoutStore.byId(owner);
        if (layout == null) {
            return;
        }
        TabLayout updated = layout.withSectionTitle(id, name);
        if (updated != layout) {
            TabLayoutStore.put(updated);
        }
    }

    private void cancelRename() {
        renamingId = null;
        renameBox = null;
        renameConfirm = null;
        renameCancel = null;
    }

    private static int mulAlpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * factor);
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static boolean isOrderable(SectionCatalog.Entry entry) {
        return !entry.readOnly() && !entry.tabOwned();
    }

    private static boolean boundToGroup(SectionCatalog.Entry entry) {
        return entry.parent() || SectionCatalog.isModDrawn(entry.id()) || LiveColors.isAdoptedNative(entry.id());
    }

    private static ResourceLocation hubOf(ResourceLocation id) {
        ResourceLocation live = LiveColors.findParent(id);
        return live != null ? live : Config.parentFor(id);
    }

    private void markOrderDirty() {
        List<SectionCatalog.Entry> previous = orderDirty && pendingOrder != null
                ? new ArrayList<>(pendingOrder)
                : new ArrayList<>(allEntries);
        pendingOrder = list.currentEntries();
        orderDirty = true;
        saveOrder();
        lastUndo = () -> {
            pendingOrder = previous;
            orderDirty = true;
            saveOrder();
            rebuildWidgets();
        };
    }

    private void resetOrder() {
        clearSearch();
        list.resetToAlphabetical();
    }

    private void saveOrder() {
        if (pendingOrder == null) {
            return;
        }
        List<ResourceLocation> ids = new ArrayList<>();
        Map<ResourceLocation, List<ResourceLocation>> byParent = new LinkedHashMap<>();
        for (SectionCatalog.Entry entry : pendingOrder) {
            if (isOrderable(entry)) {
                ids.add(entry.id());
                byParent.computeIfAbsent(hubOf(entry.id()), k -> new ArrayList<>()).add(entry.id());
            }
        }
        Config.setSectionOrder(ids);
        for (Map.Entry<ResourceLocation, List<ResourceLocation>> e : byParent.entrySet()) {
            LiveColors.applyOrder(e.getKey(), e.getValue());
            reorderStoredLayout(e.getKey(), e.getValue());
        }
        TabLayoutStore.flush();
        refreshLiveTabLayout();
        orderDirty = false;
        pendingOrder = null;
        lastUndo = null;
        Notice.showQuiet(Component.translatable("createaddonorganizer.colors.saved"), Notice.GREEN);
    }

    private void reorderStoredLayout(ResourceLocation parent, List<ResourceLocation> order) {
        TabLayout layout = TabLayoutStore.byId(parent);
        if (layout == null || layout.isCustom() || layout.sectionCount() == 0) {
            return;
        }
        TabLayout reordered = layout.withSectionsOrdered(order);
        if (reordered != layout) {
            TabLayoutStore.putQuiet(reordered);
        }
    }

    private void refreshLiveTabLayout() {
        createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams());
    }

    private void confirmResetAll() {
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                resetAllSettings();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.colors.resetAll.title"),
                Component.translatable("createaddonorganizer.colors.resetAll.message")));
    }

    private void confirmResetToPack() {
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                resetToPackDefaults();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.colors.resetPack.title"),
                Component.translatable("createaddonorganizer.colors.resetPack.message")));
    }

    private void resetToPackDefaults() {
        boolean changed = Config.resetToPackDefaults();
        createaddonorganizer.organize(ClientRegistries.displayParams());
        for (SectionCatalog.Entry entry : SectionCatalog.colorables()) {
            if (entry.readOnly() || !PackDefaults.hasAnyFor(entry.id())) {
                continue;
            }
            LiveColors.apply(entry.id(), Config.bannerColorFor(entry.id()));
            LiveColors.applyTexture(entry.id(), BannerTextures.resolve(Config.bannerRefFor(entry.id())));
            LiveColors.applyTextColor(entry.id(), Config.textColorFor(entry.id()));
            LiveColors.applyTitle(entry.id(), entry.name());
        }
        refreshLiveTabLayout();
        orderDirty = false;
        pendingOrder = null;
        rebuildWidgets();
        Notice.show(Component.translatable(changed
                ? "createaddonorganizer.colors.resetPack.done"
                : "createaddonorganizer.colors.resetPack.nothing"), Notice.GREEN);
    }

    private void resetAllSettings() {
        Config.resetAllToDefault();
        TabLayoutStore.resetAll();
        AbsorbedTabs.IDS.clear();
        createaddonorganizer.organize(ClientRegistries.displayParams());
        List<SectionCatalog.Entry> entries = SectionCatalog.colorables();
        for (SectionCatalog.Entry entry : entries) {
            if (entry.readOnly()) {
                continue;
            }
            if (!Config.isNativeSeeded(entry.id())) {
                LiveColors.apply(entry.id(), ColorSpec.solid(Config.DEFAULT_BANNER_COLOR.get()));
                LiveColors.applyTextColor(entry.id(), ColorSpec.solid(Config.DEFAULT_TEXT_COLOR.get()));
            } else {
                LiveColors.applyTexture(entry.id(), BannerTextures.resolve(Config.bannerRefFor(entry.id())));
                LiveColors.applyTextColor(entry.id(), Config.textColorFor(entry.id()));
            }
            LiveColors.applyTitle(entry.id(), entry.name());
        }

        ResourceLocation currentParent = null;
        List<SectionCatalog.Entry> group = new ArrayList<>();
        for (SectionCatalog.Entry entry : entries) {
            if (entry.parent()) {
                applyAlphabeticalGroup(currentParent, group);
                currentParent = entry.id();
                group = new ArrayList<>();
            } else if (isOrderable(entry)) {
                group.add(entry);
            }
        }
        applyAlphabeticalGroup(currentParent, group);
        refreshLiveTabLayout();

        orderDirty = false;
        pendingOrder = null;
        rebuildWidgets();
        Notice.show(Component.translatable("createaddonorganizer.colors.resetAll.done"), Notice.GREEN);
    }

    private static void applyAlphabeticalGroup(ResourceLocation parent, List<SectionCatalog.Entry> group) {
        if (parent == null || group.isEmpty()) {
            return;
        }
        group.sort(Comparator.comparing(e -> e.name().getString(), String.CASE_INSENSITIVE_ORDER));
        List<ResourceLocation> ids = new ArrayList<>(group.size());
        for (SectionCatalog.Entry e : group) {
            ids.add(e.id());
        }
        LiveColors.applyOrder(parent, ids);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private class ColorList extends ContainerObjectSelectionList<ColorList.Row> {
        private static final double DRAG_SCROLL_PX = 5d;

        private final int rowHeight;
        private Row dragRow;
        private int dragFromIndex;
        private int dragTargetIndex;
        private ResourceLocation dragTargetParent;
        private int dragGrabOffsetY;
        private boolean dragActive;
        private boolean renderingGhost;
        private double dragStartMouseY;

        private final ListGlide glide = new ListGlide();

        private static final long SLIDE_MS = 130;
        private static final int GHOST_BG = 0xB0101016;
        private static final int GHOST_BORDER = 0x60FFFFFF;
        private static final int SUB_INDENT = 14;
        private static final int SUB_LINE_X = 6;
        private static final int SUB_LINE_FADE = 10;
        private static final int PREVIEW_W = 48;
        private static final int SLIVER_LINE_OVERLAP = 3;
        private static final int PACK_TAG_COLOR = 0xFFB79CF0;

        ColorList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
            this.rowHeight = itemHeight;
        }

        void jumpToSection(ResourceLocation id) {
            List<Row> all = children();
            for (int i = 0; i < all.size(); i++) {
                Row row = all.get(i);
                if (!row.sliver && row.data.id().equals(id)) {
                    glide.beginScroll(this);
                    setScrollAmount(i * (double) rowHeight);
                    glide.endScroll(this);
                    return;
                }
            }
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            glide.beforeRender(this);
            if (dragRow != null && dragActive) {
                dragAutoScroll(mouseY);
                retarget(mouseY);
            }
            SectionColorsScreen.this.slide.begin(g);
            super.renderWidget(g, mouseX, mouseY, partialTick);
            SectionColorsScreen.this.slide.end(g);

            if (dragRow != null && dragActive) {
                int left = getRowLeft();
                int w = getRowWidth();
                int gTop = ghostTop(mouseY);
                int entryH = rowHeight - 4;
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(left - 3, gTop - 3, left + w + 3, gTop + entryH + 3, GHOST_BG);
                g.renderOutline(left - 3, gTop - 3, w + 6, entryH + 6, GHOST_BORDER);
                renderingGhost = true;
                dragRow.render(g, dragFromIndex, gTop, left, w, entryH, -1, -1, false, partialTick);
                renderingGhost = false;
                g.pose().popPose();
            }
        }

        void setEntries(List<SectionCatalog.Entry> entries) {
            dragRow = null;
            dragActive = false;
            List<Row> rows = new ArrayList<>(entries.size());
            for (SectionCatalog.Entry entry : entries) {
                if (entry.parent()) {
                    rows.add(new Row(entry, true));
                }
                rows.add(new Row(entry));
            }
            replaceEntries(rows);
        }

        @Override
        public int getRowWidth() {
            return SectionColorsScreen.this.listRowWidth;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private ResourceLocation parentIdAt(int index) {
            for (int i = Math.min(index, children().size() - 1); i >= 0; i--) {
                Row row = children().get(i);
                if (!row.sliver && row.data.parent()) {
                    return row.data.id();
                }
            }
            return null;
        }

        private boolean startsGroup(int index) {
            List<Row> all = children();
            if (index >= all.size()) {
                return true;
            }
            Row row = all.get(index);
            return row.sliver || row.data.parent();
        }

        private boolean isLastInGroup(int index) {
            return index + 1 >= children().size() || startsGroup(index + 1);
        }

        private int minNonNativeInsertIndex(ResourceLocation parent) {
            List<Row> all = children();
            int start = 0;
            for (int i = 0; i < all.size(); i++) {
                if (!all.get(i).sliver && all.get(i).data.parent() && all.get(i).data.id().equals(parent)) {
                    start = i + 1;
                    break;
                }
            }
            int i = start;
            while (i < all.size() && !startsGroup(i)
                    && (all.get(i).data.readOnly() || all.get(i).data.tabOwned()
                            || SectionCatalog.isModDrawn(all.get(i).data.id()))) {
                i++;
            }
            return i;
        }

        private void retarget(double mouseY) {
            List<Row> all = children();
            int slot = Math.round((ghostTop(mouseY) - getRowTop(0)) / (float) rowHeight);
            slot = Mth.clamp(slot, 0, all.size() - 1);
            int insertPos;
            if (startsGroup(slot) || slot > dragFromIndex) {
                insertPos = slot + 1;
            } else {
                insertPos = slot;
            }
            ResourceLocation landingParent = parentIdAt(insertPos - 1);
            if (dragRow != null && boundToGroup(dragRow.data)
                    && !Objects.equals(landingParent, parentIdAt(dragFromIndex))) {
                return;
            }
            dragTargetParent = landingParent;
            if (dragRow == null || !boundToGroup(dragRow.data)) {
                insertPos = Math.max(insertPos, minNonNativeInsertIndex(dragTargetParent));
            }
            dragTargetIndex = insertPos;
            updateSlideTargets();
        }

        private void updateSlideTargets() {
            List<Row> all = children();
            for (int i = 0; i < all.size(); i++) {
                int target = 0;
                if (dragTargetIndex > dragFromIndex && i > dragFromIndex && i < dragTargetIndex) {
                    target = -rowHeight;
                } else if (dragTargetIndex <= dragFromIndex && i >= dragTargetIndex && i < dragFromIndex) {
                    target = rowHeight;
                }
                all.get(i).slideTo(target);
            }
        }

        private int ghostTop(double mouseY) {
            return Mth.clamp((int) mouseY - dragGrabOffsetY, getY(), getY() + getHeight() - rowHeight);
        }

        private void dragAutoScroll(int mouseY) {
            int max = getMaxScroll();
            if (max <= 0) {
                return;
            }
            int band = Math.max(rowHeight, 12);
            int top = getY();
            int bottom = getY() + getHeight();
            double push;
            if (mouseY < top + band) {
                push = (mouseY - (top + band)) / (double) band;
            } else if (mouseY > bottom - band) {
                push = (mouseY - (bottom - band)) / (double) band;
            } else {
                return;
            }
            double from = glide.target();
            double to = Mth.clamp(from + Mth.clamp(push, -1d, 1d) * DRAG_SCROLL_PX, 0d, max);
            if (to == from) {
                return;
            }
            glide.beginScroll(this);
            setScrollAmount(to);
            glide.endScroll(this);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            glide.beginScroll(this);
            boolean handled = super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
            glide.endScroll(this);
            if (dragRow != null) {
                retarget(mouseY);
            }
            return handled;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            glide.beginScroll(this);
            boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            glide.endScroll(this);
            return handled;
        }

        void updateName(ResourceLocation id, Component title) {
            for (Row row : children()) {
                if (row.data.id().equals(id)) {
                    row.data = new SectionCatalog.Entry(id, title, row.data.parent(), row.data.readOnly(),
                            row.data.nativeTextColor(), row.data.nativeSecondaryTextColor(), row.data.tabOwned());
                }
            }
        }

        List<SectionCatalog.Entry> currentEntries() {
            List<SectionCatalog.Entry> out = new ArrayList<>(children().size());
            for (Row row : children()) {
                if (!row.sliver) {
                    out.add(row.data);
                }
            }
            return out;
        }

        void resetToAlphabetical() {
            List<Row> all = children();
            int i = 0;
            while (i < all.size()) {
                int start = i + 1;
                int end = start;
                while (end < all.size() && !startsGroup(end)) {
                    end++;
                }
                List<Integer> sortableIndices = new ArrayList<>();
                for (int k = start; k < end; k++) {
                    if (isOrderable(all.get(k).data)) {
                        sortableIndices.add(k);
                    }
                }
                List<Row> sortable = new ArrayList<>();
                for (int idx : sortableIndices) {
                    sortable.add(all.get(idx));
                }
                sortable.sort(Comparator.comparing(r -> r.data.name().getString(), String.CASE_INSENSITIVE_ORDER));
                for (int k = 0; k < sortableIndices.size(); k++) {
                    all.set(sortableIndices.get(k), sortable.get(k));
                }
                i = end;
            }
            SectionColorsScreen.this.markOrderDirty();
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private SectionCatalog.Entry data;
            private final boolean sliver;
            private final Button edit;
            private final Button tab;
            private float slideFrom;
            private int slideTarget;
            private long slideStart;
            private List<AbstractWidget> widgets;

            private float slideOffset() {
                float t = Mth.clamp((System.currentTimeMillis() - slideStart) / (float) SLIDE_MS, 0f, 1f);
                float inv = 1f - t;
                return Mth.lerp(1f - inv * inv * inv, slideFrom, slideTarget);
            }

            private void slideTo(int target) {
                if (target == slideTarget) {
                    return;
                }
                slideFrom = slideOffset();
                slideTarget = target;
                slideStart = System.currentTimeMillis();
            }

            private void settleFrom(float from) {
                slideFrom = from;
                slideTarget = 0;
                slideStart = System.currentTimeMillis();
            }

            Row(SectionCatalog.Entry entry) {
                this(entry, false);
            }

            Row(SectionCatalog.Entry entry, boolean sliver) {
                this.data = entry;
                this.sliver = sliver;
                if (sliver) {
                    this.edit = MenuSkin.markEdit(
                            Button.builder(Component.translatable("createaddonorganizer.colors.edit"),
                                            b -> SectionColorsScreen.this.openHighlightEditor(data.id(),
                                                    tabTitle()))
                                    .size(44, 20).build());
                } else {
                    this.edit = entry.readOnly() ? null
                            : MenuSkin.markEdit(Button.builder(Component.translatable("createaddonorganizer.colors.edit"),
                                            b -> SectionColorsScreen.this.openEditor(data))
                                    .size(44, 20).build());
                }
                this.tab = sliver && !entry.readOnly() && !SectionColorsScreen.this.classic
                        && BannerEditor.isEditableInTabCreator(entry.id())
                                ? Button.builder(Component.translatable("createaddonorganizer.colors.panel.tabCreator"),
                                                b -> BannerEditor.openInTabCreator(SectionColorsScreen.this, data.id()))
                                        .size(34, 20).build()
                                : null;
            }

            private Component tabTitle() {
                Component override = TabLayoutStore.nameOverride(data.id());
                return override != null ? override : data.name();
            }

            private void renderSliver(GuiGraphics g, int left, int top, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, float alpha) {
                Integer highlight = Config.highlightColorFor(data.id());
                int accent = highlight != null ? (0xFF000000 | (highlight & 0x00FFFFFF)) : GlassSkin.accent();

                int widgetY = top + (rowHeight - 20) / 2;
                int actionX = left + rowWidth;
                if (edit != null) {
                    edit.setX(left + rowWidth - edit.getWidth());
                    edit.setY(widgetY);
                    actionX = edit.getX();
                }
                if (tab != null) {
                    tab.setX(actionX - 2 - tab.getWidth());
                    tab.setY(widgetY);
                    actionX = tab.getX();
                }

                if (SectionColorsScreen.this.classic) {
                    int band = highlight != null ? (0x2A << 24) | (highlight & 0x00FFFFFF)
                            : MenuSkin.accent(0x2AFFFFFF);
                    int divider = highlight != null ? (0x60 << 24) | (highlight & 0x00FFFFFF)
                            : MenuSkin.accent(0x60FFFFFF);
                    g.fill(left, top, left + rowWidth, top + rowHeight, mulAlpha(band, alpha));
                    g.fill(left, top, left + rowWidth, top + 1, mulAlpha(divider, alpha));
                    int bandTextY = top + (rowHeight - 8) / 2;
                    int bandNameX = left + PREVIEW_W + 8;
                    g.drawString(font, boldFitting(tabTitle().getString(), actionX - 10 - bandNameX), bandNameX,
                            bandTextY, mulAlpha(0xFFFFFFFF, alpha), false);
                } else {
                    Component label = boldFitting(tabTitle().getString(), Math.max(0, actionX - 10 - left));
                    TwoToneText.draw(g, font, label, left, top + rowHeight - 14, mulAlpha(accent, alpha),
                            mulAlpha(MenuSkin.mixColor(accent, 0xFF000000, HEADER_TONE), alpha),
                            5f / 9f, GlassSkin.shadow());
                }

                if (!ColorList.this.renderingGhost) {
                    int lineColor = highlight != null ? (0x90 << 24) | (highlight & 0x00FFFFFF)
                            : MenuSkin.accent(0x90FFFFFF);
                    int lineX = left + SUB_LINE_X;
                    int lineTop = top + rowHeight - SLIVER_LINE_OVERLAP;
                    int gapToNext = ColorList.this.rowHeight - rowHeight;
                    g.fill(lineX, lineTop, lineX + 2, top + rowHeight + gapToNext, mulAlpha(lineColor, alpha));
                }

                if (tab != null && !ColorList.this.renderingGhost) {
                    tab.setAlpha(Math.max(alpha, 0.04f));
                    tab.render(g, mouseX, mouseY, 0f);
                }
                if (edit != null && !ColorList.this.renderingGhost) {
                    edit.setAlpha(Math.max(alpha, 0.04f));
                    edit.render(g, mouseX, mouseY, 0f);
                }
            }

            private void deleteViaShift() {
                ResourceLocation id = data.id();
                if (LiveColors.isAdoptedNative(id)) {
                    releaseNativeSection();
                    return;
                }
                boolean wasForceIncluded = Config.isForceIncluded(id);
                ResourceLocation priorRoute = Config.parentFor(id);
                TabManager.deleteSectionConfig(id);
                ColorList.this.children().remove(this);
                SectionColorsScreen.this.allEntries.removeIf(e -> e.id().equals(id));
                if (SectionColorsScreen.this.orderDirty && SectionColorsScreen.this.pendingOrder != null) {
                    SectionColorsScreen.this.pendingOrder.removeIf(e -> e.id().equals(id));
                }
                if (id.equals(SectionColorsScreen.this.selectedId)) {
                    SectionColorsScreen.this.selectedId = null;
                    SectionColorsScreen.this.selectedEntry = null;
                    lastSelectedId = null;
                }
                SectionColorsScreen.this.lastUndo = () -> {
                    TabManager.restoreSectionConfig(id, wasForceIncluded, priorRoute);
                    SectionColorsScreen.this.rebuildWidgets();
                };
            }

            private void releaseNativeSection() {
                ResourceLocation id = data.id();
                LiveColors.releaseNative(id);
                SectionColorsScreen.this.lastUndo = () -> {
                    LiveColors.readoptNative(id);
                    SectionColorsScreen.this.rebuildWidgets();
                };
                if (id.equals(SectionColorsScreen.this.selectedId)) {
                    SectionColorsScreen.this.selectedId = null;
                    SectionColorsScreen.this.selectedEntry = null;
                    lastSelectedId = null;
                }
                SectionColorsScreen.this.orderDirty = false;
                SectionColorsScreen.this.pendingOrder = null;
                SectionColorsScreen.this.rebuildWidgets();
                Notice.show(Component.translatable("createaddonorganizer.colors.native.released", data.name()),
                        Notice.GREEN);
            }

            private boolean isDeletableHub() {
                return data.parent();
            }

            private void confirmDeleteMainSection() {
                Component message = Config.isBuiltinHub(data.id())
                        ? Component.translatable("createaddonorganizer.colors.deleteMain.builtinMessage", data.name())
                        : Component.translatable("createaddonorganizer.colors.deleteMain.message", data.name());
                minecraft.setScreen(new ConfirmScreen(confirmed -> {
                    if (confirmed) {
                        deleteMainSection();
                    }
                    minecraft.setScreen(SectionColorsScreen.this);
                }, Component.translatable("createaddonorganizer.colors.deleteMain.title"), message));
            }

            private void deleteMainSection() {
                ResourceLocation id = data.id();
                boolean wasForceExcludedBefore = Config.isForceExcluded(id);
                List<ResourceLocation> routedHere = Config.subSectionsRoutedTo(id);

                Config.removeExtraMainSection(id);
                if (Config.isBuiltinHub(id)) {
                    Config.addForceExclude(id);
                }
                Config.clearRoutesTo(id);
                createaddonorganizer.reapplyAbsorption(ClientRegistries.displayParams());
                SectionColorsScreen.this.orderDirty = false;
                SectionColorsScreen.this.pendingOrder = null;

                SectionColorsScreen.this.lastUndo = () -> {
                    if (!wasForceExcludedBefore) {
                        Config.removeForceExclude(id);
                    }
                    Config.addExtraMainSection(id);
                    createaddonorganizer.MANAGED_PARENTS.add(id);
                    for (ResourceLocation subId : routedHere) {
                        Config.setRoute(subId, id);
                    }
                    createaddonorganizer.reapplyAbsorption(ClientRegistries.displayParams());
                    SectionColorsScreen.this.rebuildWidgets();
                };

                SectionColorsScreen.this.rebuildWidgets();
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return widgets();
            }

            private List<AbstractWidget> widgets() {
                if (widgets == null) {
                    widgets = RowChildren.of(tab, edit);
                }
                return widgets;
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return widgets();
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (sliver) {
                    if (button != 0) {
                        return false;
                    }
                    if (super.mouseClicked(mouseX, mouseY, button)) {
                        return true;
                    }
                    if (Screen.hasControlDown()) {
                        SectionColorsScreen.this.openTabRename(data);
                        return true;
                    }
                    boolean again = data.id().equals(SectionColorsScreen.this.lastClickId)
                            && System.currentTimeMillis() - SectionColorsScreen.this.lastClickMillis < DOUBLE_CLICK_MS;
                    SectionColorsScreen.this.lastClickId = data.id();
                    SectionColorsScreen.this.lastClickMillis = System.currentTimeMillis();
                    if (again) {
                        SectionColorsScreen.this.openTabRename(data);
                    }
                    return true;
                }
                if (super.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (button == 0 && !SectionColorsScreen.this.classic && !Screen.hasShiftDown() && !Screen.hasControlDown()) {
                    boolean sameRow = data.id().equals(SectionColorsScreen.this.lastClickId)
                            && System.currentTimeMillis() - SectionColorsScreen.this.lastClickMillis < DOUBLE_CLICK_MS;
                    SectionColorsScreen.this.lastClickId = data.id();
                    SectionColorsScreen.this.lastClickMillis = System.currentTimeMillis();
                    SectionColorsScreen.this.selectedId = data.id();
                    SectionColorsScreen.this.selectedEntry = data;
                    lastSelectedId = data.id();
                    if (sameRow && !data.readOnly()) {
                        SectionColorsScreen.this.openEditor(data);
                        return true;
                    }
                }
                if (data.readOnly()) {
                    return false;
                }
                if (button == 0 && Screen.hasShiftDown() && !data.parent() && !data.tabOwned()) {
                    deleteViaShift();
                    return true;
                }
                if (button == 0 && Screen.hasShiftDown() && isDeletableHub()) {
                    confirmDeleteMainSection();
                    return true;
                }
                if (button == 0 && Screen.hasControlDown()) {
                    SectionColorsScreen.this.startRename(data);
                    return true;
                }
                if (button == 0 && isOrderable(data)
                        && SectionColorsScreen.this.searchQuery.trim().isEmpty()) {
                    dragRow = this;
                    dragFromIndex = dragTargetIndex = ColorList.this.children().indexOf(this);
                    ColorList.this.dragTargetParent = ColorList.this.parentIdAt(dragFromIndex);
                    ColorList.this.dragGrabOffsetY = (int) (mouseY - ColorList.this.getRowTop(dragFromIndex));
                    ColorList.this.dragStartMouseY = mouseY;
                    ColorList.this.dragActive = false;
                    ColorList.this.updateSlideTargets();
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                if (dragRow != this) {
                    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                }
                if (!ColorList.this.dragActive && Math.abs(mouseY - ColorList.this.dragStartMouseY) > 4) {
                    ColorList.this.dragActive = true;
                    Sfx.grab();
                }
                ColorList.this.retarget(mouseY);
                return true;
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                if (dragRow != this) {
                    return super.mouseReleased(mouseX, mouseY, button);
                }
                List<Row> all = ColorList.this.children();
                Map<Row, Float> shownTops = new LinkedHashMap<>();
                if (ColorList.this.dragActive) {
                    Sfx.release();
                    for (int i = 0; i < all.size(); i++) {
                        Row r = all.get(i);
                        shownTops.put(r, r == dragRow
                                ? (float) ColorList.this.ghostTop(mouseY)
                                : ColorList.this.getRowTop(i) + r.slideOffset());
                    }
                }
                if (dragTargetIndex != dragFromIndex) {
                    ResourceLocation originParent = ColorList.this.parentIdAt(dragFromIndex);
                    ResourceLocation targetParent = ColorList.this.dragTargetParent;

                    int insertAt = dragTargetIndex > dragFromIndex ? dragTargetIndex - 1 : dragTargetIndex;
                    Row moved = ColorList.this.children().remove(dragFromIndex);
                    insertAt = Mth.clamp(insertAt, 0, ColorList.this.children().size());
                    ColorList.this.children().add(insertAt, moved);

                    if (targetParent != null && !targetParent.equals(originParent)) {
                        moveToHub(targetParent);
                    } else {
                        SectionColorsScreen.this.markOrderDirty();
                    }
                }
                if (ColorList.this.dragActive) {
                    for (int i = 0; i < all.size(); i++) {
                        Row r = all.get(i);
                        r.settleFrom(shownTops.get(r) - ColorList.this.getRowTop(i));
                    }
                }
                dragRow = null;
                ColorList.this.dragActive = false;
                return true;
            }

            private void moveToHub(ResourceLocation newParent) {
                Config.routeTo(data.id(), newParent);
                LiveColors.moveToParent(data.id(), newParent);
                createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams());
                SectionColorsScreen.this.markOrderDirty();
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {

                if (dragRow == this && ColorList.this.dragActive && !ColorList.this.renderingGhost) {
                    return;
                }
                if (!ColorList.this.renderingGhost) {
                    top += Math.round(slideOffset());
                }

                float alpha = 1f;

                if (alpha <= 0.03f) {
                    return;
                }

                if (sliver) {
                    renderSliver(g, left, top, rowWidth, rowHeight, mouseX, mouseY, alpha);
                    return;
                }

                if (!ColorList.this.renderingGhost && !SectionColorsScreen.this.classic
                        && data.id().equals(SectionColorsScreen.this.selectedId)) {
                    int selAccent = GlassSkin.accent();
                    g.fill(left, top, left + rowWidth, top + rowHeight, MenuSkin.fade(selAccent, 0.14f));
                    g.fill(left, top, left + 2, top + rowHeight, selAccent);
                }

                if (!ColorList.this.renderingGhost) {
                    ResourceLocation groupParent = ColorList.this.parentIdAt(index);
                    Integer highlight = groupParent != null ? Config.highlightColorFor(groupParent) : null;
                    int lineColor = highlight != null ? (0x90 << 24) | (highlight & 0x00FFFFFF)
                            : MenuSkin.accent(0x90FFFFFF);
                    int lineX = left + SUB_LINE_X;
                    int lineBottom = top + rowHeight;
                    int gapToNext = ColorList.this.rowHeight - rowHeight;
                    if (ColorList.this.isLastInGroup(index)) {
                        int fadeLen = Math.min(SUB_LINE_FADE, rowHeight);
                        int fadeStart = lineBottom - fadeLen;
                        if (fadeStart > top) {
                            g.fill(lineX, top, lineX + 2, fadeStart, mulAlpha(lineColor, alpha));
                        }
                        g.fillGradient(lineX, fadeStart, lineX + 2, lineBottom,
                                mulAlpha(lineColor, alpha), mulAlpha(lineColor, 0f));
                    } else {
                        g.fill(lineX, top, lineX + 2, lineBottom + gapToNext, mulAlpha(lineColor, alpha));
                    }
                }

                int contentLeft = left + SUB_INDENT;

                int textY = top + (rowHeight - 8) / 2;

                if (data.readOnly()) {
                    int previewW = PREVIEW_W;
                    int phantomEditX = left + rowWidth - 44;
                    String tag = Component.translatable("createaddonorganizer.colors.native").getString();
                    int tagX = phantomEditX - 10 - font.width(tag);
                    int nameX = contentLeft + previewW + 8;
                    int nameMaxWidth = tagX - 4 - nameX;
                    Component name = truncatedName(data, nameMaxWidth);
                    int primary = mulAlpha(data.nativeTextColor() != null ? data.nativeTextColor() : 0xFFAAAAAA, alpha);
                    int secondary = mulAlpha(data.nativeSecondaryTextColor() != null ? data.nativeSecondaryTextColor() : 0xFF777777, alpha);
                    TwoToneText.draw(g, font, name, nameX, textY, primary, secondary);
                    g.drawString(font, tag, tagX, textY, mulAlpha(0xFFAAAAAA, alpha));
                    return;
                }

                int previewW = PREVIEW_W;
                String previewTooltip;
                int previewX1;
                int previewY1;
                int previewX2;
                int previewY2;
                String bannerRef = Config.bannerRefFor(data.id());
                if (bannerRef != null) {
                    int th = BannerTextures.HEIGHT;
                    int ty = top + (rowHeight - th) / 2;
                    g.fill(contentLeft - 1, ty - 1, contentLeft + previewW + 1, ty + th + 1, mulAlpha(0xFF000000, alpha));
                    ResourceLocation tex = BannerTextures.resolve(bannerRef);
                    if (tex != null) {
                        g.setColor(1f, 1f, 1f, alpha);
                        BannerTextures.blitCropped(g, tex, contentLeft, ty, previewW, th,
                                BannerAnimation.sheetHeight(tex));
                        g.setColor(1f, 1f, 1f, 1f);
                    }
                    previewTooltip = Component.translatable("createaddonorganizer.banner.mode.image").getString();
                    previewX1 = contentLeft - 1;
                    previewY1 = ty - 1;
                    previewX2 = contentLeft + previewW + 1;
                    previewY2 = ty + th + 1;
                } else {
                    int swatch = 16;
                    int sy = top + (rowHeight - swatch) / 2;
                    int sx = contentLeft + (previewW - swatch) / 2;
                    ColorSpec bannerSpec = opaqueSpec(previewBannerColor(data.id()));
                    g.fill(sx, sy, sx + swatch, sy + swatch, mulAlpha(0xFF000000, alpha));
                    BannerFill.draw(g, sx + 1, sy + 1, sx + swatch - 1, sy + swatch - 1, mulAlphaSpec(bannerSpec, alpha));
                    previewTooltip = Config.formatColorSpec(bannerSpec, true);
                    previewX1 = sx;
                    previewY1 = sy;
                    previewX2 = sx + swatch;
                    previewY2 = sy + swatch;
                }

                if (!ColorList.this.renderingGhost && mouseX >= previewX1 && mouseX < previewX2
                        && mouseY >= previewY1 && mouseY < previewY2) {
                    if (SectionColorsScreen.this.classic) {
                        SectionColorsScreen.this.hoverPreviewTooltip = Component.literal(previewTooltip);
                    } else {
                        SectionColorsScreen.this.popupEntry = data;
                    }
                }

                int widgetY = top + (rowHeight - 20) / 2;
                int actionX = left + rowWidth;
                if (edit != null) {
                    edit.setX(left + rowWidth - edit.getWidth());
                    edit.setY(widgetY);
                    actionX = edit.getX();
                }
                if (tab != null) {
                    tab.setX(actionX - 2 - tab.getWidth());
                    tab.setY(widgetY);
                    actionX = tab.getX();
                }
                int nameX = contentLeft + previewW + 8;

                if (data.id().equals(renamingId)) {
                    float widgetAlpha = Math.max(alpha, 0.04f);
                    renameCancel.setX(actionX - 22);
                    renameCancel.setY(widgetY);
                    renameConfirm.setX(renameCancel.getX() - 22);
                    renameConfirm.setY(widgetY);
                    renameBox.setX(nameX - 4);
                    renameBox.setY(widgetY);
                    renameBox.setWidth(renameConfirm.getX() - 4 - nameX + 4);
                    renameBox.render(g, mouseX, mouseY, partialTick);
                    renameConfirm.setAlpha(widgetAlpha);
                    renameCancel.setAlpha(widgetAlpha);
                    renameConfirm.render(g, mouseX, mouseY, partialTick);
                    renameCancel.render(g, mouseX, mouseY, partialTick);
                } else {
                    int tagRoom = 0;
                    if (PackDefaults.hasAnyFor(data.id())) {
                        String tag = Component.translatable("createaddonorganizer.colors.pack").getString();
                        tagRoom = font.width(tag) + 6;
                        g.drawString(font, tag, actionX - 10 - font.width(tag), textY,
                                mulAlpha(PACK_TAG_COLOR, alpha));
                    }
                    int nameMaxWidth = actionX - 10 - nameX - tagRoom;
                    Component name = truncatedName(data, nameMaxWidth);
                    ColorSpec primary = mulAlphaSpec(previewTextColor(data.id()), alpha);
                    ColorSpec secondary = previewTextSecondaryColor(data.id());
                    boolean shadowOn = Config.titleTextShadow(data.id());
                    Integer shadowOverride = shadowOn ? Config.textShadowColorFor(data.id()) : null;
                    boolean vanillaShadow = shadowOn && shadowOverride == null;
                    ColorSpec outline = Config.textOutlineColorFor(data.id());
                    TwoToneText.draw(g, font, name, nameX, textY, nameX + nameMaxWidth, primary,
                            secondary != null ? mulAlphaSpec(secondary, alpha) : null,
                            Config.twoToneSplitFor(data.id()), vanillaShadow,
                            shadowOverride != null ? mulAlpha(shadowOverride, alpha) : 0,
                            outline != null ? mulAlphaSpec(outline, alpha) : null);
                }

                if (tab != null) {
                    tab.setAlpha(Math.max(alpha, 0.04f));
                    tab.render(g, mouseX, mouseY, partialTick);
                }
                if (edit != null) {
                    edit.setAlpha(Math.max(alpha, 0.04f));
                    edit.render(g, mouseX, mouseY, partialTick);
                }

                if (Screen.hasShiftDown() && hovered && (!data.parent() || isDeletableHub())) {
                    g.fill(left, top, left + rowWidth, top + rowHeight, mulAlpha(0x80AA2E24, alpha));
                }
            }

            private Component boldFitting(String name, int maxWidth) {
                Component bold = Component.literal(name).withStyle(ChatFormatting.BOLD);
                if (font.width(bold) <= maxWidth) {
                    return bold;
                }
                String trimmed = name;
                while (!trimmed.isEmpty()
                        && font.width(Component.literal(trimmed).withStyle(ChatFormatting.BOLD)) > maxWidth) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                return Component.literal(trimmed).withStyle(ChatFormatting.BOLD);
            }

            private Component truncatedName(SectionCatalog.Entry entry, int maxWidth) {
                String full = entry.name().getString();
                if (full.isBlank()) {
                    Component placeholder = Component.literal(entry.id().toString())
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
                    return placeholder;
                }
                Component base;
                if (font.width(full) <= maxWidth) {
                    base = entry.name();
                } else {
                    String ellipsis = "...";
                    int budget = Math.max(0, maxWidth - font.width(ellipsis));
                    base = Component.literal(font.plainSubstrByWidth(full, budget) + ellipsis);
                }
                return base;
            }
        }
    }

    private static int iconOffsetX(int width, int icon) {
        return Math.round((width - icon) / 2f);
    }

    private static int iconOffsetY(int height, int icon) {
        return BUTTON_FACE_TOP + Math.round((height - BUTTON_FACE_TOP - BUTTON_FACE_BOTTOM - icon) / 2f);
    }

    private static class HeartButton extends Button {
        private static final ResourceLocation HEART_ICON = ResourceLocation.withDefaultNamespace("hud/heart/full");
        private static final int ICON_SIZE = 9;

        HeartButton(int x, int y, int size, OnPress onPress) {
            super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.credits")));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            int ix = getX() + iconOffsetX(getWidth(), ICON_SIZE);
            int iy = getY() + iconOffsetY(getHeight(), ICON_SIZE);

            RenderSystem.enableBlend();
            g.setColor(0f, 0f, 0f, this.alpha);
            g.blitSprite(HEART_ICON, ix - 1, iy, ICON_SIZE, ICON_SIZE);
            g.blitSprite(HEART_ICON, ix + 1, iy, ICON_SIZE, ICON_SIZE);
            g.blitSprite(HEART_ICON, ix, iy - 1, ICON_SIZE, ICON_SIZE);
            g.blitSprite(HEART_ICON, ix, iy + 1, ICON_SIZE, ICON_SIZE);

            g.setColor(1f, 1f, 1f, this.alpha);
            g.blitSprite(HEART_ICON, ix, iy, ICON_SIZE, ICON_SIZE);
            g.setColor(1f, 1f, 1f, 1f);
        }
    }

    private static class BugButton extends Button {
        private static final ResourceLocation BUG_ICON = ResourceLocation.fromNamespaceAndPath(
                createaddonorganizer.MODID, "textures/gui/bug.png");
        private static final int ICON_SIZE = 13;

        BugButton(int x, int y, int size, OnPress onPress) {
            super(x, y, size, size, Component.empty(), onPress, DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.bugReport")));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(g, mouseX, mouseY, partialTick);
            int ix = getX() + iconOffsetX(getWidth(), ICON_SIZE);
            int iy = getY() + iconOffsetY(getHeight(), ICON_SIZE);

            RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, this.alpha);
            g.blit(BUG_ICON, ix, iy, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            g.setColor(1f, 1f, 1f, 1f);
        }
    }
}

