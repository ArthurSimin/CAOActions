package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

final class InfoPane {

    enum Kind { CREDITS, BUGS, INTEGRATIONS }

    private static final String DISCORD_URL = "https://discord.com/invite/WgYePqcRTk";
    private static final String ISSUES_URL = "https://github.com/SockyWocky7/createaddonorganizer/issues";
    private static final String MCLOGS_URL = "https://mclo.gs";

    private static final int PAD_X = 10;
    private static final int ROW_GAP = 5;
    private static final int GROUP_H = 22;
    private static final int LINE_H = 10;
    private static final int LINK_H = 18;
    private static final int NOTE_GAP = 4;
    private static final int BANNER_W = BannerTextures.WIDTH * 6 / 5;
    private static final int BANNER_H = (BannerTextures.HEIGHT * 6 + 2) / 5;

    private static final int MOD_PAD = 7;
    private static final int MOD_ICON = 32;
    private static final int MOD_GAP = 9;
    private static final int MOD_LINK_H = 15;
    private static final int MOD_LINK_GAP = 5;
    private static final int PILL_PAD = 4;

    private enum RowKind { HEADER, NOTE, LINK, BANNER, MOD }

    private final Screen owner;
    private final List<Row> rows = new ArrayList<>();
    private final Scrollbar bar = new Scrollbar();

    private int x;
    private int top;
    private int width;
    private int bottom;
    private Component hoverTip;

    InfoPane(Screen owner) {
        this.owner = owner;
    }

    void setBounds(int x, int top, int width, int height) {
        this.x = x;
        this.top = top;
        this.width = width;
        this.bottom = top + height;
        bar.bounds(x + width - 4 - Scrollbar.WIDTH, top, height);
    }

    void build(Kind kind, Font font) {
        rows.clear();
        switch (kind) {
            case CREDITS -> buildCredits();
            case INTEGRATIONS -> buildIntegrations();
            default -> buildBugReport();
        }
        layout(font);
    }

    void resetScroll() {
        bar.reset();
    }

    Component hoverTip() {
        return hoverTip;
    }

    private void buildCredits() {
        rows.add(Row.header(Component.translatable("createaddonorganizer.colors.credits.title"), 0));
        rows.add(Row.note(Component.translatable("createaddonorganizer.colors.credits.description"),
                GlassSkin.bodyTextColor()));
        rows.add(Row.link(Component.translatable("createaddonorganizer.colors.credits.discord"),
                () -> openLink(DISCORD_URL)));

        List<CreditsCatalog.Entry> banners = CreditsCatalog.rows();
        if (banners.isEmpty()) {
            rows.add(Row.note(Component.translatable("createaddonorganizer.colors.credits.empty"),
                    GlassSkin.bodyTextColor()));
        } else {
            addCreditEntries(banners);
        }

        List<CreditsCatalog.Entry> textBanners = CreditsCatalog.textBannerRows();
        if (!textBanners.isEmpty()) {
            rows.add(Row.header(Component.translatable("createaddonorganizer.settings.credits.textBanners"), 0));
            addCreditEntries(textBanners);
        }

        if (DevMode.isUnlocked()) {
            rows.add(Row.header(Component.translatable("createaddonorganizer.settings.credits.devTools"), 0));
            rows.add(Row.action(Component.translatable("createaddonorganizer.settings.credits.openLegacy"),
                    () -> ScreenSwoosh.drill(() -> new CreditsScreen(owner), Config.SWOOSH_CREDITS)));
        }
    }

    private void addCreditEntries(List<CreditsCatalog.Entry> entries) {
        for (CreditsCatalog.Entry entry : entries) {
            if (entry.header()) {
                rows.add(Row.header(Component.literal(entry.label()).withStyle(ChatFormatting.BOLD),
                        0xFF000000 | (entry.nameColor() & 0x00FFFFFF)));
            } else {
                rows.add(Row.banner(entry));
            }
        }
    }

    private void buildBugReport() {
        rows.add(Row.header(Component.translatable("createaddonorganizer.bugreport.title"), 0));
        rows.add(Row.note(Component.translatable("createaddonorganizer.bugreport.heading")
                .withStyle(ChatFormatting.BOLD), GlassSkin.titleTextColor()));
        rows.add(Row.note(Component.translatable("createaddonorganizer.bugreport.log"), GlassSkin.bodyTextColor()));
        rows.add(Row.note(Component.translatable("createaddonorganizer.bugreport.upload"), GlassSkin.bodyTextColor()));
        rows.add(Row.note(Component.translatable("createaddonorganizer.bugreport.account"), GlassSkin.bodyTextColor()));
        rows.add(Row.note(Component.translatable("createaddonorganizer.bugreport.thanks"),
                MenuSkin.accent(0xFF55FF55)));
        rows.add(Row.link(Component.translatable("createaddonorganizer.bugreport.openMclogs"),
                () -> openLink(MCLOGS_URL)));
        rows.add(Row.link(Component.translatable("createaddonorganizer.bugreport.openIssues"),
                () -> openLink(ISSUES_URL)));
    }

    private void buildIntegrations() {
        rows.add(Row.header(Component.translatable("createaddonorganizer.integrations.title"), 0));
        rows.add(Row.note(Component.translatable("createaddonorganizer.integrations.description"),
                GlassSkin.bodyTextColor()));
        for (IntegrationCatalog.Entry entry : IntegrationCatalog.entries()) {
            rows.add(Row.mod(entry));
        }
    }

    private void openLink(String url) {
        Minecraft.getInstance().setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(url);
            }
            Minecraft.getInstance().setScreen(owner);
        }, url, true));
    }

    private void layout(Font font) {
        int rowWidth = rowWidth();
        int y = 0;
        for (Row row : rows) {
            row.y = y;
            switch (row.kind) {
                case HEADER -> row.height = GROUP_H;
                case LINK -> row.height = LINK_H;
                case BANNER -> row.height = BANNER_H + 8;
                case NOTE -> {
                    row.description = font.split(row.header, rowWidth - PAD_X * 2);
                    row.height = row.description.size() * LINE_H + NOTE_GAP;
                }
                case MOD -> {
                    row.description = font.split(row.mod.description(),
                            Math.max(20, rowWidth - MOD_PAD * 2 - MOD_ICON - MOD_GAP));
                    int textH = 12 + row.description.size() * LINE_H;
                    row.height = MOD_PAD * 2 + Math.max(MOD_ICON, textH) + MOD_LINK_GAP + MOD_LINK_H;
                }
            }
            y += row.height + (row.kind == RowKind.HEADER ? 0 : ROW_GAP);
        }
        bar.content(y);
    }

    private int rowWidth() {
        return width - 16 - Scrollbar.WIDTH;
    }

    void render(GuiGraphics g, Font font, int mouseX, int mouseY) {
        hoverTip = null;
        int offset = bar.offset();
        int rowX = x + 8;
        int rowWidth = rowWidth();
        boolean overPane = mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < bottom;

        g.enableScissor(x + 1, top, x + width - 1, bottom);
        for (Row row : rows) {
            int y = top + row.y - offset;
            if (y + row.height < top - 8 || y > bottom + 8) {
                continue;
            }
            boolean hovered = overPane && mouseX >= rowX && mouseX < rowX + rowWidth
                    && mouseY >= y && mouseY < y + row.height;
            switch (row.kind) {
                case HEADER -> renderHeader(g, font, row, rowX, y, rowWidth);
                case NOTE -> renderNote(g, font, row, rowX, y);
                case LINK -> renderLink(g, font, row, rowX, y, rowWidth, hovered);
                case BANNER -> renderBanner(g, row, rowX, y, rowWidth, mouseX, mouseY);
                case MOD -> renderMod(g, font, row, rowX, y, rowWidth, mouseX, mouseY, overPane,
                        hovered);
            }
        }
        g.disableScissor();

        bar.render(g, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g, Font font, Row row, int rowX, int y, int rowWidth) {
        if (GlassSkin.glass()) {
            g.drawString(font, row.header, rowX + PAD_X, y + GROUP_H - 14,
                    row.color == 0 ? GlassSkin.accent() : row.color, GlassSkin.shadow());
            int ruleY = y + GROUP_H - 4;
            g.fill(rowX + 2, ruleY, rowX + rowWidth - 2, ruleY + 1, MenuSkin.fade(GlassSkin.accent(), 0.45f));
            return;
        }
        int textWidth = font.width(row.header);
        int textX = rowX + rowWidth / 2 - textWidth / 2;
        int lineY = y + GROUP_H / 2;
        int color = GlassSkin.borderColor();
        if (textX - 6 > rowX + 2) {
            g.fill(rowX + 2, lineY, textX - 6, lineY + 1, color);
            g.fill(textX + textWidth + 6, lineY, rowX + rowWidth - 2, lineY + 1, color);
        }
        g.drawString(font, row.header, textX, lineY - 4,
                row.color == 0 ? GlassSkin.headingColor() : row.color, GlassSkin.shadow());
    }

    private void renderNote(GuiGraphics g, Font font, Row row, int rowX, int y) {
        int textY = y;
        for (FormattedCharSequence line : row.description) {
            g.drawString(font, line, rowX + PAD_X, textY, row.color, GlassSkin.shadow());
            textY += LINE_H;
        }
    }

    private void renderLink(GuiGraphics g, Font font, Row row, int rowX, int y, int rowWidth, boolean hovered) {
        GlassSkin.widgetBox(g, rowX, y, rowWidth, LINK_H, hovered);
        int color = hovered ? GlassSkin.titleTextColor() : GlassSkin.accent();
        int textWidth = font.width(row.header);
        int textX = rowX + (rowWidth - textWidth - (row.arrow ? 9 : 0)) / 2;
        g.drawString(font, row.header, textX, y + 5, color, GlassSkin.shadow());
        if (row.arrow) {
            linkArrow(g, textX + textWidth + 3, y + 5, color);
        }
    }

    private static int[] modLink(int rowX, int rowWidth, int y, int height, boolean second) {
        int left = rowX + MOD_PAD;
        int usable = rowWidth - MOD_PAD * 2;
        int each = (usable - MOD_LINK_GAP) / 2;
        return new int[] {second ? left + usable - each : left, y + height - MOD_PAD - MOD_LINK_H, each,
                MOD_LINK_H};
    }

    private static boolean inRect(double mouseX, double mouseY, int[] rect) {
        return mouseX >= rect[0] && mouseX < rect[0] + rect[2]
                && mouseY >= rect[1] && mouseY < rect[1] + rect[3];
    }

    private void renderMod(GuiGraphics g, Font font, Row row, int rowX, int y, int rowWidth, int mouseX,
            int mouseY, boolean overPane, boolean hovered) {
        IntegrationCatalog.Entry entry = row.mod;
        boolean installed = entry.installed();

        g.fill(rowX, y, rowX + rowWidth, y + row.height, GlassSkin.cardColor(hovered));
        GlassSkin.outline(g, rowX, y, rowWidth, row.height, GlassSkin.cardBorder(hovered));
        ModIcons.render(g, entry.modId(), entry.name(), rowX + MOD_PAD, y + MOD_PAD, MOD_ICON, installed);

        int textX = rowX + MOD_PAD + MOD_ICON + MOD_GAP;
        int titleColor = installed ? GlassSkin.titleTextColor() : GlassSkin.mutedTextColor();
        g.drawString(font, entry.name(), textX, y + MOD_PAD, titleColor, GlassSkin.shadow());

        Component pill = Component.translatable(installed
                ? "createaddonorganizer.integrations.installed"
                : entry.required()
                        ? "createaddonorganizer.integrations.missing"
                        : "createaddonorganizer.integrations.notInstalled");
        int pillW = font.width(pill) + PILL_PAD * 2;
        int pillX = rowX + rowWidth - MOD_PAD - pillW;
        int pillColor = installed
                ? MenuSkin.accent(0xFF55D07A)
                : entry.required() ? GlassSkin.DANGER_LIT : GlassSkin.mutedTextColor();
        g.fill(pillX, y + MOD_PAD - 1, pillX + pillW, y + MOD_PAD + 10, MenuSkin.fade(pillColor, 0.16f));
        g.drawString(font, pill, pillX + PILL_PAD, y + MOD_PAD, pillColor, GlassSkin.shadow());

        int lineY = y + MOD_PAD + 12;
        int bodyColor = installed ? GlassSkin.bodyTextColor() : GlassSkin.mutedTextColor();
        for (FormattedCharSequence line : row.description) {
            g.drawString(font, line, textX, lineY, bodyColor, GlassSkin.shadow());
            lineY += LINE_H;
        }

        renderModLink(g, font, rowX, rowWidth, y, row.height, false,
                Component.translatable("createaddonorganizer.integrations.modrinth"), mouseX, mouseY,
                overPane);
        renderModLink(g, font, rowX, rowWidth, y, row.height, true,
                Component.translatable("createaddonorganizer.integrations.curseforge"), mouseX, mouseY,
                overPane);
    }

    private void renderModLink(GuiGraphics g, Font font, int rowX, int rowWidth, int y, int height,
            boolean second, Component label, int mouseX, int mouseY, boolean overPane) {
        int[] rect = modLink(rowX, rowWidth, y, height, second);
        boolean hovered = overPane && inRect(mouseX, mouseY, rect) && mouseY >= top && mouseY < bottom;
        GlassSkin.widgetBox(g, rect[0], rect[1], rect[2], rect[3], hovered);
        int color = hovered ? GlassSkin.titleTextColor() : GlassSkin.accent();
        int textW = font.width(label);
        int textX = rect[0] + (rect[2] - textW - 9) / 2;
        g.drawString(font, label, textX, rect[1] + 4, color, GlassSkin.shadow());
        linkArrow(g, textX + textW + 3, rect[1] + 4, color);
    }

    private void renderBanner(GuiGraphics g, Row row, int rowX, int y, int rowWidth, int mouseX, int mouseY) {
        CreditsCatalog.Entry entry = row.credit;
        int bx = rowX + (rowWidth - BANNER_W) / 2;
        int by = y + 4;
        g.fill(bx - 1, by - 1, bx + BANNER_W + 1, by + BANNER_H + 1, 0xFF000000);
        ResourceLocation texture = entry.texture();
        Optional<BannerAnimation.AnimInfo> anim = BannerAnimation.get(texture);
        int frameCount = anim.map(BannerAnimation.AnimInfo::frameCount).orElse(1);
        boolean hovered = BannerAnimation.isHovering(bx, by, BANNER_W, BANNER_H, mouseX, mouseY)
                && mouseY >= top && mouseY < bottom;
        int frame = anim.map(info -> BannerAnimation.currentFrame(texture, info, hovered)).orElse(0);
        g.blit(texture, bx, by, BANNER_W, BANNER_H, 0.0F, frame * BannerTextures.HEIGHT,
                BannerTextures.WIDTH, BannerTextures.HEIGHT,
                BannerTextures.WIDTH, frameCount * BannerTextures.HEIGHT);
        if (hovered && entry.filename() != null) {
            hoverTip = Component.literal(entry.filename());
        }
    }

    boolean mouseClicked(double mouseX, double mouseY) {
        if (bar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        int offset = bar.displayed();
        int rowX = x + 8;
        int rowWidth = rowWidth();
        for (Row row : rows) {
            int y = top + row.y - offset;
            if (mouseY < y || mouseY >= y + row.height) {
                continue;
            }
            if (row.kind == RowKind.MOD) {
                return clickMod(row, rowX, rowWidth, y, mouseX, mouseY);
            }
            if (row.kind != RowKind.LINK || mouseX < rowX || mouseX >= rowX + rowWidth) {
                return false;
            }
            Sfx.uiClick();
            row.action.run();
            return true;
        }
        return false;
    }

    private boolean clickMod(Row row, int rowX, int rowWidth, int y, double mouseX, double mouseY) {
        if (inRect(mouseX, mouseY, modLink(rowX, rowWidth, y, row.height, false))) {
            Sfx.uiClick();
            openLink(row.mod.modrinth());
            return true;
        }
        if (inRect(mouseX, mouseY, modLink(rowX, rowWidth, y, row.height, true))) {
            Sfx.uiClick();
            openLink(row.mod.curseforge());
            return true;
        }
        return false;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!inside(mouseX, mouseY)) {
            return false;
        }
        return bar.wheel(amount);
    }

    boolean mouseDragged(double mouseY) {
        return bar.mouseDragged(mouseY);
    }

    void mouseReleased() {
        bar.mouseReleased();
    }

    private boolean inside(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= top && mouseY < bottom;
    }

    private static void linkArrow(GuiGraphics g, int x, int y, int color) {
        for (int i = 0; i < 5; i++) {
            g.fill(x + i, y + 5 - i, x + i + 1, y + 6 - i, color);
        }
        g.fill(x + 2, y + 1, x + 5, y + 2, color);
        g.fill(x + 4, y + 1, x + 5, y + 4, color);
    }

    private static final class Row {
        final RowKind kind;
        final Component header;
        final CreditsCatalog.Entry credit;
        final Runnable action;
        final boolean arrow;
        IntegrationCatalog.Entry mod;
        int color;
        List<FormattedCharSequence> description = List.of();
        int y;
        int height;

        private Row(RowKind kind, Component header, CreditsCatalog.Entry credit, Runnable action, boolean arrow) {
            this.kind = kind;
            this.header = header;
            this.credit = credit;
            this.action = action;
            this.arrow = arrow;
        }

        static Row mod(IntegrationCatalog.Entry entry) {
            Row row = new Row(RowKind.MOD, null, null, null, false);
            row.mod = entry;
            return row;
        }

        static Row header(Component title, int color) {
            Row row = new Row(RowKind.HEADER, title, null, null, false);
            row.color = color;
            return row;
        }

        static Row note(Component text, int color) {
            Row row = new Row(RowKind.NOTE, text, null, null, false);
            row.color = color;
            return row;
        }

        static Row link(Component label, Runnable action) {
            return new Row(RowKind.LINK, label, null, action, true);
        }

        static Row action(Component label, Runnable action) {
            return new Row(RowKind.LINK, label, null, action, false);
        }

        static Row banner(CreditsCatalog.Entry entry) {
            return new Row(RowKind.BANNER, null, entry, null, false);
        }
    }
}
