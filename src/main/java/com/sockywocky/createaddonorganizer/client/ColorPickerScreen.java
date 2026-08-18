package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.IntSupplier;

import com.mojang.blaze3d.platform.NativeImage;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class ColorPickerScreen extends Screen {
    private enum Mode { COLOR, IMAGE }

    private enum EditTarget { BANNER, BOX, TEXT, HIGHLIGHT }

    private enum TextTarget { PRIMARY, SECONDARY, OUTLINE, SHADOW }

    private final Screen parent;
    private final ResourceLocation id;
    private final Component sectionName;

    private Mode mode;
    private EditTarget target = EditTarget.BANNER;
    private TextTarget textEditTarget = TextTarget.PRIMARY;

    private final Hsva bannerHsva;
    private final Hsva bannerHsva2;
    private boolean bannerGradientEnabled;
    private ColorSpec.Direction bannerDirection = ColorSpec.Direction.VERTICAL;
    private ColorSpec.Style bannerStyle = ColorSpec.Style.SMOOTH;
    private final Hsva boxHsva;
    private final Hsva textHsva;
    private final Hsva textHsva2;
    private boolean textGradientEnabled;
    private Hsva text2Hsva;
    private final Hsva text2Hsva2;
    private boolean text2GradientEnabled;
    private boolean twoTone;
    private float twoToneSplit;
    private final Hsva outlineHsva;
    private final Hsva outlineHsva2;
    private boolean outlineGradientEnabled;
    private boolean outlineEnabled;
    private boolean editingStop2;
    private boolean shadowEnabled;
    private boolean shadowUnlinked;
    private final Hsva shadowHsva;
    private float scrollCutoff;

    private final boolean isMainTab;
    private final boolean highlightOnly;
    private boolean hasHighlight;
    private final Hsva highlightHsva;

    private ResourceLocation selectedTexture;
    private String selectedRef;
    private boolean bannerAnimated;
    private int bannerFrameTicks = 2;

    private Mode boxMode;
    private ResourceLocation selectedBoxTexture;
    private String selectedBoxRef;
    private float boxDarken;
    private float boxOpacity;

    private int previewY;
    private int panelTop;
    private static final int PICKER_MAX_W = 300;
    private static final int PICKER_INSET = 24;
    private static final int CONTRAST_PAD = 5;
    private static final int EMBED_PAD = 10;
    private int contentX;
    private int contentW;
    private int controlW = PICKER_MAX_W;
    private int galleryColumns = 1;
    private int contrastY;
    private int contrastH;
    private static final int ROW_H = 20;
    private static final int FULL_ROW_GAP = 6;
    private static final int FULL_SQUARE_H = 100;
    private static final int FULL_BAR_H = 16;
    private static final int REFERENCE_CONTENT_HEIGHT = 236;
    private static final float MIN_LAYOUT_SCALE = 0.5f;
    private int rowGap = FULL_ROW_GAP;
    private int squareHeight = FULL_SQUARE_H;
    private int barHeight = FULL_BAR_H;
    private Component hoverBannerTooltip;
    private GalleryList galleryList;
    private BoxGalleryList boxGalleryList;

    private final GradientTexture svSquareTexture = new GradientTexture("sv_square");
    private final GradientTexture hueBarTexture = new GradientTexture("hue_bar");

    private boolean embedded;
    private int embedX;
    private int embedY;
    private int embedW;
    private int embedH;
    private Runnable onEmbeddedDone;

    public void embedInto(int x, int y, int width, int height, Runnable onDone) {
        this.embedded = true;
        this.embedX = x;
        this.embedY = y;
        this.embedW = width;
        this.embedH = height;
        this.onEmbeddedDone = onDone;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void releaseEmbedded() {
        svSquareTexture.release();
        hueBarTexture.release();
    }

    private int areaTop() {
        return embedded ? embedY : 0;
    }

    private int areaBottom() {
        return embedded ? embedY + embedH : this.height;
    }

    private int areaCenterX() {
        return embedded ? embedX + embedW / 2 : this.width / 2;
    }

    public ColorPickerScreen(Screen parent, ResourceLocation id, Component sectionName, boolean isMainTab) {
        this(parent, id, sectionName, isMainTab, false);
    }

    public ColorPickerScreen(Screen parent, ResourceLocation id, Component sectionName, boolean isMainTab,
            boolean highlightOnly) {
        super(Component.translatable("createaddonorganizer.colors.pick"));
        this.parent = parent;
        this.id = id;
        this.sectionName = sectionName;

        ColorSpec bannerSpec = Config.bannerColorFor(id);
        this.bannerHsva = Hsva.fromArgb(bannerSpec.color1());
        this.bannerGradientEnabled = bannerSpec.isGradient();
        this.bannerHsva2 = Hsva.fromArgb(bannerSpec.isGradient() ? bannerSpec.color2() : bannerSpec.color1());
        this.bannerDirection = bannerSpec.direction();
        this.bannerStyle = bannerSpec.style();

        this.boxHsva = Hsva.fromArgb(Config.boxColorFor(id));

        ColorSpec textSpec = Config.textColorFor(id);
        this.textHsva = Hsva.fromArgb(textSpec.color1());
        this.textGradientEnabled = textSpec.isGradient();
        this.textHsva2 = Hsva.fromArgb(textSpec.isGradient() ? textSpec.color2() : textSpec.color1());

        ColorSpec secondary = Config.textSecondaryColorFor(id);
        this.twoTone = secondary != null;
        ColorSpec secondarySpec = secondary != null ? secondary : ColorSpec.solid(0xFFCEA05A);
        this.text2Hsva = Hsva.fromArgb(secondarySpec.color1());
        this.text2GradientEnabled = secondarySpec.isGradient();
        this.text2Hsva2 = Hsva.fromArgb(secondarySpec.isGradient() ? secondarySpec.color2() : secondarySpec.color1());
        this.twoToneSplit = Config.twoToneSplitFor(id);

        ColorSpec outline = Config.textOutlineColorFor(id);
        this.outlineEnabled = outline != null;
        ColorSpec outlineSpec = outline != null ? outline : Config.defaultTextOutlineSpec();
        this.outlineHsva = Hsva.fromArgb(outlineSpec.color1());
        this.outlineGradientEnabled = outlineSpec.isGradient();
        this.outlineHsva2 = Hsva.fromArgb(outlineSpec.isGradient() ? outlineSpec.color2() : outlineSpec.color1());

        this.shadowEnabled = Config.titleTextShadow(id);
        Integer shadowColor = Config.textShadowColorFor(id);
        this.shadowUnlinked = shadowColor != null;
        this.shadowHsva = Hsva.fromArgb(shadowColor != null ? shadowColor : Config.DEFAULT_TEXT_SHADOW_COLOR.get());
        this.scrollCutoff = Config.scrollCutoffFor(id);

        this.selectedRef = Config.bannerRefFor(id);
        if (selectedRef != null) {
            this.selectedTexture = BannerTextures.resolve(selectedRef);
            this.mode = Mode.IMAGE;
            syncAnimationFields();
        } else {
            this.mode = Mode.COLOR;
        }

        this.selectedBoxRef = Config.boxTextureRefFor(id);
        if (selectedBoxRef != null) {
            this.selectedBoxTexture = BoxTextures.resolve(selectedBoxRef);
            this.boxMode = Mode.IMAGE;
        } else {
            this.boxMode = Mode.COLOR;
        }
        this.boxDarken = Config.boxDarkenFor(id);
        this.boxOpacity = Config.boxOpacityFor(id);

        this.isMainTab = isMainTab;
        this.highlightOnly = highlightOnly || SimulatedSupport.isMainTab(id);
        if (highlightOnly) {
            this.target = EditTarget.HIGHLIGHT;
        }
        Integer highlightOverride = Config.highlightColorFor(id);
        this.hasHighlight = highlightOverride != null;
        this.highlightHsva = Hsva.fromArgb(highlightOverride != null ? highlightOverride : 0xFF4A90D9);
    }

    private void syncAnimationFields() {
        Integer declared = selectedTexture != null ? Config.animatedFrameTicks(selectedTexture) : null;
        this.bannerAnimated = selectedTexture != null && BannerAnimation.isAnimatable(selectedTexture);
        this.bannerFrameTicks = declared != null ? declared : 2;
    }

    @Override
    protected void init() {
        computeRegions();
        int doneY = areaBottom() - MenuLayout.ROW_H - 4;
        this.panelTop = areaTop() + MenuLayout.ROW_1;
        this.contrastH = 0;

        if (highlightOnly) {
            initHighlightPanel();
            int helpW = MenuLayout.ROW_H;
            int pair = MenuLayout.split(contentW - helpW - MenuLayout.GAP, 2);
            GlassButton help = new GlassButton(contentX, doneY, helpW, MenuLayout.ROW_H,
                    Component.literal("?"), b -> {});
            help.setTooltip(Tooltip.create(
                    Component.translatable("createaddonorganizer.colors.simulatedUneditable")));
            addRenderableWidget(help);
            addRenderableWidget(new GlassButton(contentX + helpW + MenuLayout.GAP, doneY, pair, MenuLayout.ROW_H,
                    Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose()));
            addRenderableWidget(new GlassButton(contentX + contentW - pair, doneY, pair, MenuLayout.ROW_H,
                    Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                    .style(GlassButton.Style.ACCENT));
            return;
        }

        boolean previewTop = Config.bannerEditorPreviewTop();
        this.previewY = previewTop ? areaTop() + MenuLayout.DESC_Y : areaBottom() - 52;
        this.panelTop = areaTop() + (previewTop
                ? MenuLayout.DESC_Y + BannerTextures.HEIGHT + 8
                : MenuLayout.DESC_Y);
        int buttonsY = doneY - MenuLayout.GAP;
        int contentBottom = previewTop ? buttonsY : Math.min(buttonsY, this.previewY - 8);
        computeLayoutScale(contentBottom - this.panelTop);

        switch (target) {
            case BANNER -> initBannerPanel();
            case BOX -> initBoxPanel();
            case TEXT -> initTextPanel();
            case HIGHLIGHT -> initHighlightPanel();
        }

        if (DevMode.isUnlocked()) {
            GlassButton screenshotButton = addRenderableWidget(new GlassButton(6, 6, 100, 20,
                    Component.translatable("createaddonorganizer.banner.screenshot"),
                    b -> captureScreenshot()));
            screenshotButton.active = hasBanner();

            addRenderableWidget(new GlassButton(6, 28, 130, 20,
                    Component.translatable("createaddonorganizer.banner.screenshot.openFolder"),
                    b -> BannerScreenshot.openFolder()));
        }

        int parts = isMainTab ? 3 : 2;
        int each = MenuLayout.split(contentW, parts);
        int slot = 0;
        if (isMainTab) {
            boolean editingHighlight = target == EditTarget.HIGHLIGHT;
            addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot++), doneY,
                    each, MenuLayout.ROW_H,
                    Component.translatable(editingHighlight
                            ? "createaddonorganizer.colors.target.banner"
                            : "createaddonorganizer.colors.target.highlight"),
                    b -> {
                        target = editingHighlight ? EditTarget.BANNER : EditTarget.HIGHLIGHT;
                        editingStop2 = false;
                        rebuildWidgets();
                    }));
        }
        addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot++), doneY,
                each, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose()));
        addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot), doneY,
                each, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                .style(GlassButton.Style.ACCENT));
    }

    private void initHighlightPanel() {
        int x = columnX();
        addRenderableWidget(new GlassToggle(x, panelTop,
                Component.translatable("createaddonorganizer.colors.highlight.enabled"), hasHighlight,
                checked -> {
                    hasHighlight = checked;
                    rebuildWidgets();
                }));
        if (hasHighlight) {
            addColorControls(x, panelTop + 30, highlightHsva);
        }
    }

    private void computeRegions() {
        contentW = embedded ? Math.max(120, embedW - EMBED_PAD * 2) : MenuLayout.panelWidth(this.width);
        contentX = embedded ? embedX + EMBED_PAD : MenuLayout.panelX(this.width);
        controlW = Math.max(120, Math.min(PICKER_MAX_W, contentW - PICKER_INSET * 2));
        galleryColumns = BannerGrid.columnsFor(contentW);
    }

    private int columnX() {
        return contentX + Math.max(0, (contentW - controlW) / 2);
    }

    private int previewX() {
        return contentX + Math.max(0, (contentW - BannerTextures.WIDTH) / 2);
    }

    private int listRegionW() {
        return contentW;
    }

    private void computeLayoutScale(int available) {
        float scale = available >= REFERENCE_CONTENT_HEIGHT
                ? 1.0f
                : Math.max(MIN_LAYOUT_SCALE, available / (float) REFERENCE_CONTENT_HEIGHT);
        rowGap = Math.max(2, Math.round(FULL_ROW_GAP * scale));
        squareHeight = Math.max(50, Math.round(FULL_SQUARE_H * scale));
        barHeight = Math.max(8, Math.round(FULL_BAR_H * scale));
    }

    private record GradientSwitch(boolean enabled, Runnable toggle) {}

    private int initTopRow(int y, Component toggleLabel, Runnable onToggle, Runnable onToggleBackward,
            GradientSwitch gradient, Runnable onReset) {
        int parts = 1 + (toggleLabel != null ? 1 : 0) + (gradient != null ? 1 : 0);
        int each = MenuLayout.split(contentW, parts);
        int slot = 0;
        if (toggleLabel != null) {
            addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot++), y,
                    each, ROW_H, toggleLabel, onToggle, onToggleBackward));
        }
        if (gradient != null) {
            addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot++), y,
                    each, ROW_H, gradientToggleLabel(gradient.enabled()),
                    () -> { gradient.toggle().run(); rebuildWidgets(); },
                    () -> { gradient.toggle().run(); rebuildWidgets(); }));
        }
        GlassButton reset = new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot), y, each, ROW_H,
                Component.translatable(parts == 1
                        ? "createaddonorganizer.colors.reset"
                        : "createaddonorganizer.colors.resetShort"), b -> onReset.run());
        if (parts > 1) {
            reset.setTooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.reset")));
        }
        addRenderableWidget(reset);
        return y + ROW_H + Math.max(rowGap, MenuLayout.GAP + CONTRAST_PAD);
    }

    private void initBannerPanel() {
        if (mode == Mode.COLOR) {
            int x = columnX();
            int contentY = initTopRow(panelTop, modeLabel(mode), this::toggleBannerMode, this::toggleBannerMode,
                    new GradientSwitch(bannerGradientEnabled, () -> bannerGradientEnabled = !bannerGradientEnabled),
                    this::resetBanner);
            addGradientControls(x, contentY, bannerGradientEnabled, bannerHsva, bannerHsva2,
                    new BannerGradientExtras(bannerDirection, d -> bannerDirection = d, bannerStyle, s -> bannerStyle = s));
        } else {
            boolean isUpload = selectedRef != null && selectedRef.startsWith("file:");
            int parts = isUpload ? 4 : 3;
            int each = MenuLayout.split(contentW, parts);
            int slot = 0;
            addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot++), panelTop,
                    each, ROW_H, modeLabel(mode), this::toggleBannerMode, this::toggleBannerMode));
            addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot++), panelTop,
                    each, ROW_H, Component.translatable("createaddonorganizer.banner.upload"), b -> upload()));
            if (isUpload) {
                addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot++), panelTop,
                        each, ROW_H, Component.translatable("createaddonorganizer.banner.delete"),
                        b -> confirmDelete()).style(GlassButton.Style.DANGER));
            }
            GlassButton resetBannerButton = new GlassButton(
                    MenuLayout.splitX(contentX, contentW, parts, slot), panelTop, each, ROW_H,
                    Component.translatable("createaddonorganizer.colors.resetShort"), b -> resetBanner());
            resetBannerButton.setTooltip(Tooltip.create(
                    Component.translatable("createaddonorganizer.colors.reset")));
            addRenderableWidget(resetBannerButton);

            boolean canAnimate = selectedTexture != null && BannerAnimation.isAnimatable(selectedTexture);
            List<String> pool = BannerEditor.poolFor(id);
            boolean hasPool = !pool.isEmpty();
            int optionsY = panelTop + ROW_H + MenuLayout.GAP;
            int optionX = contentX;

            if (canAnimate) {
                GlassToggle animated = new GlassToggle(optionX, optionsY,
                        Component.translatable("createaddonorganizer.banner.animated"), bannerAnimated,
                        checked -> {
                            bannerAnimated = checked;
                            rebuildWidgets();
                        });
                addRenderableWidget(animated);
                optionX += animated.getWidth() + MenuLayout.GAP * 2;
                if (bannerAnimated) {
                    addRenderableWidget(new ChannelSlider(optionX, optionsY, 96, ROW_H,
                            Component.translatable("createaddonorganizer.banner.speed"),
                            (bannerFrameTicks - 1) / 9.0,
                            v -> bannerFrameTicks = 1 + (int) Math.round(v * 9),
                            v -> (1 + (int) Math.round(v * 9)) + "t"));
                    optionX += 96 + MenuLayout.GAP * 2;
                }
            }
            if (hasPool) {
                GlassToggle showAll = new GlassToggle(optionX, optionsY,
                        Component.translatable("createaddonorganizer.banner.showAll"), Config.showAllBanners(),
                        checked -> {
                            Config.setShowAllBanners(checked);
                            rebuildWidgets();
                        });
                if (showAll.getX() + showAll.getWidth() > contentX + contentW) {
                    showAll.setX(contentX + contentW - showAll.getWidth());
                }
                addRenderableWidget(showAll);
            }

            int listTop = canAnimate || hasPool ? optionsY + ROW_H + MenuLayout.GAP : optionsY;
            int listBottom = Config.bannerEditorPreviewTop()
                    ? areaBottom() - MenuLayout.ROW_H - 4 - MenuLayout.GAP
                    : previewY - 8;
            double restoreScroll = galleryList != null ? galleryList.getScrollAmount() : 0;
            galleryList = new GalleryList(this.minecraft, listRegionW(), listBottom - listTop, listTop,
                    BannerTextures.HEIGHT + 6);
            galleryList.setX(contentX);
            boolean restrict = hasPool && !Config.showAllBanners()
                    && !(DevMode.isUnlocked() && Config.devAllBannerPools());
            Collection<String> refs;
            if (restrict) {
                LinkedHashSet<String> merged = new LinkedHashSet<>(pool);
                merged.addAll(Config.extraPoolFor(id));
                refs = merged;
            } else {
                refs = BannerTextures.gallery();
            }
            List<String> drawable = new ArrayList<>();
            for (String ref : refs) {
                if (BannerTextures.resolve(ref) != null) {
                    drawable.add(ref);
                }
            }
            galleryList.setRefs(drawable);
            addRenderableWidget(galleryList);
            galleryList.setScrollAmount(restoreScroll);
        }
    }

    private void toggleBannerMode() {
        mode = (mode == Mode.COLOR) ? Mode.IMAGE : Mode.COLOR;
        rebuildWidgets();
    }

    private void toggleBoxMode() {
        boxMode = (boxMode == Mode.COLOR) ? Mode.IMAGE : Mode.COLOR;
        rebuildWidgets();
    }

    private void initBoxPanel() {
        if (boxMode != Mode.COLOR) {
            initBoxImagePanel();
            return;
        }
        int x = columnX();
        int contentY = initTopRow(panelTop, modeLabel(boxMode), this::toggleBoxMode, this::toggleBoxMode, null,
                this::resetBox);

        int y = addColorControls(x, contentY, boxHsva);
        addTintedBoxCheckbox(x, y + 4);
        addRenderableWidget(new ChannelSlider(x + 96, y + 3, controlW - 96, 20,
                Component.translatable("createaddonorganizer.colors.alpha"),
                boxHsva.a, v -> boxHsva.a = (float) v));
    }

    private void initBoxImagePanel() {
        boolean isUpload = selectedBoxRef != null && selectedBoxRef.startsWith("file:");
        int parts = isUpload ? 4 : 3;
        int each = MenuLayout.split(contentW, parts);
        int slot = 0;
        addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot++), panelTop,
                each, ROW_H, modeLabel(boxMode), this::toggleBoxMode, this::toggleBoxMode));
        addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot++), panelTop,
                each, ROW_H, Component.translatable("createaddonorganizer.banner.upload"), b -> uploadBox()));
        if (isUpload) {
            addRenderableWidget(new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot++), panelTop,
                    each, ROW_H, Component.translatable("createaddonorganizer.banner.delete"),
                    b -> confirmDeleteBox()).style(GlassButton.Style.DANGER));
        }
        GlassButton resetBoxButton = new GlassButton(MenuLayout.splitX(contentX, contentW, parts, slot), panelTop,
                each, ROW_H, Component.translatable("createaddonorganizer.colors.resetShort"), b -> resetBox());
        resetBoxButton.setTooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.reset")));
        addRenderableWidget(resetBoxButton);

        int optionsY = panelTop + ROW_H + MenuLayout.GAP;
        addTintedBoxCheckbox(contentX, optionsY + 1);
        int checkboxW = 54;
        int sliderW = (contentW - checkboxW - MenuLayout.GAP) / 2;
        addRenderableWidget(new ChannelSlider(contentX + checkboxW, optionsY, sliderW, ROW_H,
                Component.translatable("createaddonorganizer.colors.boxDarken"),
                boxDarken, v -> boxDarken = (float) v));
        addRenderableWidget(new ChannelSlider(contentX + contentW - sliderW, optionsY, sliderW, ROW_H,
                Component.translatable("createaddonorganizer.colors.boxOpacity"),
                boxOpacity, v -> boxOpacity = (float) v));

        int listTop = optionsY + ROW_H + MenuLayout.GAP;
        int listBottom = Config.bannerEditorPreviewTop()
                ? areaBottom() - MenuLayout.ROW_H - 4 - MenuLayout.GAP
                : previewY - 8;
        double restoreScroll = boxGalleryList != null ? boxGalleryList.getScrollAmount() : 0;
        boxGalleryList = new BoxGalleryList(this.minecraft, listRegionW(), listBottom - listTop, listTop,
                BoxTextures.HEIGHT + 10);
        boxGalleryList.setX(contentX);
        boxGalleryList.setRefs(BoxTextures.gallery());
        addRenderableWidget(boxGalleryList);
        boxGalleryList.setScrollAmount(restoreScroll);
    }

    private void addTintedBoxCheckbox(int x, int y) {
        addRenderableWidget(new GlassToggle(x, y,
                Component.translatable("createaddonorganizer.colors.tintedBox"), Config.tintedTextBoxFor(id),
                checked -> Config.setTintedTextBoxFor(id, checked)));
    }

    private void initTextPanel() {
        int x = columnX();
        GradientSwitch gradient = switch (textEditTarget) {
            case PRIMARY -> new GradientSwitch(textGradientEnabled, () -> textGradientEnabled = !textGradientEnabled);
            case SECONDARY -> new GradientSwitch(text2GradientEnabled, () -> text2GradientEnabled = !text2GradientEnabled);
            case OUTLINE -> new GradientSwitch(outlineGradientEnabled, () -> outlineGradientEnabled = !outlineGradientEnabled);
            case SHADOW -> null;
        };
        int contentY = initTopRow(panelTop, textTargetLabel(), this::cycleTextTarget, this::cycleTextTargetBackward,
                gradient, this::resetActiveTextTarget);

        switch (textEditTarget) {
            case PRIMARY -> {
                int y = addGradientControls(x, contentY, textGradientEnabled, textHsva, textHsva2, null);
                addRenderableWidget(new ChannelSlider(x, y + 4, controlW, 20,
                        Component.translatable("createaddonorganizer.colors.scrollCutoff"),
                        scrollCutoff, v -> scrollCutoff = (float) v));
            }
            case SECONDARY -> {
                int y = addGradientControls(x, contentY, text2GradientEnabled, text2Hsva, text2Hsva2, null);
                addRenderableWidget(new GlassToggle(x, y + 4,
                        Component.translatable("createaddonorganizer.colors.twoTone"), twoTone,
                        checked -> twoTone = checked));
                addRenderableWidget(new ChannelSlider(x + 96, y + 3, controlW - 96, 20,
                        Component.translatable("createaddonorganizer.colors.twoToneSplit"),
                        twoToneSplit, v -> twoToneSplit = (float) v));
            }
            case OUTLINE -> {
                int y = addGradientControls(x, contentY, outlineGradientEnabled, outlineHsva, outlineHsva2, null);
                addRenderableWidget(new GlassToggle(x, y + 4,
                        Component.translatable("createaddonorganizer.colors.outline"), outlineEnabled,
                        checked -> outlineEnabled = checked));
            }
            case SHADOW -> {
                addRenderableWidget(new GlassToggle(x, contentY,
                        Component.translatable("createaddonorganizer.colors.shadow"), shadowEnabled,
                        checked -> shadowEnabled = checked));
                addRenderableWidget(new GlassToggle(x, contentY + 24,
                        Component.translatable("createaddonorganizer.colors.shadowUnlinked"), shadowUnlinked,
                        checked -> {
                            shadowUnlinked = checked;
                            rebuildWidgets();
                        }));
                if (shadowUnlinked) {
                    addColorControls(x, contentY + 50, shadowHsva);
                }
            }
        }
    }

    private void cycleTextTarget() {
        textEditTarget = switch (textEditTarget) {
            case PRIMARY -> TextTarget.SECONDARY;
            case SECONDARY -> TextTarget.OUTLINE;
            case OUTLINE -> TextTarget.SHADOW;
            case SHADOW -> TextTarget.PRIMARY;
        };
        editingStop2 = false;
        rebuildWidgets();
    }

    private void cycleTextTargetBackward() {
        textEditTarget = switch (textEditTarget) {
            case PRIMARY -> TextTarget.SHADOW;
            case SECONDARY -> TextTarget.PRIMARY;
            case OUTLINE -> TextTarget.SECONDARY;
            case SHADOW -> TextTarget.OUTLINE;
        };
        editingStop2 = false;
        rebuildWidgets();
    }

    private void resetActiveTextTarget() {
        switch (textEditTarget) {
            case PRIMARY -> {
                resetGradientTarget(Config.defaultTextSpec(), textHsva, textHsva2, v -> textGradientEnabled = v);
                scrollCutoff = Config.DEFAULT_SCROLL_CUTOFF.get().floatValue();
            }
            case SECONDARY -> resetGradientTarget(Config.defaultTextSecondarySpec(), text2Hsva, text2Hsva2,
                    v -> text2GradientEnabled = v);
            case OUTLINE -> resetGradientTarget(Config.defaultTextOutlineSpec(), outlineHsva, outlineHsva2,
                    v -> outlineGradientEnabled = v);
            case SHADOW -> {
                shadowEnabled = Config.TITLE_TEXT_SHADOW.get();
                shadowUnlinked = false;
                resetHsva(shadowHsva, Config.DEFAULT_TEXT_SHADOW_COLOR.get());
            }
        }
        editingStop2 = false;
        rebuildWidgets();
    }

    private static void resetGradientTarget(ColorSpec def, Hsva stop1, Hsva stop2, Consumer<Boolean> setGradientEnabled) {
        resetHsva(stop1, def.color1());
        resetHsva(stop2, def.isGradient() ? def.color2() : def.color1());
        setGradientEnabled.accept(def.isGradient());
    }

    private static void resetHsva(Hsva target, int argb) {
        Hsva def = Hsva.fromArgb(argb);
        target.h = def.h;
        target.s = def.s;
        target.v = def.v;
    }

    private Component textTargetLabel() {
        String key = switch (textEditTarget) {
            case PRIMARY -> "createaddonorganizer.colors.text.primary";
            case SECONDARY -> "createaddonorganizer.colors.text.secondary";
            case OUTLINE -> "createaddonorganizer.colors.text.outline";
            case SHADOW -> "createaddonorganizer.colors.text.shadow";
        };
        return Component.translatable("createaddonorganizer.colors.text.editing").copy().append(": ").append(Component.translatable(key));
    }

    private record BannerGradientExtras(ColorSpec.Direction direction, Consumer<ColorSpec.Direction> setDirection,
            ColorSpec.Style style, Consumer<ColorSpec.Style> setStyle) {}

    private int addGradientControls(int x, int y, boolean gradientEnabled,
            Hsva stop1, Hsva stop2, BannerGradientExtras extras) {
        if (!gradientEnabled) {
            return addColorControls(x, y, stop1);
        }

        int parts = extras != null ? 3 : 1;
        int each = MenuLayout.split(contentW, parts);
        int slot = 0;
        addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot++), y,
                each, ROW_H, stopLabel(),
                () -> { editingStop2 = !editingStop2; rebuildWidgets(); },
                () -> { editingStop2 = !editingStop2; rebuildWidgets(); }));
        if (extras != null) {
            ColorSpec.Direction direction = extras.direction();
            addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot++), y,
                    each, ROW_H, directionLabel(direction),
                    () -> { extras.setDirection().accept(nextDirection(direction)); rebuildWidgets(); },
                    () -> { extras.setDirection().accept(prevDirection(direction)); rebuildWidgets(); }));
            ColorSpec.Style style = extras.style();
            addRenderableWidget(new CycleActionButton(MenuLayout.splitX(contentX, contentW, parts, slot), y,
                    each, ROW_H, styleLabel(style),
                    () -> { extras.setStyle().accept(nextStyle(style)); rebuildWidgets(); },
                    () -> { extras.setStyle().accept(prevStyle(style)); rebuildWidgets(); }));
        }
        y += ROW_H + rowGap;

        return addColorControls(x, y, editingStop2 ? stop2 : stop1);
    }

    private static ColorSpec.Direction nextDirection(ColorSpec.Direction dir) {
        return switch (dir) {
            case VERTICAL -> ColorSpec.Direction.HORIZONTAL;
            case HORIZONTAL -> ColorSpec.Direction.DIAGONAL_UP;
            case DIAGONAL_UP -> ColorSpec.Direction.DIAGONAL_DOWN;
            case DIAGONAL_DOWN -> ColorSpec.Direction.VERTICAL;
        };
    }

    private static ColorSpec.Direction prevDirection(ColorSpec.Direction dir) {
        return switch (dir) {
            case VERTICAL -> ColorSpec.Direction.DIAGONAL_DOWN;
            case HORIZONTAL -> ColorSpec.Direction.VERTICAL;
            case DIAGONAL_UP -> ColorSpec.Direction.HORIZONTAL;
            case DIAGONAL_DOWN -> ColorSpec.Direction.DIAGONAL_UP;
        };
    }

    private static ColorSpec.Style nextStyle(ColorSpec.Style style) {
        return switch (style) {
            case SMOOTH -> ColorSpec.Style.DITHER_2X2;
            case DITHER_2X2 -> ColorSpec.Style.DITHER_4X4;
            case DITHER_4X4 -> ColorSpec.Style.DITHER_TRICOLOR;
            case DITHER_TRICOLOR -> ColorSpec.Style.DITHER_QUADCOLOR;
            case DITHER_QUADCOLOR -> ColorSpec.Style.SMOOTH;
        };
    }

    private static ColorSpec.Style prevStyle(ColorSpec.Style style) {
        return switch (style) {
            case SMOOTH -> ColorSpec.Style.DITHER_QUADCOLOR;
            case DITHER_2X2 -> ColorSpec.Style.SMOOTH;
            case DITHER_4X4 -> ColorSpec.Style.DITHER_2X2;
            case DITHER_TRICOLOR -> ColorSpec.Style.DITHER_4X4;
            case DITHER_QUADCOLOR -> ColorSpec.Style.DITHER_TRICOLOR;
        };
    }

    private Component gradientToggleLabel(boolean enabled) {
        String key = enabled ? "createaddonorganizer.colors.gradient.gradient" : "createaddonorganizer.colors.gradient.solid";
        return Component.translatable("createaddonorganizer.colors.gradient.mode").copy().append(": ").append(Component.translatable(key));
    }

    private Component stopLabel() {
        String key = editingStop2 ? "createaddonorganizer.colors.gradient.stop2" : "createaddonorganizer.colors.gradient.stop1";
        return Component.translatable("createaddonorganizer.colors.gradient.editing").copy().append(": ").append(Component.translatable(key));
    }

    private Component directionLabel(ColorSpec.Direction dir) {
        String key = switch (dir) {
            case VERTICAL -> "createaddonorganizer.colors.gradient.direction.vertical";
            case HORIZONTAL -> "createaddonorganizer.colors.gradient.direction.horizontal";
            case DIAGONAL_UP -> "createaddonorganizer.colors.gradient.direction.diagonalUp";
            case DIAGONAL_DOWN -> "createaddonorganizer.colors.gradient.direction.diagonalDown";
        };
        return Component.translatable("createaddonorganizer.colors.gradient.direction").copy().append(": ").append(Component.translatable(key));
    }

    private Component styleLabel(ColorSpec.Style style) {
        String key = switch (style) {
            case SMOOTH -> "createaddonorganizer.colors.gradient.style.smooth";
            case DITHER_2X2 -> "createaddonorganizer.colors.gradient.style.dither2x2";
            case DITHER_4X4 -> "createaddonorganizer.colors.gradient.style.dither4x4";
            case DITHER_TRICOLOR -> "createaddonorganizer.colors.gradient.style.dithertricolor";
            case DITHER_QUADCOLOR -> "createaddonorganizer.colors.gradient.style.ditherquadcolor";
        };
        return Component.translatable("createaddonorganizer.colors.gradient.style").copy().append(": ").append(Component.translatable(key));
    }

    private int addColorControls(int x, int y, Hsva target) {
        int width = controlW;

        int barY = y + squareHeight + rowGap;
        int hexY = barY + barHeight + rowGap;
        contrastY = y;
        contrastH = hexY + 20 - y;

        EditBox hexBox = new EditBox(this.font, x, hexY, width - 50, 20, Component.empty());
        hexBox.setMaxLength(7);
        hexBox.setValue(hex6(target));

        boolean[] refreshing = {false};
        hexBox.setResponder(text -> {
            if (!refreshing[0]) {
                applyHex(text, target, null);
            }
        });
        Runnable refreshHexText = () -> {
            refreshing[0] = true;
            hexBox.setValue(hex6(target));
            refreshing[0] = false;
        };

        IntSupplier cellSize = Config::gradientCellSize;
        addRenderableWidget(new SvSquare(x, y, width, squareHeight, target, refreshHexText, cellSize, svSquareTexture));
        addRenderableWidget(new HueBar(x, barY, width, barHeight, target, refreshHexText, cellSize, hueBarTexture));
        addRenderableWidget(hexBox);
        addRenderableWidget(new GlassButton(x + width - 44, hexY, 44, 20,
                Component.translatable("createaddonorganizer.colors.copy"),
                b -> this.minecraft.keyboardHandler.setClipboard(hexBox.getValue())));

        return hexY + 20;
    }

    private static String hex6(Hsva hsva) {
        return String.format(Locale.ROOT, "#%06X", ColorUtil.hsvToRgb(hsva.h, hsva.s, hsva.v));
    }

    private static void applyHex(String raw, Hsva target, Runnable afterChange) {
        String s = raw.startsWith("#") ? raw.substring(1) : raw;
        if (s.length() != 6) {
            return;
        }
        try {
            int rgb = Integer.parseInt(s, 16);
            float[] hsv = ColorUtil.rgbToHsv(0xFF000000 | rgb);
            target.h = hsv[0];
            target.s = hsv[1];
            target.v = hsv[2];
            if (afterChange != null) {
                afterChange.run();
            }
        } catch (NumberFormatException ignored) {

        }
    }

    private void resetBanner() {
        ColorSpec def = Config.defaultBannerSpec();
        Hsva stop1 = Hsva.fromArgb(def.color1());
        bannerHsva.h = stop1.h;
        bannerHsva.s = stop1.s;
        bannerHsva.v = stop1.v;
        bannerHsva.a = stop1.a;
        resetHsva(bannerHsva2, def.isGradient() ? def.color2() : def.color1());
        bannerGradientEnabled = def.isGradient();
        bannerDirection = def.direction();
        bannerStyle = def.style();
        editingStop2 = false;
        mode = Mode.COLOR;
        selectedRef = null;
        selectedTexture = null;
        syncAnimationFields();
        rebuildWidgets();
    }

    private void resetBox() {
        Hsva def = Hsva.fromArgb(Config.DEFAULT_BOX_COLOR.get());
        boxHsva.h = def.h;
        boxHsva.s = def.s;
        boxHsva.v = def.v;
        boxHsva.a = def.a;
        boxMode = Mode.COLOR;
        selectedBoxRef = null;
        selectedBoxTexture = null;
        boxDarken = Config.DEFAULT_BOX_DARKEN.get().floatValue();
        boxOpacity = Config.DEFAULT_BOX_OPACITY.get().floatValue();
        Config.clearTintedTextBoxFor(id);
        rebuildWidgets();
    }

    private Component modeLabel(Mode m) {
        String key = m == Mode.COLOR
                ? "createaddonorganizer.banner.mode.color"
                : "createaddonorganizer.banner.mode.image";
        return Component.translatable("createaddonorganizer.banner.mode").copy().append(": ").append(Component.translatable(key));
    }

    private void uploadBox() {
        Optional<Path> chosen = BoxTextures.chooseFile();
        if (chosen.isEmpty()) {
            return;
        }
        try {
            String ref = BoxTextures.importFile(chosen.get());
            ResourceLocation tex = BoxTextures.resolve(ref);
            if (tex != null) {
                this.selectedBoxRef = ref;
                this.selectedBoxTexture = tex;
                rebuildWidgets();
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to import box texture image", e);
        }
    }

    private void confirmDeleteBox() {
        String ref = this.selectedBoxRef;
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                BoxTextures.deleteFile(ref);
                this.selectedBoxRef = null;
                this.selectedBoxTexture = null;
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.box.delete.title"),
                Component.translatable("createaddonorganizer.box.delete.message", ref)));
    }

    private Component currentTitle() {
        String key = switch (target) {
            case BANNER -> "createaddonorganizer.colors.target.banner";
            case BOX -> "createaddonorganizer.colors.target.box";
            case TEXT -> "createaddonorganizer.colors.target.text";
            case HIGHLIGHT -> "createaddonorganizer.colors.target.highlight";
        };
        return this.title.copy().append(" — ").append(Component.translatable(key));
    }

    private void upload() {
        Optional<Path> chosen = BannerTextures.chooseFile();
        if (chosen.isEmpty()) {
            return;
        }
        try {
            String ref = BannerTextures.importFile(chosen.get());
            ResourceLocation tex = BannerTextures.resolve(ref);
            if (tex != null) {
                this.selectedRef = ref;
                this.selectedTexture = tex;
                Config.addExtraPoolEntry(id, ref);
                syncAnimationFields();
                rebuildWidgets();
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to import banner image", e);
        }
    }

    private void confirmDelete() {
        String ref = this.selectedRef;
        ResourceLocation texture = this.selectedTexture;
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                BannerTextures.deleteFile(ref);
                if (texture != null) {
                    Config.clearAnimatedBanner(texture);
                    BannerAnimation.invalidate(texture);
                }
                Config.removeExtraPoolEntriesForRef(ref);
                this.selectedRef = null;
                this.selectedTexture = null;
                syncAnimationFields();
            }
            this.minecraft.setScreen(this);
        }, Component.translatable("createaddonorganizer.banner.delete.title"),
                Component.translatable("createaddonorganizer.banner.delete.message", ref)));
    }

    private static ColorSpec buildSpec(Hsva stop1, boolean gradientEnabled, Hsva stop2, ColorSpec.Direction direction,
            ColorSpec.Style style) {
        if (!gradientEnabled) {
            return ColorSpec.solid(stop1.toArgb());
        }
        return new ColorSpec(stop1.toArgb(), stop2.toArgb(), direction, style);
    }

    private void confirm() {
        if (highlightOnly) {
            if (hasHighlight) {
                Config.setHighlightColor(id, highlightHsva.toArgb());
            } else {
                Config.clearHighlightColor(id);
            }
            onClose();
            return;
        }

        if (mode == Mode.COLOR) {
            ColorSpec bannerSpec = buildSpec(bannerHsva, bannerGradientEnabled, bannerHsva2, bannerDirection, bannerStyle);
            Config.clearSectionBanner(id);
            Config.setSectionColor(id, bannerSpec);
            LiveColors.apply(id, bannerSpec);
        } else if (selectedRef != null && selectedTexture != null) {
            Config.setSectionBanner(id, selectedRef);
            LiveColors.applyTexture(id, selectedTexture);
            if (BannerAnimation.isAnimatable(selectedTexture)) {
                if (bannerAnimated) {
                    Config.setAnimatedBanner(selectedTexture, bannerFrameTicks);
                } else {
                    Config.clearAnimatedBanner(selectedTexture);
                }
                BannerAnimation.invalidate(selectedTexture);
            }
        }

        ColorSpec textSpec = buildSpec(textHsva, textGradientEnabled, textHsva2, ColorSpec.Direction.HORIZONTAL, ColorSpec.Style.SMOOTH);
        Config.setTextColor(id, textSpec);
        LiveColors.applyTextColor(id, textSpec);
        Config.setScrollCutoff(id, scrollCutoff);

        if (boxMode == Mode.COLOR) {
            Config.clearSectionBoxTexture(id);
            Config.setBoxColor(id, boxHsva.toArgb());
        } else if (selectedBoxRef != null && selectedBoxTexture != null) {
            Config.setSectionBoxTexture(id, selectedBoxRef);
        }
        Config.setBoxDarken(id, boxDarken);
        Config.setBoxOpacity(id, boxOpacity);

        if (twoTone) {
            ColorSpec secondarySpec = buildSpec(text2Hsva, text2GradientEnabled, text2Hsva2, ColorSpec.Direction.HORIZONTAL, ColorSpec.Style.SMOOTH);
            Config.setTextSecondaryColor(id, secondarySpec);
            Config.setTwoToneSplit(id, twoToneSplit);
        } else {
            Config.clearTextSecondaryColor(id);
            Config.clearTwoToneSplit(id);
        }

        if (outlineEnabled) {
            ColorSpec outlineSpec = buildSpec(outlineHsva, outlineGradientEnabled, outlineHsva2, ColorSpec.Direction.HORIZONTAL, ColorSpec.Style.SMOOTH);
            Config.setTextOutlineColor(id, outlineSpec);
        } else {
            Config.clearTextOutlineColor(id);
        }
        Config.setTitleTextShadow(id, shadowEnabled);
        if (shadowUnlinked) {
            Config.setTextShadowColor(id, shadowHsva.toArgb());
        } else {
            Config.clearTextShadowColor(id);
        }

        if (isMainTab) {
            if (hasHighlight) {
                Config.setHighlightColor(id, highlightHsva.toArgb());
            } else {
                Config.clearHighlightColor(id);
            }
        }

        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (!highlightOnly && button == 0) {
            EditTarget hit = hitTest((int) mouseX, (int) mouseY);
            if (hit != null && !Config.editorHintSeen()) {

                Config.setEditorHintSeen(true);
            }
            if (hit != null && hit != target) {
                target = hit;
                editingStop2 = false;
                rebuildWidgets();
                return true;
            }
        }
        return false;
    }

    private EditTarget hitTest(int mx, int my) {
        int bx = previewX();
        int by = previewY;
        boolean hasBanner = mode == Mode.COLOR || selectedTexture != null;
        if (hasBanner) {
            int textX = bx + 5;
            int textY = by + 4;
            int w = this.font.width(this.sectionName);
            if (within(mx, my, textX, textY, textX + w, textY + 9)) {
                return EditTarget.TEXT;
            }
            if (within(mx, my, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2)) {
                return EditTarget.BOX;
            }
        }
        if (within(mx, my, bx, by, bx + BannerTextures.WIDTH, by + CaoSection.CONTENT_H)) {
            return EditTarget.BANNER;
        }
        return null;
    }

    private static boolean within(int x, int y, int x1, int y1, int x2, int y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }

    private static String displayFilename(String ref) {
        if (ref.startsWith("file:") || ref.startsWith("remote:")) {
            return ref.substring(ref.indexOf(':') + 1);
        }
        int slash = ref.lastIndexOf('/');
        return slash >= 0 ? ref.substring(slash + 1) : ref;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoverBannerTooltip = null;
        super.render(g, mouseX, mouseY, partialTick);
        Component titleLine = currentTitle();
        g.drawString(this.font, titleLine, areaCenterX() - this.font.width(titleLine) / 2,
                areaTop() + MenuLayout.TITLE_Y, GlassSkin.titleTextColor(), GlassSkin.shadow());

        if (hoverBannerTooltip != null) {
            g.renderTooltip(this.font, hoverBannerTooltip, mouseX, mouseY);
        }

        if (highlightOnly) {
            return;
        }

        int bx = previewX();
        int by = previewY;
        g.fill(bx - 2, by - 2, bx + BannerTextures.WIDTH + 2, by + CaoSection.CONTENT_H + 2, 0xD0202020);

        boolean hasBanner = hasBanner();
        drawBannerContent(g, bx, by, mouseX, mouseY, false);

        EditTarget hovered = hitTest(mouseX, mouseY);

        boolean pulsing = !Config.editorHintSeen();

        if (hasBanner) {
            int textX = bx + 5;
            int textY = by + 4;
            int w = this.font.width(this.sectionName);
            if (pulsing) {
                outline(g, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2, pulseColor(0.12f));
                outline(g, textX, textY, textX + w, textY + 9, pulseColor(0.24f));
            }
            if (hovered == EditTarget.TEXT) {
                outline(g, textX, textY, textX + w, textY + 9);
            } else if (hovered == EditTarget.BOX) {
                outline(g, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2);
            }
        }
        if (pulsing) {
            outline(g, bx - 2, by - 2, bx + BannerTextures.WIDTH + 2, by + CaoSection.CONTENT_H + 2, pulseColor(0f));
        }
        if (hovered == EditTarget.BANNER) {
            outline(g, bx - 2, by - 2, bx + BannerTextures.WIDTH + 2, by + CaoSection.CONTENT_H + 2);
        }
    }

    private static final long PULSE_MS = 1700;

    private static int pulseColor(float phaseOffset) {
        float phase = (System.currentTimeMillis() % PULSE_MS) / (float) PULSE_MS + phaseOffset;
        float wave = (Mth.sin(phase * Mth.TWO_PI) + 1f) / 2f;
        int a = 0x30 + Math.round((0xB0 - 0x30) * wave);
        return (a << 24) | 0x00FFFFFF;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!embedded) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        }
        if (contrastH <= 0) {
            return;
        }
        GlassSkin.panel(g, contentX, contrastY - CONTRAST_PAD, contentW, contrastH + CONTRAST_PAD * 2);
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2) {
        outline(g, x1, y1, x2, y2, 0xFFFFFFFF);
    }

    private static void outline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    private static void drawMarker(GuiGraphics g, int cx, int cy) {
        outline(g, cx - 5, cy - 5, cx + 5, cy + 5, 0xFF000000);
        outline(g, cx - 4, cy - 4, cx + 4, cy + 4, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        svSquareTexture.release();
        hueBarTexture.release();
        if (embedded) {
            onEmbeddedDone.run();
            return;
        }
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    private boolean hasBanner() {
        return mode == Mode.COLOR || selectedTexture != null;
    }

    private void captureScreenshot() {
        BannerScreenshot.capture(BannerTextures.WIDTH, BannerTextures.HEIGHT, sectionName.getString(),
                g -> drawBannerContent(g, 0, 0, -1, -1, true));
    }

    private void drawBannerContent(GuiGraphics g, int bx, int by, int mouseX, int mouseY, boolean staticFrame) {
        int renderHeight = staticFrame ? BannerTextures.HEIGHT : CaoSection.CONTENT_H;
        if (mode == Mode.COLOR) {
            ColorSpec bannerSpec = buildSpec(bannerHsva, bannerGradientEnabled, bannerHsva2, bannerDirection, bannerStyle);
            BannerFill.draw(g, bx, by, bx + BannerTextures.WIDTH, by + renderHeight, bannerSpec);
        } else if (selectedTexture != null) {
            float v = 0.0F;
            int texHeight = BannerTextures.HEIGHT;
            var anim = BannerAnimation.preview(selectedTexture, bannerAnimated, bannerFrameTicks);
            if (anim.isPresent()) {
                boolean hovered = !staticFrame
                        && BannerAnimation.isHovering(bx, by, BannerTextures.WIDTH, CaoSection.CONTENT_H, mouseX, mouseY);
                int frame = staticFrame ? 0 : BannerAnimation.currentFrame(selectedTexture, anim.get(), hovered);
                v = frame * BannerTextures.HEIGHT;
                texHeight = anim.get().frameCount() * BannerTextures.HEIGHT;
            }

            g.blit(selectedTexture, bx, by, BannerTextures.WIDTH, renderHeight, 0.0F, v,
                    BannerTextures.WIDTH, renderHeight, BannerTextures.WIDTH, texHeight);
        } else {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.banner.none"),
                    bx + BannerTextures.WIDTH / 2, by + (renderHeight - 8) / 2, 0xFF888888);
        }

        if (hasBanner()) {
            int textX = bx + 5;
            int textY = by + 4;
            int maxX = bx + BannerTextures.WIDTH - 3;
            int available = maxX - textX;
            int viewMaxX = textX + Math.round(available * scrollCutoff);
            int viewAvailable = viewMaxX - textX;
            boolean scrolling = !staticFrame && this.font.width(this.sectionName) > viewAvailable;
            if (Config.tintedTextBox()) {
                int w = scrolling ? viewAvailable : this.font.width(this.sectionName);
                ResourceLocation boxTex = boxMode == Mode.IMAGE ? selectedBoxTexture : null;
                BoxTextures.draw(g, boxTex, textX - 4, textY - 3, textX + w + 3, textY + 9 + 2, boxHsva.toArgb(),
                        boxDarken, boxOpacity);
            }
            boolean vanillaShadow = shadowEnabled && !shadowUnlinked;
            int manualShadowArgb = shadowEnabled && shadowUnlinked ? shadowHsva.toArgb() : 0;
            ColorSpec textSpec = buildSpec(textHsva, textGradientEnabled, textHsva2, ColorSpec.Direction.HORIZONTAL, ColorSpec.Style.SMOOTH);
            ColorSpec secondarySpec = twoTone
                    ? buildSpec(text2Hsva, text2GradientEnabled, text2Hsva2, ColorSpec.Direction.HORIZONTAL, ColorSpec.Style.SMOOTH)
                    : null;
            ColorSpec outlineSpec = outlineEnabled
                    ? buildSpec(outlineHsva, outlineGradientEnabled, outlineHsva2, ColorSpec.Direction.HORIZONTAL, ColorSpec.Style.SMOOTH)
                    : null;
            TwoToneText.draw(g, this.font, this.sectionName, textX, textY, viewMaxX, textSpec,
                    secondarySpec, twoToneSplit, vanillaShadow, manualShadowArgb, outlineSpec);

            if (!staticFrame && target == EditTarget.TEXT && textEditTarget == TextTarget.PRIMARY) {
                g.fill(viewMaxX, by, viewMaxX + 1, by + renderHeight, 0xFFFFFF55);
            }
        }
    }

    private static final class Hsva {
        float h;
        float s;
        float v;
        float a = 1f;

        int toArgb() {
            return (Math.round(a * 255) << 24) | ColorUtil.hsvToRgb(h, s, v);
        }

        static Hsva fromArgb(int argb) {
            Hsva hsva = new Hsva();
            float[] hsv = ColorUtil.rgbToHsv(argb);
            hsva.h = hsv[0];
            hsva.s = hsv[1];
            hsva.v = hsv[2];
            hsva.a = ((argb >>> 24) & 0xFF) / 255f;
            return hsva;
        }
    }

    private static class ChannelSlider extends AbstractSliderButton {
        private final Component label;
        private final DoubleConsumer onChange;
        private final DoubleFunction<String> formatter;

        ChannelSlider(int x, int y, int w, int h, Component label, double initial, DoubleConsumer onChange) {
            this(x, y, w, h, label, initial, onChange, null);
        }

        ChannelSlider(int x, int y, int w, int h, Component label, double initial, DoubleConsumer onChange,
                DoubleFunction<String> formatter) {
            super(x, y, w, h, Component.empty(), initial);
            this.label = label;
            this.onChange = onChange;
            this.formatter = formatter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            String suffix = formatter != null ? formatter.apply(this.value) : Math.round(this.value * 100) + "%";
            setMessage(label.copy().append(": " + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(this.value);
        }
    }

    @FunctionalInterface
    private interface CellColor {
        int argb(float fracX, float fracY);
    }

    private static final class GradientTexture {
        private final ResourceLocation id;
        private DynamicTexture texture;
        private float keyA = Float.NaN;
        private int keyCell = -1;

        GradientTexture(String name) {
            this.id = ResourceLocation.fromNamespaceAndPath(createaddonorganizer.MODID, "gradient_cache/" + name);
        }

        ResourceLocation ensure(int w, int h, float keyA, int cellSize, CellColor fn) {
            boolean sizeMatches = texture != null
                    && texture.getPixels() != null
                    && texture.getPixels().getWidth() == w
                    && texture.getPixels().getHeight() == h;
            if (sizeMatches && this.keyA == keyA && this.keyCell == cellSize) {
                return id;
            }
            NativeImage image = sizeMatches ? texture.getPixels() : new NativeImage(w, h, false);
            for (int cy = 0; cy < h; cy += cellSize) {
                int ch = Math.min(cellSize, h - cy);
                float fracY = (cy + ch / 2f) / h;
                for (int cx = 0; cx < w; cx += cellSize) {
                    int cw = Math.min(cellSize, w - cx);
                    float fracX = (cx + cw / 2f) / w;
                    int abgr = FastColor.ABGR32.fromArgb32(0xFF000000 | fn.argb(fracX, fracY));
                    for (int py = cy; py < cy + ch; py++) {
                        for (int px = cx; px < cx + cw; px++) {
                            image.setPixelRGBA(px, py, abgr);
                        }
                    }
                }
            }
            if (!sizeMatches) {
                if (texture != null) {
                    Minecraft.getInstance().getTextureManager().release(id);
                    texture.close();
                }
                texture = new DynamicTexture(image);
                Minecraft.getInstance().getTextureManager().register(id, texture);
            } else {
                texture.upload();
            }
            this.keyA = keyA;
            this.keyCell = cellSize;
            return id;
        }

        void release() {
            if (texture != null) {
                Minecraft.getInstance().getTextureManager().release(id);
                texture = null;
                keyA = Float.NaN;
                keyCell = -1;
            }
        }
    }

    private static class SvSquare extends AbstractWidget {
        private final Hsva target;
        private final Runnable afterChange;
        private final IntSupplier cellSize;
        private final GradientTexture gradient;

        SvSquare(int x, int y, int w, int h, Hsva target, Runnable afterChange, IntSupplier cellSize, GradientTexture gradient) {
            super(x, y, w, h, Component.empty());
            this.target = target;
            this.afterChange = afterChange;
            this.cellSize = cellSize;
            this.gradient = gradient;
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            update(mouseX, mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            update(mouseX, mouseY);
        }

        private void update(double mouseX, double mouseY) {
            target.s = Mth.clamp((float) ((mouseX - getX()) / getWidth()), 0f, 1f);
            target.v = Mth.clamp((float) (1 - (mouseY - getY()) / getHeight()), 0f, 1f);
            afterChange.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            int w = getWidth();
            int h = getHeight();
            ResourceLocation tex = gradient.ensure(w, h, target.h, cellSize.getAsInt(),
                    (fracX, fracY) -> ColorUtil.hsvToRgb(target.h, fracX, 1f - fracY));
            g.blit(tex, x0, y0, 0.0F, 0.0F, w, h, w, h);

            int mx = x0 + Math.round(target.s * w);
            int my = y0 + Math.round((1 - target.v) * h);
            drawMarker(g, mx, my);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {}
    }

    private static class HueBar extends ChannelSlider {
        private final Hsva target;
        private final IntSupplier cellSize;
        private final GradientTexture gradient;

        HueBar(int x, int y, int w, int h, Hsva target, Runnable afterChange, IntSupplier cellSize, GradientTexture gradient) {
            super(x, y, w, h, Component.translatable("createaddonorganizer.colors.hue"), target.h,
                    v -> {
                        target.h = (float) v;
                        afterChange.run();
                    });
            this.target = target;
            this.cellSize = cellSize;
            this.gradient = gradient;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x0 = getX();
            int y0 = getY();
            int w = getWidth();
            int h = getHeight();
            ResourceLocation tex = gradient.ensure(w, h, 0f, cellSize.getAsInt(),
                    (fracX, fracY) -> ColorUtil.hsvToRgb(fracX, 1f, 1f));
            g.blit(tex, x0, y0, 0.0F, 0.0F, w, h, w, h);

            int mx = x0 + Math.round(target.h * w);
            drawMarker(g, mx, y0 + h / 2);
        }
    }

    private class GalleryList extends ContainerObjectSelectionList<GalleryList.Row> {
        private final ListGlide glide = new ListGlide();

        GalleryList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void setRefs(List<String> refs) {
            clearEntries();
            for (List<String> group : BannerGrid.chunk(refs, galleryColumns)) {
                addEntry(new Row(group));
            }
        }

        @Override
        public int getRowWidth() {
            return BannerGrid.rowWidth(galleryColumns);
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
                return RowChildren.none();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return RowChildren.none();
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button != 0) {
                    return false;
                }
                int cell = BannerGrid.cellAt(mx, GalleryList.this.getRowLeft(), getRowWidth(), galleryColumns);
                if (cell < 0 || cell >= refs.size()) {
                    return false;
                }
                String ref = refs.get(cell);
                ResourceLocation texture = BannerTextures.resolve(ref);
                if (texture == null) {
                    return false;
                }
                selectedTexture = texture;
                selectedRef = ref;
                syncAnimationFields();
                rebuildWidgets();
                return true;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int by = top + (rowHeight - BannerTextures.HEIGHT) / 2;
                for (int i = 0; i < refs.size(); i++) {
                    String ref = refs.get(i);
                    int bx = BannerGrid.cellX(left, rowWidth, galleryColumns, i);
                    boolean bannerHovered = BannerAnimation.isHovering(bx, by, BannerTextures.WIDTH,
                            BannerTextures.HEIGHT, mouseX, mouseY);
                    if (ref.equals(selectedRef)) {
                        outline(g, bx - 1, by - 1, bx + BannerTextures.WIDTH + 1, by + BannerTextures.HEIGHT + 1, 0xFFFFFFFF);
                    } else if (bannerHovered) {
                        outline(g, bx - 1, by - 1, bx + BannerTextures.WIDTH + 1, by + BannerTextures.HEIGHT + 1, 0x80FFFFFF);
                    }
                    ResourceLocation texture = BannerTextures.resolve(ref);
                    if (texture != null) {
                        int frame = BannerAnimation.get(texture)
                                .map(info -> BannerAnimation.currentFrame(texture, info, bannerHovered)).orElse(0);
                        g.blit(texture, bx, by, 0.0F, frame * BannerTextures.HEIGHT, BannerTextures.WIDTH,
                                BannerTextures.HEIGHT, BannerTextures.WIDTH, BannerAnimation.sheetHeight(texture));
                    }
                    if (bannerHovered) {
                        ColorPickerScreen.this.hoverBannerTooltip = Component.literal(displayFilename(ref));
                    }
                }
            }
        }
    }

    private class BoxGalleryList extends ContainerObjectSelectionList<BoxGalleryList.Row> {
        private final ListGlide glide = new ListGlide();

        BoxGalleryList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void setRefs(List<String> refs) {
            clearEntries();
            for (List<String> group : BannerGrid.chunk(refs, galleryColumns)) {
                addEntry(new Row(group));
            }
        }

        @Override
        public int getRowWidth() {
            return BannerGrid.rowWidth(galleryColumns);
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
                return RowChildren.none();
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return RowChildren.none();
            }

            @Override
            public boolean mouseClicked(double mx, double my, int button) {
                if (button != 0) {
                    return false;
                }
                int cell = BannerGrid.cellAt(mx, BoxGalleryList.this.getRowLeft(), getRowWidth(), galleryColumns);
                if (cell < 0 || cell >= refs.size()) {
                    return false;
                }
                String ref = refs.get(cell);
                ResourceLocation texture = BoxTextures.resolve(ref);
                if (texture == null) {
                    return false;
                }
                selectedBoxTexture = texture;
                selectedBoxRef = ref;
                rebuildWidgets();
                return true;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                int previewW = BannerTextures.WIDTH;
                int previewH = BoxTextures.HEIGHT;
                int by = top + (rowHeight - previewH) / 2;
                for (int i = 0; i < refs.size(); i++) {
                    String ref = refs.get(i);
                    int bx = BannerGrid.cellX(left, rowWidth, galleryColumns, i);
                    boolean cellHovered = mouseX >= bx && mouseX < bx + previewW
                            && mouseY >= by && mouseY < by + previewH;
                    if (ref.equals(selectedBoxRef)) {
                        outline(g, bx - 1, by - 1, bx + previewW + 1, by + previewH + 1, 0xFFFFFFFF);
                    } else if (cellHovered) {
                        outline(g, bx - 1, by - 1, bx + previewW + 1, by + previewH + 1, 0x80FFFFFF);
                    }
                    ResourceLocation texture = BoxTextures.resolve(ref);
                    OptionalInt nativeW = texture != null ? BoxTextures.nativeWidth(texture) : OptionalInt.empty();
                    if (texture != null && nativeW.isPresent()) {
                        BoxTextures.blit3Slice(g, texture, bx, by, previewW, previewH, nativeW.getAsInt(),
                                BoxTextures.HEIGHT);
                    }
                }
            }
        }
    }
}

