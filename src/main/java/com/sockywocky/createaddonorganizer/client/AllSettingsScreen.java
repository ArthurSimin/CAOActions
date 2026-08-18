package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

public class AllSettingsScreen extends Screen {

    private static final String DISCORD_URL = "https://discord.com/invite/WgYePqcRTk";
    private static final String GITHUB_URL = "https://github.com/SockyWocky7/createaddonorganizer";

    private static final int OUTER = 6;
    private static final int PANEL_GAP = 6;

    private static final int SEARCH_H = 18;
    private static final int SEARCH_MAX_W = 320;
    private static final int RESET_W = 46;

    private static final int CARD_GAP = 5;
    private static final int CARD_PAD_X = 10;
    private static final int CARD_PAD_Y = 8;
    private static final int CARD_MIN_H = 34;
    private static final int GROUP_H = 22;
    private static final int LINE_H = 10;
    private static final int MAX_DESC_LINES = 3;
    private static final int SCROLLBAR_W = 4;

    private static final int TOGGLE_W = 24;
    private static final int TOGGLE_H = 12;
    private static final int KNOB_W = 10;
    private static final float TOGGLE_SECONDS = 0.14f;
    private static final int CHOICE_H = 14;
    private static final int CHOICE_MIN_W = 56;
    private static final int CHOICE_MAX_W = 122;
    private static final int SLIDER_W = 72;
    private static final int SLIDER_H = 12;
    private static final int BOX_H = 14;
    private static final int TEXT_BOX_W = 132;
    private static final int NUMBER_BOX_W = 66;
    private static final int COLOR_BOX_W = 74;
    private static final int SWATCH_W = 12;
    private static final int LIST_BUTTON_W = 84;

    private static final int PREVIEW_PAD_TOP = 18;
    private static final int PREVIEW_PAD_BOTTOM = 8;


    private static final ResourceLocation VANILLA_FIELD =
            ResourceLocation.withDefaultNamespace("widget/text_field");
    private static final ResourceLocation VANILLA_FIELD_FOCUS =
            ResourceLocation.withDefaultNamespace("widget/text_field_highlighted");

    private enum Page { SETTINGS, CREDITS, BUGS, INTEGRATIONS }

    private enum RowKind { HEADER, CARD, PREVIEW }

    private final Screen parent;

    private final List<SettingsCatalog.Category> categories = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<ValueBox> boxes = new ArrayList<>();
    private final Map<String, Float> toggleAnim = new HashMap<>();
    private final Scrollbar bar = new Scrollbar();
    private final SlidePreview preview = new SlidePreview();
    private final InfoPane infoPane = new InfoPane(this);
    private final PanelSlide slide = new PanelSlide();
    private final GlassSidebar sidebar;

    private static int lastCategory;
    private static String lastQuery = "";

    private boolean builtUnlocked;

    private Page page = Page.SETTINGS;
    private int selected;
    private String query = "";
    private EditBox searchBox;
    private int searchWidth;

    private int panelY;
    private int panelH;
    private int sidebarX;
    private int sidebarW;
    private int contentX;
    private int contentW;
    private int listTop;
    private int listBottom;

    private long frameNanos;
    private final GlassArrow backArrow = new GlassArrow();
    private SettingsCatalog.Option dragging;
    private SettingsCatalog.Option sliderEditOption;
    private EditBox sliderEditBox;
    private SettingsCatalog.Option openChoice;
    private int choiceX;
    private int choiceY;
    private int choiceH;
    private int choiceW;
    private Component hoverTip;

    public AllSettingsScreen(Screen parent, ModContainer container) {
        super(Component.translatable("createaddonorganizer.settings.title"));
        this.parent = parent;
        this.sidebar = new GlassSidebar(container, this::onClose)
                .title(this.title)
                .onUpdate(() -> openLink(UpdateCheck.PAGE_URL))
                .onFolder(() -> Util.getPlatform().openPath(FMLPaths.CONFIGDIR.get()),
                        Component.translatable("createaddonorganizer.settings.openFolder"));
    }

    @Override
    protected void init() {
        if (categories.isEmpty() || builtUnlocked != DevMode.isUnlocked()) {
            categories.clear();
            categories.addAll(SettingsCatalog.build());
            builtUnlocked = DevMode.isUnlocked();
            selected = Mth.clamp(lastCategory, 0, Math.max(0, categories.size() - 1));
            query = lastQuery;
        }

        panelY = OUTER;
        panelH = this.height - OUTER * 2;
        sidebarX = OUTER;
        sidebarW = GlassSidebar.widthFor(this.width);
        contentX = sidebarX + sidebarW + PANEL_GAP;
        contentW = this.width - contentX - OUTER;

        listTop = panelY + SEARCH_H + 12;
        listBottom = panelY + panelH - 4;
        bar.bounds(contentX + contentW - 4 - Scrollbar.WIDTH, listTop, listBottom - listTop);

        int searchW = Math.max(60, Math.min(SEARCH_MAX_W, contentW - 16 - RESET_W - 6));
        searchBox = new EditBox(this.font, contentX + 26, panelY + 11, Math.max(40, searchW - 24), SEARCH_H,
                Component.translatable("createaddonorganizer.settings.search"));
        searchBox.setHint(Component.translatable("createaddonorganizer.settings.search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(64);
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            if (!value.equals(query)) {
                query = value;
                lastQuery = value;
                if (!value.trim().isEmpty()) {
                    page = Page.SETTINGS;
                }
                bar.reset();
                rebuild();
            }
        });
        searchWidth = searchW;
        addWidget(searchBox);

        sidebar.rows().clear();
        for (int i = 0; i < categories.size(); i++) {
            int index = i;
            sidebar.rows().add(GlassSidebar.Row.of(categories.get(i).label(), () -> {
                slide.play(navIndex(), index);
                page = Page.SETTINGS;
                selected = index;
                lastCategory = selected;
                clearSearch();
                bar.reset();
                rebuild();
            }).active(() -> page == Page.SETTINGS && selected == index && query.trim().isEmpty()));
        }
        sidebar.rows().add(GlassSidebar.Row.gap());
        sidebar.rows().add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.credits.clearCache"),
                RemoteCache::clearAllReporting));
        sidebar.rows().add(GlassSidebar.Row.gap());
        sidebar.rows().add(GlassSidebar.Row.of(
                Component.translatable("createaddonorganizer.integrations.title"), () -> {
                    slide.play(navIndex(), categories.size());
                    page = Page.INTEGRATIONS;
                    clearSearch();
                    bar.reset();
                    infoPane.resetScroll();
                    rebuild();
                }).active(() -> page == Page.INTEGRATIONS));
        sidebar.rows().add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.colors.credits"), () -> {
            slide.play(navIndex(), categories.size() + 1);
            page = Page.CREDITS;
            clearSearch();
            bar.reset();
            infoPane.resetScroll();
            rebuild();
        }).active(() -> page == Page.CREDITS));
        sidebar.rows().add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.bugreport.title"), () -> {
            slide.play(navIndex(), categories.size() + 2);
            page = Page.BUGS;
            clearSearch();
            bar.reset();
            infoPane.resetScroll();
            rebuild();
        }).active(() -> page == Page.BUGS));
        sidebar.rows().add(GlassSidebar.Row.gap());
        sidebar.rows().add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.settings.discord"),
                () -> Util.getPlatform().openUri(DISCORD_URL)).external());
        sidebar.rows().add(GlassSidebar.Row.of(Component.translatable("createaddonorganizer.settings.github"),
                () -> Util.getPlatform().openUri(GITHUB_URL)).external());
        sidebar.setBounds(sidebarX, panelY, sidebarW, panelH);

        rebuild();
    }

    private int navIndex() {
        return switch (page) {
            case INTEGRATIONS -> categories.size();
            case CREDITS -> categories.size() + 1;
            case BUGS -> categories.size() + 2;
            default -> selected;
        };
    }

    private void rebuild() {
        closeSliderEdit(false);
        for (ValueBox box : boxes) {
            removeWidget(box);
        }
        boxes.clear();
        rows.clear();

        if (page == Page.SETTINGS) {
            buildSettings();
        } else {
            infoPane.setBounds(contentX, listTop, contentW, listBottom - listTop);
            infoPane.build(switch (page) {
                case CREDITS -> InfoPane.Kind.CREDITS;
                case INTEGRATIONS -> InfoPane.Kind.INTEGRATIONS;
                default -> InfoPane.Kind.BUGS;
            }, this.font);
        }
        layoutRows();
    }

    private void buildSettings() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            if (selected < 0 || selected >= categories.size()) {
                return;
            }
            SettingsCatalog.Category category = categories.get(selected);
            if (category.key().equals("slide")) {
                rows.add(Row.preview());
                preview.sample();
                preview.restart();
            }
            for (SettingsCatalog.Group group : category.groups()) {
                addGroup(group.title(), group.options());
            }
            return;
        }
        for (SettingsCatalog.Category category : categories) {
            for (SettingsCatalog.Group group : category.groups()) {
                List<SettingsCatalog.Option> hits = new ArrayList<>();
                for (SettingsCatalog.Option option : group.options()) {
                    if (option.search().contains(needle)) {
                        hits.add(option);
                    }
                }
                if (!hits.isEmpty()) {
                    Component header = group.title().getString().equals(category.label().getString())
                            ? category.label()
                            : Component.empty().append(category.label()).append(" / ").append(group.title());
                    addGroup(header, hits);
                }
            }
        }
    }

    private void openLink(String url) {
        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(url);
            }
            this.minecraft.setScreen(this);
        }, url, true));
    }

    private void addGroup(Component title, List<SettingsCatalog.Option> options) {
        rows.add(Row.header(title, 0));
        for (SettingsCatalog.Option option : options) {
            Row row = Row.card(option);
            rows.add(row);
            ValueBox box = createBox(option);
            if (box != null) {
                boxes.add(box);
                addWidget(box);
                row.box = box;
            }
        }
    }

    private ValueBox createBox(SettingsCatalog.Option option) {
        int width = switch (option.kind()) {
            case TEXT -> TEXT_BOX_W;
            case NUMBER -> NUMBER_BOX_W;
            case COLOR -> COLOR_BOX_W;
            default -> 0;
        };
        if (width == 0) {
            return null;
        }
        ValueBox box = new ValueBox(option, width);
        box.visible = false;
        return box;
    }

    private void layoutRows() {
        int width = cardWidth();
        int y = 0;
        for (Row row : rows) {
            row.y = y;
            switch (row.kind) {
                case HEADER -> row.height = GROUP_H;
                case PREVIEW -> row.height = PREVIEW_PAD_TOP + SlidePreview.HEIGHT + PREVIEW_PAD_BOTTOM;
                case CARD -> {
                    int controlW = controlWidth(row.option);
                    int textW = Math.max(40, width - CARD_PAD_X * 2 - controlW - 10);
                    row.title = this.font.split(row.option.title(), textW);
                    String description = row.option.description().getString();
                    List<FormattedCharSequence> wrapped = description.isBlank()
                            ? List.of()
                            : this.font.split(row.option.description(), textW);
                    row.truncated = wrapped.size() > MAX_DESC_LINES;
                    row.description = row.truncated ? wrapped.subList(0, MAX_DESC_LINES) : wrapped;
                    int lines = row.title.size() * LINE_H
                            + (row.description.isEmpty() ? 0 : 3 + row.description.size() * LINE_H);
                    row.height = Math.max(CARD_MIN_H, CARD_PAD_Y * 2 + lines);
                }
            }
            y += row.height + (row.kind == RowKind.HEADER ? 0 : CARD_GAP);
        }
        bar.content(y);
    }

    private int cardWidth() {
        return contentW - 16 - SCROLLBAR_W;
    }

    private int controlWidth(SettingsCatalog.Option option) {
        return switch (option.kind()) {
            case TOGGLE -> TOGGLE_W;
            case CHOICE -> choiceWidth(option);
            case SLIDER -> SLIDER_W + 6 + this.font.width(sliderLabel(option));
            case NUMBER -> NUMBER_BOX_W;
            case TEXT -> TEXT_BOX_W;
            case COLOR -> COLOR_BOX_W + SWATCH_W + 4;
            case LIST -> LIST_BUTTON_W;
        };
    }

    private int choiceWidth(SettingsCatalog.Option option) {
        int widest = 0;
        for (Enum<?> value : SettingsCatalog.choices(option)) {
            widest = Math.max(widest, this.font.width(choiceLabel(value)));
        }
        return Mth.clamp(widest + 20, CHOICE_MIN_W, CHOICE_MAX_W);
    }

    private static String choiceLabel(Enum<?> value) {
        return SettingsCatalog.choiceLabel(value);
    }

    private String sliderLabel(SettingsCatalog.Option option) {
        Object value = option.current();
        if (value instanceof Double number) {
            return String.format(Locale.ROOT, "%.2f", number);
        }
        return String.valueOf(value);
    }

    private void click() {
        Sfx.uiClick();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoverTip = null;
        sidebar.layout();
        MenuSkin.advanceBlend();
        this.renderBackground(g, mouseX, mouseY, partialTick);

        long now = System.nanoTime();
        float delta = frameNanos == 0 ? 0f : Math.min(0.25f, (now - frameNanos) / 1_000_000_000f);
        frameNanos = now;

        sidebar.render(g, this.font, mouseX, mouseY);
        panel(g, contentX, panelY, contentW, panelH);

        renderSearch(g, mouseX, mouseY);
        slide.begin(g);
        if (page == Page.SETTINGS) {
            renderRows(g, mouseX, mouseY, delta);
        } else {
            infoPane.render(g, this.font, mouseX, mouseY);
            hoverTip = infoPane.hoverTip();
        }

        for (Renderable renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
        slide.end(g);

        renderBackArrow(g, mouseX, mouseY, delta);
        renderChoicePopup(g, mouseX, mouseY);

        if (hoverTip == null) {
            hoverTip = sidebar.hoverTip();
        }
        if (hoverTip != null) {
            g.renderTooltip(this.font, this.font.split(hoverTip, 200), mouseX, mouseY);
        }
    }

    private static boolean vanilla() {
        return GlassSkin.vanilla();
    }

    private void renderBackArrow(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = GlassArrow.rightX(this.width);
        int y = GlassArrow.top(this.height);
        boolean hovered = GlassArrow.contains(mouseX, mouseY, x, y);
        backArrow.render(g, x, y, true, hovered, delta);
        if (hovered) {
            hoverTip = Component.translatable("createaddonorganizer.colors.title");
        }
    }

    private void panel(GuiGraphics g, int x, int y, int width, int height) {
        GlassSkin.panel(g, x, y, width, height);
    }

    private void widgetBox(GuiGraphics g, int x, int y, int width, int height, boolean hovered) {
        GlassSkin.widgetBox(g, x, y, width, height, hovered);
    }

    private int hoverLine(boolean lit) {
        return GlassSkin.hoverLine(lit);
    }

    private static void outline(GuiGraphics g, int x, int y, int width, int height, int color) {
        GlassSkin.outline(g, x, y, width, height, color);
    }

    private int cardColor(boolean hovered) {
        return GlassSkin.cardColor(hovered);
    }

    private int cardBorder(boolean hovered) {
        return GlassSkin.cardBorder(hovered);
    }

    private int borderColor() {
        return GlassSkin.borderColor();
    }

    private static boolean glass() {
        return GlassSkin.glass();
    }

    private int accent() {
        return GlassSkin.accent();
    }

    private boolean shadow() {
        return GlassSkin.shadow();
    }

    private int titleTextColor() {
        return GlassSkin.titleTextColor();
    }

    private int bodyTextColor() {
        return GlassSkin.bodyTextColor();
    }

    private int headingColor() {
        return GlassSkin.headingColor();
    }

    private void renderSearch(GuiGraphics g, int mouseX, int mouseY) {
        int x = contentX + 8;
        int y = panelY + 6;
        int width = searchWidth;
        if (vanilla()) {
            g.blitSprite(searchBox.isFocused() ? VANILLA_FIELD_FOCUS : VANILLA_FIELD, x, y, width, SEARCH_H);
        } else {
            g.fill(x, y, x + width, y + SEARCH_H, cardColor(false));
            outline(g, x, y, width, SEARCH_H, hoverLine(searchBox.isFocused()));
        }
        magnifier(g, x + 6, y + 6, bodyTextColor());
        searchBox.setX(x + 18);
        searchBox.setY(y + (SEARCH_H - 8) / 2);
        searchBox.setWidth(width - 24);
        searchBox.render(g, mouseX, mouseY, 0f);

        if (page != Page.SETTINGS) {
            return;
        }
        int resetX = contentX + contentW - 8 - RESET_W;
        boolean hovered = inside(mouseX, mouseY, resetX, y, RESET_W, SEARCH_H);
        widgetBox(g, resetX, y, RESET_W, SEARCH_H, hovered);
        Component label = Component.translatable("createaddonorganizer.settings.reset");
        g.drawString(this.font, label, resetX + (RESET_W - this.font.width(label)) / 2, y + (SEARCH_H - 8) / 2,
                hovered ? titleTextColor() : bodyTextColor(), shadow());
        if (hovered) {
            hoverTip = Component.translatable("createaddonorganizer.settings.reset.tooltip");
        }
    }

    private void renderRows(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int offset = bar.offset();
        int x = contentX + 8;
        int width = cardWidth();
        boolean overList = mouseX >= contentX && mouseX < contentX + contentW
                && mouseY >= listTop && mouseY < listBottom;

        g.enableScissor(contentX + 1, listTop, contentX + contentW - 1, listBottom);
        if (rows.isEmpty()) {
            Component empty = Component.translatable("createaddonorganizer.settings.empty");
            g.drawString(this.font, empty, x + (width - this.font.width(empty)) / 2, listTop + 20,
                    bodyTextColor(), shadow());
        }
        for (Row row : rows) {
            int y = listTop + row.y - offset;
            if (y + row.height < listTop - 8 || y > listBottom + 8) {
                if (row.box != null) {
                    row.box.visible = false;
                }
                continue;
            }
            boolean hovered = overList && mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + row.height;
            switch (row.kind) {
                case HEADER -> renderHeader(g, row, x, y, width);
                case CARD -> renderCard(g, row, x, y, width, mouseX, mouseY, hovered, delta);
                case PREVIEW -> renderPreview(g, x, y, width, hovered);
            }
        }
        g.disableScissor();

        bar.render(g, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g, Row row, int x, int y, int width) {
        if (glass()) {
            g.drawString(this.font, row.header, x + CARD_PAD_X, y + GROUP_H - 14,
                    row.color == 0 ? accent() : row.color, shadow());
            int ruleY = y + GROUP_H - 4;
            g.fill(x + 2, ruleY, x + width - 2, ruleY + 1, MenuSkin.fade(accent(), 0.45f));
            return;
        }
        int textWidth = this.font.width(row.header);
        int textX = x + width / 2 - textWidth / 2;
        int lineY = y + GROUP_H / 2;
        int color = borderColor();
        if (textX - 6 > x + 2) {
            g.fill(x + 2, lineY, textX - 6, lineY + 1, color);
            g.fill(textX + textWidth + 6, lineY, x + width - 2, lineY + 1, color);
        }
        g.drawString(this.font, row.header, textX, lineY - 4, row.color == 0 ? headingColor() : row.color,
                shadow());
    }

    private void renderPreview(GuiGraphics g, int x, int y, int width, boolean hovered) {
        int height = PREVIEW_PAD_TOP + SlidePreview.HEIGHT + PREVIEW_PAD_BOTTOM;
        g.fill(x, y, x + width, y + height, cardColor(hovered));
        outline(g, x, y, width, height, cardBorder(hovered));
        Component total = Component.translatable("createaddonorganizer.slide.total", preview.cycleMillis());
        g.drawString(this.font, total, x + (width - this.font.width(total)) / 2, y + 6, bodyTextColor(), shadow());
        preview.render(g, x + (width - SlidePreview.WIDTH) / 2, y + PREVIEW_PAD_TOP);
        if (hovered) {
            hoverTip = Component.translatable("createaddonorganizer.slide.play");
        }
    }

    private void renderCard(GuiGraphics g, Row row, int x, int y, int width, int mouseX, int mouseY,
            boolean hovered, float delta) {
        SettingsCatalog.Option option = row.option;
        g.fill(x, y, x + width, y + row.height, cardColor(hovered));
        outline(g, x, y, width, row.height, cardBorder(hovered));

        int textY = y + CARD_PAD_Y;
        for (FormattedCharSequence line : row.title) {
            g.drawString(this.font, line, x + CARD_PAD_X, textY, titleTextColor(), shadow());
            textY += LINE_H;
        }
        if (!row.description.isEmpty()) {
            textY += 3;
            for (int i = 0; i < row.description.size(); i++) {
                FormattedCharSequence line = row.description.get(i);
                g.drawString(this.font, line, x + CARD_PAD_X, textY, bodyTextColor(), shadow());
                if (row.truncated && i == row.description.size() - 1) {
                    g.drawString(this.font, "...", x + CARD_PAD_X + this.font.width(line), textY,
                            bodyTextColor(), shadow());
                }
                textY += LINE_H;
            }
            if (row.truncated && hovered) {
                hoverTip = option.description();
            }
        }

        if (!option.isDefault()) {
            g.fill(x + 1, y + 1, x + 3, y + row.height - 1, MenuSkin.fade(accent(), 0.65f));
        }

        int right = x + width - CARD_PAD_X;
        int middle = y + row.height / 2;
        row.controlRight = right;
        row.controlMiddle = middle;
        renderControl(g, row, option, right, middle, mouseX, mouseY, delta);
    }

    private void renderControl(GuiGraphics g, Row row, SettingsCatalog.Option option, int right, int middle,
            int mouseX, int mouseY, float delta) {
        switch (option.kind()) {
            case TOGGLE -> {
                int x = right - TOGGLE_W;
                int y = middle - TOGGLE_H / 2;
                boolean on = Boolean.TRUE.equals(option.current());
                toggle(g, x, y, advanceToggle(option.key(), on, delta),
                        inside(mouseX, mouseY, x, y, TOGGLE_W, TOGGLE_H));
            }
            case CHOICE -> {
                int width = choiceWidth(option);
                int x = right - width;
                int y = middle - CHOICE_H / 2;
                boolean hovered = inside(mouseX, mouseY, x, y, width, CHOICE_H) || openChoice == option;
                widgetBox(g, x, y, width, CHOICE_H, hovered);
                Object current = option.current();
                String label = current instanceof Enum<?> value ? choiceLabel(value) : String.valueOf(current);
                g.drawString(this.font, this.font.plainSubstrByWidth(label, width - 18), x + 5, y + 3,
                        titleTextColor(), shadow());
                chevron(g, x + width - 10, y + 6, bodyTextColor());
            }
            case SLIDER -> {
                String label = sliderLabel(option);
                int labelWidth = this.font.width(label);
                int x = right - SLIDER_W;
                int y = middle - SLIDER_H / 2;
                if (sliderEditOption == option && sliderEditBox != null) {
                    int boxX = x - 6 - labelWidth;
                    int boxW = right - boxX;
                    int boxY = middle - BOX_H / 2;
                    if (vanilla()) {
                        g.blitSprite(VANILLA_FIELD_FOCUS, boxX, boxY, boxW, BOX_H);
                    } else {
                        g.fill(boxX, boxY, boxX + boxW, boxY + BOX_H, cardColor(true));
                        outline(g, boxX, boxY, boxW, BOX_H, hoverLine(true));
                    }
                    sliderEditBox.setX(boxX + 4);
                    sliderEditBox.setY(boxY + (BOX_H - 8) / 2);
                    sliderEditBox.setWidth(boxW - 8);
                    sliderEditBox.render(g, mouseX, mouseY, 0f);
                    return;
                }
                g.drawString(this.font, label, x - 6 - labelWidth, middle - 4, bodyTextColor(), shadow());
                boolean overSlider = inside(mouseX, mouseY, x, y, SLIDER_W, SLIDER_H);
                slider(g, x, y, sliderFraction(option), overSlider || dragging == option);
                if (overSlider && hoverTip == null) {
                    hoverTip = Component.translatable("createaddonorganizer.settings.slider.typeHint");
                }
            }
            case NUMBER, TEXT -> positionBox(g, row, right - boxFrameWidth(row), middle - BOX_H / 2,
                    mouseX, mouseY);
            case COLOR -> {
                int boxX = right - COLOR_BOX_W;
                positionBox(g, row, boxX, middle - BOX_H / 2, mouseX, mouseY);
                int swatchX = boxX - SWATCH_W - 4;
                int argb = option.current() instanceof Integer value ? value : 0;
                g.fill(swatchX, middle - SWATCH_W / 2, swatchX + SWATCH_W, middle + SWATCH_W / 2, 0xFF000000);
                g.fill(swatchX + 1, middle - SWATCH_W / 2 + 1, swatchX + SWATCH_W - 1, middle + SWATCH_W / 2 - 1,
                        0xFF000000 | (argb & 0x00FFFFFF));
            }
            case LIST -> {
                int x = right - LIST_BUTTON_W;
                int y = middle - CHOICE_H / 2;
                boolean hovered = inside(mouseX, mouseY, x, y, LIST_BUTTON_W, CHOICE_H);
                widgetBox(g, x, y, LIST_BUTTON_W, CHOICE_H, hovered);
                Component label = Component.translatable("createaddonorganizer.settings.entries",
                        SettingsCatalog.listValue(option).size());
                g.drawString(this.font, label, x + (LIST_BUTTON_W - this.font.width(label)) / 2, y + 3,
                        hovered ? titleTextColor() : bodyTextColor(), shadow());
            }
        }
    }

    private float advanceToggle(String key, boolean on, float delta) {
        float target = on ? 1f : 0f;
        if (!Config.animOn(Config.ANIM_BUTTON_HOVER)) {
            toggleAnim.put(key, target);
            return target;
        }
        float current = toggleAnim.getOrDefault(key, target);
        float step = delta / TOGGLE_SECONDS;
        current = current < target ? Math.min(target, current + step) : Math.max(target, current - step);
        toggleAnim.put(key, current);
        return current;
    }

    private void positionBox(GuiGraphics g, Row row, int x, int y, int mouseX, int mouseY) {
        if (row.box == null) {
            return;
        }
        row.box.visible = true;
        row.box.setX(x + 4);
        row.box.setY(y + (BOX_H - 8) / 2);
        row.box.frameX = x;
        row.box.frameY = y;
        row.box.render(g, mouseX, mouseY, 0f);
    }

    private static int boxFrameWidth(Row row) {
        return row.box == null ? 0 : row.box.getWidth() + 8;
    }

    private double sliderFraction(SettingsCatalog.Option option) {
        Object current = option.current();
        if (current instanceof Integer value) {
            ModConfigSpec.Range<Integer> range = option.spec().getRange();
            if (range == null) {
                return 0;
            }
            double span = range.getMax() - (double) range.getMin();
            return span <= 0 ? 0 : (value - range.getMin()) / span;
        }
        if (current instanceof Double value) {
            ModConfigSpec.Range<Double> range = option.spec().getRange();
            if (range == null) {
                return 0;
            }
            double span = range.getMax() - range.getMin();
            return span <= 0 ? 0 : (value - range.getMin()) / span;
        }
        return 0;
    }

    private void applySlider(SettingsCatalog.Option option, double fraction, boolean commit) {
        double clamped = Mth.clamp(fraction, 0, 1);
        Object value;
        if (option.current() instanceof Integer) {
            ModConfigSpec.Range<Integer> range = option.spec().getRange();
            if (range == null) {
                return;
            }
            value = range.getMin() + (int) Math.round(clamped * (range.getMax() - (double) range.getMin()));
        } else {
            ModConfigSpec.Range<Double> range = option.spec().getRange();
            if (range == null) {
                return;
            }
            double raw = range.getMin() + clamped * (range.getMax() - range.getMin());
            value = Math.round(raw * 100d) / 100d;
        }
        if (!SettingsCatalog.accepts(option, value)) {
            return;
        }
        if (commit) {
            SettingsCatalog.apply(option, value);
            preview.restart();
        } else {
            SettingsCatalog.stage(option, value);
        }
    }

    private void openSliderEdit(SettingsCatalog.Option option) {
        closeSliderEdit(false);
        sliderEditOption = option;
        sliderEditBox = new EditBox(this.font, 0, 0, SLIDER_W, BOX_H, option.title());
        sliderEditBox.setBordered(false);
        sliderEditBox.setMaxLength(24);
        sliderEditBox.setValue(sliderLabel(option));
        sliderEditBox.setTextColor(titleTextColor());
        sliderEditBox.moveCursorToEnd(false);
        sliderEditBox.setHighlightPos(0);
        addWidget(sliderEditBox);
        setFocused(sliderEditBox);
        sliderEditBox.setFocused(true);
    }

    private void closeSliderEdit(boolean commit) {
        if (sliderEditOption == null) {
            return;
        }
        SettingsCatalog.Option option = sliderEditOption;
        EditBox box = sliderEditBox;
        sliderEditOption = null;
        sliderEditBox = null;
        if (box != null) {
            removeWidget(box);
            if (getFocused() == box) {
                setFocused(null);
            }
        }
        if (!commit || box == null) {
            return;
        }
        Object parsed = parseSliderValue(option, box.getValue());
        if (parsed != null) {
            SettingsCatalog.apply(option, parsed);
            preview.restart();
            layoutRows();
        }
    }

    private static Object parseSliderValue(SettingsCatalog.Option option, String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            if (option.current() instanceof Integer) {
                ModConfigSpec.Range<Integer> range = option.spec().getRange();
                int value = (int) Math.round(Double.parseDouble(trimmed));
                if (range != null) {
                    value = Mth.clamp(value, range.getMin(), range.getMax());
                }
                return SettingsCatalog.accepts(option, value) ? value : null;
            }
            ModConfigSpec.Range<Double> range = option.spec().getRange();
            double value = Math.round(Double.parseDouble(trimmed) * 100d) / 100d;
            if (range != null) {
                value = Mth.clamp(value, range.getMin(), range.getMax());
            }
            return SettingsCatalog.accepts(option, value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void toggle(GuiGraphics g, int x, int y, float progress, boolean hovered) {
        float eased = progress * progress * (3f - 2f * progress);
        int off = MenuSkin.mutedColor(0xFF5A5A5A);
        int track = MenuSkin.mixColor(off, accent(), eased);
        if (hovered) {
            track = MenuSkin.mixColor(track, 0xFFFFFFFF, 0.15f);
        }
        g.fill(x, y, x + TOGGLE_W, y + TOGGLE_H, track);
        outline(g, x, y, TOGGLE_W, TOGGLE_H, MenuSkin.mixColor(track, 0xFF000000, 0.35f));
        int knobX = x + 1 + Math.round(eased * (TOGGLE_W - 2 - KNOB_W));
        g.fill(knobX, y + 1, knobX + KNOB_W, y + TOGGLE_H - 1, hovered ? 0xFFFFFFFF : 0xFFF0F0F0);
    }

    private void slider(GuiGraphics g, int x, int y, double fraction, boolean hovered) {
        int trackY = y + SLIDER_H / 2 - 2;
        g.fill(x, trackY, x + SLIDER_W, trackY + 3, MenuSkin.scrollTrackColor(0xFF3A3A3A));
        int filled = (int) Math.round(Mth.clamp(fraction, 0, 1) * (SLIDER_W - 4));
        g.fill(x, trackY, x + filled + 4, trackY + 3, accent());
        int knobX = x + filled;
        g.fill(knobX, y, knobX + 4, y + SLIDER_H, hovered ? 0xFFFFFFFF : 0xFFDCDCDC);
    }

    private int popupTop(int height) {
        int below = choiceY + choiceH + 1;
        if (below + height <= listBottom) {
            return below;
        }
        return Math.max(panelY + 2, choiceY - height - 1);
    }

    private void renderChoicePopup(GuiGraphics g, int mouseX, int mouseY) {
        if (openChoice == null) {
            return;
        }
        Enum<?>[] values = SettingsCatalog.choices(openChoice);
        int height = values.length * CHOICE_H + 2;
        int y = popupTop(height);
        g.fill(choiceX, y, choiceX + choiceW, y + height, MenuSkin.active()
                ? MenuSkin.tint(MenuSkin.palette().opaqueBackground()) : 0xF0100010);
        outline(g, choiceX, y, choiceW, height, glass() ? 0x66FFFFFF : accent());
        for (int i = 0; i < values.length; i++) {
            int rowY = y + 1 + i * CHOICE_H;
            boolean hovered = inside(mouseX, mouseY, choiceX, rowY, choiceW, CHOICE_H);
            boolean current = values[i].equals(openChoice.current());
            if (hovered) {
                g.fill(choiceX + 1, rowY, choiceX + choiceW - 1, rowY + CHOICE_H, MenuSkin.fade(accent(), 0.35f));
            }
            g.drawString(this.font, this.font.plainSubstrByWidth(choiceLabel(values[i]), choiceW - 10),
                    choiceX + 5, rowY + 3, current ? accent() : titleTextColor(), shadow());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sliderEditOption != null && sliderEditBox != null
                && !sliderEditBox.isMouseOver(mouseX, mouseY)) {
            closeSliderEdit(true);
        }
        if (openChoice != null) {
            handleChoiceClick(mouseX, mouseY);
            return true;
        }
        if (page == Page.SETTINGS && bar.mouseClicked(mouseX, mouseY)) {
            setFocused(null);
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        setFocused(null);
        sidebar.layout();

        if (GlassArrow.contains(mouseX, mouseY, GlassArrow.rightX(this.width), GlassArrow.top(this.height))) {
            click();
            onClose();
            return true;
        }

        if (sidebar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        int resetX = contentX + contentW - 8 - RESET_W;
        if (page == Page.SETTINGS && inside(mouseX, mouseY, resetX, panelY + 6, RESET_W, SEARCH_H)) {
            click();
            confirmReset();
            return true;
        }
        if (mouseX >= contentX && mouseX < contentX + contentW && mouseY >= listTop && mouseY < listBottom) {
            return clickRow(mouseX, mouseY);
        }
        return false;
    }

    private void handleChoiceClick(double mouseX, double mouseY) {
        Enum<?>[] values = SettingsCatalog.choices(openChoice);
        int height = values.length * CHOICE_H + 2;
        int y = popupTop(height);
        for (int i = 0; i < values.length; i++) {
            int rowY = y + 1 + i * CHOICE_H;
            if (inside(mouseX, mouseY, choiceX, rowY, choiceW, CHOICE_H)) {
                SettingsCatalog.apply(openChoice, values[i]);
                preview.restart();
                click();
                break;
            }
        }
        openChoice = null;
        layoutRows();
    }

    private void clearSearch() {
        if (query.isEmpty()) {
            return;
        }
        query = "";
        lastQuery = "";
        searchBox.setValue("");
    }

    private boolean clickRow(double mouseX, double mouseY) {
        if (page != Page.SETTINGS) {
            return infoPane.mouseClicked(mouseX, mouseY);
        }
        int offset = bar.displayed();
        for (Row row : rows) {
            int y = listTop + row.y - offset;
            if (mouseY < y || mouseY >= y + row.height) {
                continue;
            }
            if (row.kind == RowKind.PREVIEW) {
                click();
                preview.restart();
                return true;
            }
            if (row.kind != RowKind.CARD) {
                return false;
            }
            return clickControl(row, mouseX, mouseY);
        }
        return false;
    }

    private boolean clickControl(Row row, double mouseX, double mouseY) {
        SettingsCatalog.Option option = row.option;
        int right = row.controlRight;
        int middle = row.controlMiddle;
        switch (option.kind()) {
            case TOGGLE -> {
                int x = right - TOGGLE_W;
                if (inside(mouseX, mouseY, x, middle - TOGGLE_H / 2, TOGGLE_W, TOGGLE_H)) {
                    SettingsCatalog.apply(option, !Boolean.TRUE.equals(option.current()));
                    preview.restart();
                    click();
                    return true;
                }
            }
            case CHOICE -> {
                int width = choiceWidth(option);
                int x = right - width;
                if (inside(mouseX, mouseY, x, middle - CHOICE_H / 2, width, CHOICE_H)) {
                    openChoice = option;
                    choiceX = x;
                    choiceY = middle - CHOICE_H / 2;
                    choiceH = CHOICE_H;
                    choiceW = width;
                    click();
                    return true;
                }
            }
            case SLIDER -> {
                int x = right - SLIDER_W;
                int labelWidth = this.font.width(sliderLabel(option));
                if (hasControlDown()
                        && inside(mouseX, mouseY, x - 6 - labelWidth, middle - BOX_H / 2,
                                SLIDER_W + 6 + labelWidth, BOX_H)) {
                    click();
                    openSliderEdit(option);
                    return true;
                }
                if (inside(mouseX, mouseY, x, middle - SLIDER_H / 2, SLIDER_W, SLIDER_H)) {
                    dragging = option;
                    applySlider(option, (mouseX - x - 2) / (SLIDER_W - 4d), false);
                    return true;
                }
            }
            case LIST -> {
                int x = right - LIST_BUTTON_W;
                if (inside(mouseX, mouseY, x, middle - CHOICE_H / 2, LIST_BUTTON_W, CHOICE_H)) {
                    click();
                    ScreenSwoosh.drill(() -> new SettingsListScreen(this, option), Config.SWOOSH_MENU_STYLE);
                    return true;
                }
            }
            default -> {
                return false;
            }
        }
        return false;
    }

    private Component scopeLabel() {
        if (!query.trim().isEmpty()) {
            return Component.translatable("createaddonorganizer.settings.reset.searchScope");
        }
        return selected >= 0 && selected < categories.size()
                ? categories.get(selected).label()
                : Component.empty();
    }

    private void confirmReset() {
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                resetVisible();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.settings.reset.title"),
                Component.translatable("createaddonorganizer.settings.reset.message", scopeLabel())));
    }

    private void resetVisible() {
        for (Row row : rows) {
            if (row.option != null) {
                SettingsCatalog.stage(row.option, row.option.spec().correct(null));
            }
        }
        Config.save();
        for (ValueBox box : boxes) {
            box.refresh();
        }
        preview.restart();
        layoutRows();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (sidebar.mouseDragged(mouseY)) {
            return true;
        }
        if (page != Page.SETTINGS) {
            return infoPane.mouseDragged(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (bar.mouseDragged(mouseY)) {
            return true;
        }
        if (dragging != null) {
            int right = 0;
            for (Row row : rows) {
                if (row.option == dragging) {
                    right = row.controlRight;
                }
            }
            applySlider(dragging, (mouseX - (right - SLIDER_W) - 2) / (SLIDER_W - 4d), false);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        bar.mouseReleased();
        infoPane.mouseReleased();
        sidebar.mouseReleased();
        if (dragging != null) {
            Config.save();
            dragging = null;
            preview.restart();
            layoutRows();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (openChoice != null) {
            return true;
        }
        if (sidebar.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (page != Page.SETTINGS) {
            return infoPane.mouseScrolled(mouseX, mouseY, scrollY)
                    || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (mouseY >= listTop && mouseY < listBottom && mouseX >= contentX) {
            return bar.wheel(scrollY);
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (openChoice != null && keyCode == 256) {
            openChoice = null;
            return true;
        }
        if (sliderEditOption != null) {
            if (keyCode == 257 || keyCode == 335) {
                closeSliderEdit(true);
                return true;
            }
            if (keyCode == 256) {
                closeSliderEdit(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void magnifier(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 1, y, x + 4, y + 1, color);
        g.fill(x, y + 1, x + 1, y + 4, color);
        g.fill(x + 4, y + 1, x + 5, y + 4, color);
        g.fill(x + 1, y + 4, x + 4, y + 5, color);
        g.fill(x + 4, y + 4, x + 6, y + 6, color);
    }

    private static void chevron(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y, x + 1, y + 1, color);
        g.fill(x + 4, y, x + 5, y + 1, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, color);
        g.fill(x + 3, y + 1, x + 4, y + 2, color);
        g.fill(x + 2, y + 2, x + 3, y + 3, color);
    }

    @Override
    public void onClose() {
        closeSliderEdit(true);
        for (ValueBox box : boxes) {
            box.commit();
        }
        Config.save();
        ScreenSwoosh.push(() -> parent, Config.SWOOSH_BACK);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class Row {
        final RowKind kind;
        final Component header;
        final SettingsCatalog.Option option;
        int color;
        List<FormattedCharSequence> title = List.of();
        List<FormattedCharSequence> description = List.of();
        boolean truncated;
        ValueBox box;
        int y;
        int height;
        int controlRight;
        int controlMiddle;

        private Row(RowKind kind, Component header, SettingsCatalog.Option option) {
            this.kind = kind;
            this.header = header;
            this.option = option;
        }

        static Row header(Component title, int color) {
            Row row = new Row(RowKind.HEADER, title, null);
            row.color = color;
            return row;
        }

        static Row card(SettingsCatalog.Option option) {
            return new Row(RowKind.CARD, null, option);
        }

        static Row preview() {
            return new Row(RowKind.PREVIEW, null, null);
        }
    }

    private final class ValueBox extends EditBox {
        private final SettingsCatalog.Option option;
        private boolean valid = true;
        int frameX;
        int frameY;

        ValueBox(SettingsCatalog.Option option, int width) {
            super(AllSettingsScreen.this.font, 0, 0, width - 8, BOX_H, option.title());
            this.option = option;
            setBordered(false);
            setMaxLength(256);
            setValue(display(option));
            setResponder(text -> valid = parse(text) != null);
        }

        void refresh() {
            setValue(display(option));
            valid = true;
        }

        private String display(SettingsCatalog.Option target) {
            Object current = target.current();
            if (target.kind() == SettingsCatalog.Kind.COLOR && current instanceof Integer value) {
                return String.format("#%08X", value);
            }
            return String.valueOf(current);
        }

        private Object parse(String text) {
            String trimmed = text.trim();
            Object current = option.current();
            try {
                Object parsed;
                if (option.kind() == SettingsCatalog.Kind.COLOR) {
                    String hex = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
                    if (hex.startsWith("0x") || hex.startsWith("0X")) {
                        hex = hex.substring(2);
                    }
                    if (hex.isEmpty() || hex.length() > 8) {
                        return null;
                    }
                    long raw = Long.parseLong(hex, 16);
                    parsed = (int) (hex.length() <= 6 ? raw | 0xFF000000L : raw);
                } else if (current instanceof Integer) {
                    parsed = Integer.decode(trimmed);
                } else if (current instanceof Long) {
                    parsed = Long.decode(trimmed);
                } else if (current instanceof Double) {
                    parsed = Double.parseDouble(trimmed);
                } else {
                    parsed = trimmed;
                }
                return SettingsCatalog.accepts(option, parsed) ? parsed : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        void commit() {
            Object parsed = parse(getValue());
            if (parsed != null) {
                SettingsCatalog.stage(option, parsed);
            } else {
                refresh();
            }
        }

        @Override
        public void setFocused(boolean focused) {
            boolean was = isFocused();
            super.setFocused(focused);
            if (was && !focused) {
                commit();
                Config.save();
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (isFocused() && (keyCode == 257 || keyCode == 335)) {
                commit();
                Config.save();
                setFocused(false);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int width = getWidth() + 8;
            if (vanilla()) {
                g.blitSprite(isFocused() ? VANILLA_FIELD_FOCUS : VANILLA_FIELD, frameX, frameY, width, BOX_H);
                if (!valid) {
                    outline(g, frameX, frameY, width, BOX_H, 0xFFD85B5B);
                }
            } else {
                g.fill(frameX, frameY, frameX + width, frameY + BOX_H, cardColor(isFocused()));
                outline(g, frameX, frameY, width, BOX_H,
                        !valid ? 0xFFD85B5B : hoverLine(isFocused()));
            }
            setTextColor(valid ? titleTextColor() : 0xFFD85B5B);
            super.renderWidget(g, mouseX, mouseY, partialTick);
        }
    }
}
