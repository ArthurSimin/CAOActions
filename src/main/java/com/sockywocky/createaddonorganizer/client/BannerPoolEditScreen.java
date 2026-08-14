package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BannerPoolEditScreen extends Screen {
    private static final int PAD = 10;
    private static final float TITLE_SCALE = 1.6f;

    private final Screen parent;
    private final ResourceLocation tabId;
    private final Set<String> pool;
    private final Set<String> originalPool;
    private PoolList list;
    private Component hoverBannerTooltip;
    private String query = "";
    private double savedScroll;
    private int panelW = MenuLayout.PANEL_W;
    private int columns = 1;
    private EditBox search;

    private int chromeX;
    private int chromeY;
    private int chromeW;
    private int chromeH;
    private int searchX;
    private int searchY;
    private int searchW;

    public BannerPoolEditScreen(Screen parent, ResourceLocation tabId, String label) {
        super(Component.translatable("createaddonorganizer.bannerPool.title", label));
        this.parent = parent;
        this.tabId = tabId;
        this.pool = new LinkedHashSet<>(BannerPools.poolFor(tabId));
        this.originalPool = Set.copyOf(this.pool);
    }

    @Override
    protected void init() {
        panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);
        columns = BannerGrid.columnsFor(panelW);

        chromeX = panelX - PAD;
        chromeW = panelW + PAD * 2;
        chromeY = MenuLayout.ROW_1 - PAD;
        chromeH = this.height - 6 - chromeY;

        searchX = panelX;
        searchY = MenuLayout.ROW_1;
        searchW = panelW;
        search = new EditBox(this.font, searchX + 18, searchY, searchW - 24, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.bannerPool.search"));
        search.setHint(Component.translatable("createaddonorganizer.bannerPool.search"));
        search.setMaxLength(64);
        search.setBordered(false);
        search.setValue(query);
        search.setResponder(this::onQueryChanged);
        addWidget(search);

        int cancelY = chromeY + chromeH - PAD - MenuLayout.ROW_H;
        int buttonsY = cancelY - MenuLayout.ROW_H - MenuLayout.GAP;
        int listTop = MenuLayout.ROW_1 + MenuLayout.ROW_H + 8;
        int listBottom = buttonsY - 8;

        list = new PoolList(this.minecraft, panelW, listBottom - listTop, listTop, BannerTextures.HEIGHT + 6);
        list.setX(panelX);
        list.setRows(visibleRefs());
        addRenderableWidget(list);
        list.setScrollAmount(savedScroll);

        int half = MenuLayout.split(panelW, 2);
        addRenderableWidget(new GlassButton(panelX, buttonsY, half, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.banner.upload"), b -> upload()));
        addRenderableWidget(new GlassButton(panelX + panelW - half, buttonsY, half, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.bannerPool.save"), b -> save())
                .style(GlassButton.Style.ACCENT)
                .changed(() -> !pool.equals(originalPool)));
        addRenderableWidget(new GlassButton(panelX, cancelY, panelW, MenuLayout.ROW_H,
                Component.translatable("gui.cancel"), b -> onClose()));
    }

    private void onQueryChanged(String value) {
        if (value.equals(query)) {
            return;
        }
        query = value;
        savedScroll = 0.0d;
        if (list != null) {
            list.setRows(visibleRefs());
        }
    }

    private List<String> allRefs() {
        List<String> gallery = BannerTextures.gallery();
        List<String> refs = new ArrayList<>(gallery);
        for (String ref : pool) {
            if (!gallery.contains(ref)) {
                refs.add(ref);
            }
        }
        return refs;
    }

    private boolean matchesQuery(String ref) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty()
                || displayFilename(ref).toLowerCase(Locale.ROOT).contains(needle)
                || ref.toLowerCase(Locale.ROOT).contains(needle);
    }

    private List<String> visibleRefs() {
        List<String> matches = new ArrayList<>();
        for (String ref : allRefs()) {
            if (matchesQuery(ref)) {
                matches.add(ref);
            }
        }
        return matches;
    }

    private void upload() {
        Optional<Path> chosen = BannerTextures.chooseFile();
        if (chosen.isEmpty()) {
            return;
        }
        try {
            String ref = BannerTextures.importFile(chosen.get());
            pool.add(ref);
            if (list != null) {
                savedScroll = list.scrollTarget();
            }
            if (!matchesQuery(ref)) {
                query = "";
                savedScroll = 0.0d;
            }
            rebuildWidgets();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to import banner image", e);
        }
    }

    private void save() {
        try {
            BannerPools.setPool(tabId, List.copyOf(pool));
            Notice.show(Component.translatable("createaddonorganizer.bannerPool.saved"), Notice.GREEN);
            onClose();
        } catch (BannerPools.DevWriteException e) {
            Notice.show(Component.translatable("createaddonorganizer.devmode.writeFailed", e.getMessage()), Notice.RED);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save banner pool for {}", tabId, e);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        GlassSkin.panel(g, chromeX, chromeY, chromeW, chromeH);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoverBannerTooltip = null;
        super.render(g, mouseX, mouseY, partialTick);

        String titleText = this.title.getString();
        g.pose().pushPose();
        g.pose().scale(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE);
        g.drawString(this.font, titleText,
                Math.round(this.width / 2f / TITLE_SCALE) - this.font.width(titleText) / 2,
                Math.round(MenuLayout.TITLE_Y / TITLE_SCALE), GlassSkin.titleTextColor(), GlassSkin.shadow());
        g.pose().popPose();

        Component description = Component.translatable("createaddonorganizer.bannerPool.description");
        g.drawString(this.font, description, this.width / 2 - this.font.width(description) / 2,
                MenuLayout.DESC_Y, GlassSkin.bodyTextColor(), GlassSkin.shadow());

        GlassSkin.widgetBox(g, searchX, searchY, searchW, MenuLayout.ROW_H, search.isFocused());
        GlassSidebar.magnifier(g, searchX + 6, searchY + 6, GlassSkin.bodyTextColor());
        g.pose().pushPose();
        g.pose().translate(0f, (MenuLayout.ROW_H - 8) / 2f, 0f);
        search.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();

        if (list != null && list.isEmpty()) {
            Component empty = Component.translatable("createaddonorganizer.bannerPool.noMatches");
            g.drawString(this.font, empty, this.width / 2 - this.font.width(empty) / 2,
                    list.getY() + 20, GlassSkin.bodyTextColor(), GlassSkin.shadow());
        }

        if (hoverBannerTooltip != null) {
            g.renderTooltip(this.font, hoverBannerTooltip, mouseX, mouseY);
        }
    }

    private static String displayFilename(String ref) {
        if (ref.startsWith("file:") || ref.startsWith("remote:")) {
            return ref.substring(ref.indexOf(':') + 1);
        }
        int slash = ref.lastIndexOf('/');
        return slash >= 0 ? ref.substring(slash + 1) : ref;
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    private class PoolList extends ContainerObjectSelectionList<PoolList.Row> {
        private final ListGlide glide = new ListGlide();

        PoolList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void setRows(List<String> refs) {
            clearEntries();
            for (List<String> group : BannerGrid.chunk(refs, columns)) {
                addEntry(new Row(group));
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
            return BannerGrid.rowWidth(columns);
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final List<String> refs;

            Row(List<String> refs) {
                this.refs = refs;
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
            public boolean mouseClicked(double mx, double my, int button) {
                if (button != 0) {
                    return false;
                }
                int cell = BannerGrid.cellAt(mx, PoolList.this.getRowLeft(), getRowWidth(), columns);
                if (cell < 0 || cell >= refs.size()) {
                    return false;
                }
                String ref = refs.get(cell);
                if (!pool.add(ref)) {
                    pool.remove(ref);
                }
                return true;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int by = top + (rowHeight - BannerTextures.HEIGHT) / 2;
                for (int i = 0; i < refs.size(); i++) {
                    String ref = refs.get(i);
                    int bx = BannerGrid.cellX(left, rowWidth, columns, i);
                    boolean bannerHovered = BannerAnimation.isHovering(bx, by, BannerTextures.WIDTH,
                            BannerTextures.HEIGHT, mouseX, mouseY);
                    boolean inPool = pool.contains(ref);
                    int accent = GlassSkin.accent();
                    int border = inPool ? accent
                            : (bannerHovered ? GlassSkin.hoverLine(true) : GlassSkin.borderColor());
                    g.fill(bx - 1, by - 1, bx + BannerTextures.WIDTH + 1, by + BannerTextures.HEIGHT + 1, border);
                    ResourceLocation texture = BannerTextures.resolve(ref);
                    if (texture != null) {
                        int frame = BannerAnimation.get(texture)
                                .map(info -> BannerAnimation.currentFrame(texture, info, bannerHovered)).orElse(0);
                        g.blit(texture, bx, by, 0.0F, frame * BannerTextures.HEIGHT, BannerTextures.WIDTH,
                                BannerTextures.HEIGHT, BannerTextures.WIDTH, BannerAnimation.sheetHeight(texture));
                    }
                    if (bannerHovered) {
                        BannerPoolEditScreen.this.hoverBannerTooltip = Component.literal(displayFilename(ref));
                    }
                    if (inPool) {
                        Component check = Component.literal("✓");
                        int cx = bx + BannerTextures.WIDTH - font.width(check) - 3;
                        int cy = by + BannerTextures.HEIGHT - 11;
                        g.fill(cx - 2, cy - 2, cx + font.width(check) + 2, cy + 10, 0xC0000000);
                        g.drawString(font, check, cx, cy, accent, false);
                    }
                }
            }
        }
    }
}
