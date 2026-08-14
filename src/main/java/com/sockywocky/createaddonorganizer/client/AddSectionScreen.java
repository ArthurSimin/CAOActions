package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.AbsorbedTabs;
import com.sockywocky.createaddonorganizer.AddonDetection;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.LayoutApplier;
import com.sockywocky.createaddonorganizer.SectionCatalog;
import com.sockywocky.createaddonorganizer.TabLayout;
import com.sockywocky.createaddonorganizer.TabLayoutStore;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

public class AddSectionScreen extends Screen implements EmbeddedPane {
    private enum Mode { SUB, MAIN }

    private static final int PANEL_W = 400;
    private static final int PAD = 10;
    private static final int BUTTON_H = 18;
    private static final int TITLE_Y = 16;
    private static final int CHROME_Y = 36;
    private static final int ROW_1 = CHROME_Y + PAD;
    private static final int FILTER_W = 92;
    private static final float TITLE_SCALE = 1.6f;

    private static final int BAND_H = 26;

    private int panelW = PANEL_W;
    private boolean showPlaced = true;
    private boolean embedded;
    private int embedX;
    private int embedY;
    private int embedW;
    private int embedH;
    private int chromeY = CHROME_Y;
    private Runnable onEmbeddedDone;
    private Runnable onEmbeddedChanged;

    @Override
    public void onEmbeddedChanged(Runnable listener) {
        this.onEmbeddedChanged = listener;
    }

    @Override
    public void embedInto(int x, int y, int width, int height, Runnable onDone) {
        this.embedded = true;
        this.embedX = x;
        this.embedY = y;
        this.embedW = width;
        this.embedH = height;
        this.onEmbeddedDone = onDone;
    }

    private int areaX() {
        return embedded ? embedX : 0;
    }

    private int areaW() {
        return embedded ? embedW : this.width;
    }

    private int areaCenterX() {
        return areaX() + areaW() / 2;
    }

    private final Screen returnTo;
    private ResourceLocation currentHub;
    private Mode mode = Mode.SUB;
    private String searchQuery = "";
    private EditBox searchBox;
    private CandidateList list;

    private int chromeX;
    private int chromeW;
    private int chromeH;
    private int searchX;
    private int searchY;
    private int searchW;

    public AddSectionScreen(Screen returnTo) {
        super(Component.translatable("createaddonorganizer.addsection.title"));
        this.returnTo = returnTo;
        this.currentHub = firstHub();
    }

    private static ResourceLocation firstHub() {
        return SectionCatalog.knownHubs().stream()
                .min(Comparator.comparing(id -> nameOf(id).getString(), String.CASE_INSENSITIVE_ORDER))
                .orElse(null);
    }

    private boolean showHubPicker() {
        return mode == Mode.SUB && currentHub != null;
    }

    @Override
    protected void init() {
        panelW = Math.min(PANEL_W, areaW() - (embedded ? PAD * 4 : 60));
        int panelX = areaX() + (areaW() - panelW) / 2;
        int half = MenuLayout.split(panelW, 2);

        chromeX = embedded ? areaX() : panelX - PAD;
        chromeW = embedded ? areaW() : panelW + PAD * 2;
        chromeY = embedded ? embedY : CHROME_Y;
        chromeH = embedded ? embedH : this.height - 6 - chromeY;
        int rowTop = chromeY + PAD + (embedded ? BAND_H : 0);

        if (showHubPicker()) {
            addRenderableWidget(new GlassButton(panelX, rowTop, half, BUTTON_H, modeLabel(), b -> toggleMode()));
            CycleActionButton hubButton = new CycleActionButton(panelX + panelW - half, rowTop, half, BUTTON_H,
                    hubLabel(),
                    () -> {
                        currentHub = cycleHub(currentHub, 1);
                        rebuildWidgets();
                    },
                    () -> {
                        currentHub = cycleHub(currentHub, -1);
                        rebuildWidgets();
                    });
            hubButton.setTooltip(Tooltip.create(
                    Component.translatable("createaddonorganizer.addsection.hub.cycleHint")));
            addRenderableWidget(hubButton);
        } else {
            addRenderableWidget(new GlassButton(panelX, rowTop, panelW, BUTTON_H, modeLabel(), b -> toggleMode()));
        }

        searchY = rowTop + BUTTON_H + MenuLayout.GAP;
        searchX = panelX;
        searchW = panelW - FILTER_W - MenuLayout.GAP;
        addRenderableWidget(new GlassButton(panelX + panelW - FILTER_W, searchY, FILTER_W, BUTTON_H,
                placedLabel(), b -> {
                    showPlaced = !showPlaced;
                    rebuildWidgets();
                }));

        searchBox = new EditBox(this.font, searchX + 18, searchY, searchW - 24, BUTTON_H,
                Component.translatable("createaddonorganizer.addsection.search"));
        searchBox.setHint(Component.translatable("createaddonorganizer.addsection.search"));
        searchBox.setBordered(false);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(s -> {
            searchQuery = s;
            refreshList(0);
        });
        addWidget(searchBox);

        int doneY = chromeY + chromeH - PAD - BUTTON_H;
        int listTop = searchY + BUTTON_H + 8;
        int listBottom = doneY - 8;

        list = new CandidateList(this.minecraft, panelW, listBottom - listTop, listTop, 20);
        list.setX(panelX);
        for (ResourceLocation id : candidates()) {
            list.add(id);
        }
        addRenderableWidget(list);

        addRenderableWidget(new GlassButton(panelX, doneY, panelW, BUTTON_H,
                Component.translatable("gui.done"), b -> onClose()).style(GlassButton.Style.ACCENT));
    }

    private void toggleMode() {
        mode = mode == Mode.SUB ? Mode.MAIN : Mode.SUB;
        rebuildWidgets();
    }

    private void refreshList(double scrollAmount) {
        list.setEntries(candidates());
        list.setScrollAmount(scrollAmount);
    }

    private Component modeLabel() {
        String key = mode == Mode.SUB ? "createaddonorganizer.addsection.mode.sub" : "createaddonorganizer.addsection.mode.main";
        return Component.translatable("createaddonorganizer.addsection.mode").append(": ").append(Component.translatable(key));
    }

    private Component placedLabel() {
        return Component.translatable("createaddonorganizer.addsection.showPlaced").append(": ")
                .append(Component.translatable(showPlaced ? "options.on" : "options.off"));
    }

    private Component hubLabel() {
        return Component.translatable("createaddonorganizer.addsection.hub").append(": ").append(nameOf(currentHub));
    }

    private static ResourceLocation cycleHub(ResourceLocation current, int step) {
        List<ResourceLocation> hubs = SectionCatalog.knownHubs().stream()
                .sorted(Comparator.comparing(id -> nameOf(id).getString(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (hubs.isEmpty()) {
            return current;
        }
        int index = hubs.indexOf(current);
        return hubs.get(Math.floorMod(index + step, hubs.size()));
    }

    private List<ResourceLocation> candidates() {
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        Set<ResourceLocation> knownHubs = SectionCatalog.knownHubs();
        Map<ResourceLocation, Component> names = new HashMap<>();
        List<ResourceLocation> out = new ArrayList<>();
        for (var entry : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            boolean eligible = mode == Mode.SUB
                    ? AddonDetection.isSubSectionCandidate(id, knownHubs)
                    : AddonDetection.isHubPromotionCandidate(id, knownHubs);
            if (!eligible || (!showPlaced && AddonDetection.isPlaced(id))) {
                continue;
            }
            Component name = nameOf(id);
            if (name.getString().isBlank()) {
                continue;
            }
            if (query.isEmpty() || name.getString().toLowerCase(Locale.ROOT).contains(query)) {
                names.put(id, name);
                out.add(id);
            }
        }
        out.sort((a, b) -> names.get(a).getString().compareToIgnoreCase(names.get(b).getString()));
        return out;
    }

    private static Component nameOf(ResourceLocation id) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        return tab != null ? tab.getDisplayName() : Component.literal(id.toString());
    }

    private void pick(ResourceLocation id) {
        double scroll = list.getScrollAmount();
        if (mode == Mode.SUB) {
            addSubSection(id);
        } else {
            addMainSection(id);
        }
        refreshList(scroll);
        if (onEmbeddedChanged != null) {
            onEmbeddedChanged.run();
        }
    }

    private void addSubSection(ResourceLocation id) {
        if (currentHub == null) {
            return;
        }
        Config.addForceInclude(id);
        Config.removeForceExclude(id);
        if (!foldIntoLayout(id)) {
            Config.routeTo(id, currentHub);
        }
        AbsorbedTabs.IDS.add(id);
        LiveColors.remove(id);
        if (SimulatedSupport.isMainTab(currentHub)) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
            if (tab != null) {
                SimulatedHub.inject(id, tab.getDisplayName());
                SimulatedHub.foldItems(id, tab.getDisplayItems());
            }
        } else {
            Section<?> section = createaddonorganizer.sectionFromLiveTab(id);
            if (section != null) {
                FancyTabSections.addSection(currentHub, section);
            }
        }
        createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams());
    }

    private boolean foldIntoLayout(ResourceLocation id) {
        TabLayout layout = TabLayoutStore.byId(currentHub);
        if (layout == null || layout.sectionCount() == 0) {
            return false;
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        if (tab == null) {
            return false;
        }
        TabLayout updated = layout.withTabFolded(id, nameOf(id).getString(),
                LayoutApplier.producedIds(tab.getDisplayItems()));
        if (updated == layout) {
            return false;
        }
        TabLayoutStore.put(updated);
        return true;
    }

    private void addMainSection(ResourceLocation id) {
        Config.addExtraMainSection(id);
        Config.removeForceExclude(id);
        createaddonorganizer.MANAGED_PARENTS.add(id);
        LiveColors.remove(id);
        if (!FancyTabSections.REGISTERED_TABS.containsKey(id)) {
            Section<?> section = createaddonorganizer.sectionFromLiveTab(id);
            if (section != null) {
                FancyTabSections.addSection(id, section);
            }
        }
        createaddonorganizer.refreshTabLayout(ClientRegistries.displayParams());
    }

    @Override
    public void onClose() {
        if (embedded) {
            onEmbeddedDone.run();
            return;
        }
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (embedded) {
            return;
        }
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
                Math.round(areaCenterX() / TITLE_SCALE) - this.font.width(titleText) / 2,
                Math.round((embedded ? chromeY + 6 : TITLE_Y) / TITLE_SCALE),
                GlassSkin.titleTextColor(), GlassSkin.shadow());
        g.pose().popPose();

        GlassSkin.widgetBox(g, searchX, searchY, searchW, BUTTON_H, searchBox.isFocused());
        GlassSidebar.magnifier(g, searchX + 6, searchY + 6, GlassSkin.bodyTextColor());
        g.pose().pushPose();
        g.pose().translate(0f, (BUTTON_H - 8) / 2f, 0f);
        searchBox.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();

        if (list.children().isEmpty()) {
            Component empty = Component.translatable("createaddonorganizer.colors.search.none");
            g.drawString(this.font, empty, areaCenterX() - this.font.width(empty) / 2,
                    list.getY() + 20, GlassSkin.bodyTextColor(), GlassSkin.shadow());
        }
    }

    private class CandidateList extends ContainerObjectSelectionList<CandidateList.Row> {
        private final ListGlide glide = new ListGlide();

        CandidateList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void add(ResourceLocation id) {
            addEntry(new Row(id));
        }

        void setEntries(List<ResourceLocation> ids) {
            replaceEntries(ids.stream().map(Row::new).toList());
        }

        @Override
        public int getRowWidth() {
            return AddSectionScreen.this.panelW - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final ResourceLocation id;
            private final Component name;
            private final boolean placed;
            private final ItemStack icon;
            private final Component placedTag;

            Row(ResourceLocation id) {
                this.id = id;
                this.name = nameOf(id);
                this.placed = AddonDetection.isPlaced(id);
                this.icon = SafeIcon.of(BuiltInRegistries.CREATIVE_MODE_TAB.get(id));
                this.placedTag = this.placed
                        ? Component.translatable("createaddonorganizer.addsection.placed")
                        : null;
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
                if (button == 0) {
                    pick(id);
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (hovered) {
                    int accent = GlassSkin.accent();
                    g.fill(left, top, left + rowWidth, top + rowHeight, MenuSkin.fade(accent, 0.12f));
                    g.fill(left, top, left + 2, top + rowHeight, accent);
                }
                SafeIcon.render(g, icon, left + 6, top + (rowHeight - 16) / 2);
                int textY = top + (rowHeight - 8) / 2;
                int tagWidth = placedTag != null ? font.width(placedTag) + 10 : 0;
                String fitted = font.plainSubstrByWidth(name.getString(),
                        Math.max(0, rowWidth - 28 - 6 - tagWidth));
                g.drawString(font, fitted, left + 28, textY,
                        hovered ? GlassSkin.titleTextColor() : GlassSkin.rowTextColor(), GlassSkin.shadow());
                if (placedTag != null) {
                    g.drawString(font, placedTag, left + rowWidth - font.width(placedTag) - 6, textY,
                            GlassSkin.bodyTextColor(), GlassSkin.shadow());
                }
            }
        }
    }
}

