package com.sockywocky.createaddonorganizer.client;

import java.util.List;
import java.util.Optional;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CreditsScreen extends Screen {
    private static final int BAND_ALPHA = 0x2A;
    private static final int DIVIDER_ALPHA = 0x60;
    private static final int LINE_ALPHA = 0x50;
    private static final int SUB_LINE_X = 6;
    private static final int SUB_LINE_FADE = 10;

    private static final int BANNER_W = BannerTextures.WIDTH * 6 / 5;
    private static final int BANNER_H = (BannerTextures.HEIGHT * 6 + 2) / 5;
    private static final int ROW_HEIGHT = BANNER_H + 8;

    private static final String DISCORD_URL = "https://discord.com/invite/WgYePqcRTk";
    private static final int CONTRIBUTE_Y = MenuLayout.DESC_Y + 12;

    private final Screen parent;
    private boolean empty;
    private Component hoverBannerTooltip;
    private int panelW = MenuLayout.PANEL_W;

    public CreditsScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.colors.credits.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);

        int listTop = MenuLayout.ROW_1 + MenuLayout.GAP;
        int listBottom = MenuLayout.listBottom(this.height, 0);
        CreditsList list = new CreditsList(this.minecraft, panelW, listBottom - listTop, listTop, ROW_HEIGHT);
        list.setX(panelX);
        List<CreditsCatalog.Entry> entries = CreditsCatalog.rows();
        empty = entries.isEmpty();
        for (CreditsCatalog.Entry entry : entries) {
            list.add(entry);
        }
        addRenderableWidget(list);

        if (DevMode.isUnlocked()) {
            addRenderableWidget(new GlassToggle(6, 6,
                    Component.translatable("createaddonorganizer.colors.credits.localTesting"),
                    RemoteBanners.isLocalTesting(),
                    checked -> {
                        RemoteBanners.setLocalTesting(checked);
                        if (checked) {
                            RemoteBannerPools.refreshLocal();
                        } else {
                            RemoteBannerPools.loadCacheFromDisk();
                        }
                        BannerTextures.invalidateRemoteCache();
                        rebuildWidgets();
                    }));
            Button refreshButton = addRenderableWidget(Button.builder(
                            Component.translatable("createaddonorganizer.colors.credits.refresh"),
                            b -> {
                                RemoteBanners.refreshLocal();
                                RemoteBannerPools.refreshLocal();
                                BannerTextures.invalidateRemoteCache();
                                rebuildWidgets();
                            })
                    .bounds(6, 28, 100, 20).build());
            refreshButton.active = RemoteBanners.isLocalTesting();
        }

        int doneY = MenuLayout.doneY(this.height);
        int iconW = MenuLayout.ROW_H;
        addRenderableWidget(Button.builder(Component.literal("1"), b -> this.minecraft.setScreen(new TextBannerCreditsScreen(this.parent)))
                .bounds(panelX, doneY, iconW, MenuLayout.ROW_H)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.credits.viewTextBanner")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX + iconW + MenuLayout.GAP, doneY,
                        panelW - (iconW + MenuLayout.GAP) * 2, MenuLayout.ROW_H).build());
        addRenderableWidget(Button.builder(Component.literal("?"), b -> {})
                .bounds(panelX + panelW - iconW, doneY, iconW, MenuLayout.ROW_H)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.colors.credits.onlineHint")))
                .build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hoverBannerTooltip = null;
        super.render(g, mouseX, mouseY, partialTick);

        float scale = 1.6f;
        g.pose().pushPose();
        g.pose().scale(scale, scale, scale);
        g.drawCenteredString(this.font, this.title, Math.round(this.width / 2 / scale),
                Math.round(MenuLayout.TITLE_Y / scale), 0xFFFFFFFF);
        g.pose().popPose();

        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.credits.description"),
                this.width / 2, MenuLayout.DESC_Y, 0xAAAAAAAA);

        Component prefix = Component.translatable("createaddonorganizer.colors.credits.contribute");
        Component link = Component.translatable("createaddonorganizer.colors.credits.discord");
        int startX = this.width / 2 - (this.font.width(prefix) + this.font.width(link)) / 2;
        g.drawString(this.font, prefix, startX, CONTRIBUTE_Y, 0xAAAAAAAA);
        g.drawString(this.font, link, startX + this.font.width(prefix), CONTRIBUTE_Y, 0xFF5555FF);

        if (empty) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.colors.credits.empty"),
                    this.width / 2, this.height / 2, 0xFFAAAAAA);
        }

        if (hoverBannerTooltip != null) {
            g.renderTooltip(this.font, hoverBannerTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Component prefix = Component.translatable("createaddonorganizer.colors.credits.contribute");
            Component link = Component.translatable("createaddonorganizer.colors.credits.discord");
            int prefixWidth = this.font.width(prefix);
            int linkWidth = this.font.width(link);
            int linkX1 = this.width / 2 - (prefixWidth + linkWidth) / 2 + prefixWidth;
            int linkX2 = linkX1 + linkWidth;
            if (mouseX >= linkX1 && mouseX < linkX2 && mouseY >= CONTRIBUTE_Y && mouseY < CONTRIBUTE_Y + 9) {
                this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
                    if (confirmed) {
                        Util.getPlatform().openUri(DISCORD_URL);
                    }
                    this.minecraft.setScreen(this);
                }, DISCORD_URL, true));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    private class CreditsList extends ContainerObjectSelectionList<CreditsList.Row> {
        private final int rowHeight;
        private final ListGlide glide = new ListGlide();

        CreditsList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
            this.rowHeight = itemHeight;
        }

        void add(CreditsCatalog.Entry entry) {
            addEntry(new Row(entry));
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

        @Override
        public int getRowWidth() {
            return CreditsScreen.this.panelW - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private boolean isLastInGroup(int index) {
            List<Row> all = children();
            return index + 1 >= all.size() || all.get(index + 1).data.header();
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private final CreditsCatalog.Entry data;

            Row(CreditsCatalog.Entry data) {
                this.data = data;
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
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                if (data.header()) {
                    int band = (BAND_ALPHA << 24) | (data.nameColor() & 0x00FFFFFF);
                    int divider = (DIVIDER_ALPHA << 24) | (data.nameColor() & 0x00FFFFFF);
                    g.fill(left, top, left + rowWidth, top + rowHeight, band);
                    if (index != 0) {
                        g.fill(left, top, left + rowWidth, top + 1, divider);
                    }
                    Component name = Component.literal(data.label()).withStyle(ChatFormatting.BOLD);
                    g.drawString(font, name, left + 6, top + (rowHeight - 8) / 2, data.nameColor());
                    return;
                }

                int lineColor = (LINE_ALPHA << 24) | (data.nameColor() & 0x00FFFFFF);
                int lineX = left + SUB_LINE_X;
                int lineBottom = top + rowHeight;
                int gapToNext = CreditsList.this.rowHeight - rowHeight;
                if (CreditsList.this.isLastInGroup(index)) {
                    int fadeLen = Math.min(SUB_LINE_FADE, rowHeight);
                    int fadeStart = lineBottom - fadeLen;
                    if (fadeStart > top) {
                        g.fill(lineX, top, lineX + 2, fadeStart, lineColor);
                    }
                    g.fillGradient(lineX, fadeStart, lineX + 2, lineBottom, lineColor, lineColor & 0x00FFFFFF);
                } else {
                    g.fill(lineX, top, lineX + 2, lineBottom + gapToNext, lineColor);
                }

                int bx = left + (rowWidth - BANNER_W) / 2;
                int by = top + (rowHeight - BANNER_H) / 2;
                g.fill(bx - 1, by - 1, bx + BANNER_W + 1, by + BANNER_H + 1, 0xFF000000);
                Optional<BannerAnimation.AnimInfo> anim = BannerAnimation.get(data.texture());
                int frameCount = anim.map(BannerAnimation.AnimInfo::frameCount).orElse(1);
                boolean bannerHovered = BannerAnimation.isHovering(bx, by, BANNER_W, BANNER_H, mouseX, mouseY);
                int frame = anim.map(info -> BannerAnimation.currentFrame(data.texture(), info, bannerHovered)).orElse(0);
                g.blit(data.texture(), bx, by, BANNER_W, BANNER_H, 0.0F, frame * BannerTextures.HEIGHT,
                        BannerTextures.WIDTH, BannerTextures.HEIGHT,
                        BannerTextures.WIDTH, frameCount * BannerTextures.HEIGHT);
                if (bannerHovered && data.filename() != null) {
                    CreditsScreen.this.hoverBannerTooltip = Component.literal(data.filename());
                }
            }
        }
    }
}
