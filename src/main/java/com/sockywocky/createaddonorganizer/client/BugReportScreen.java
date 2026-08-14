package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;

import com.sockywocky.createaddonorganizer.Config;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public class BugReportScreen extends Screen {

    private static final String ISSUES_URL = "https://github.com/SockyWocky7/createaddonorganizer/issues";
    private static final String MCLOGS_URL = "https://mclo.gs";

    private static final int LINE_H = 11;
    private static final int PARAGRAPH_GAP = 6;

    private final Screen parent;
    private final List<Line> lines = new ArrayList<>();
    private int panelW = MenuLayout.PANEL_W;
    private int textTop;

    public BugReportScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.bugreport.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);
        int textW = panelW - 20;

        lines.clear();
        addParagraph(Component.translatable("createaddonorganizer.bugreport.heading").withStyle(ChatFormatting.BOLD),
                textW, MenuSkin.bodyColor(0xFFFFFFFF));
        addGap();
        addParagraph(Component.translatable("createaddonorganizer.bugreport.log"), textW, MenuSkin.mutedColor(0xFFAAAAAA));
        addParagraph(Component.translatable("createaddonorganizer.bugreport.upload"), textW, MenuSkin.mutedColor(0xFFAAAAAA));
        addParagraph(Component.translatable("createaddonorganizer.bugreport.account"), textW, MenuSkin.mutedColor(0xFFAAAAAA));
        addGap();
        addParagraph(Component.translatable("createaddonorganizer.bugreport.thanks"), textW, MenuSkin.accent(0xFF55FF55));

        int linkRowY = MenuLayout.doneY(this.height) - MenuLayout.ROW_H - MenuLayout.GAP;
        int blockH = lines.size() * LINE_H;
        int available = linkRowY - MenuLayout.GAP - MenuLayout.ROW_1;
        textTop = MenuLayout.ROW_1 + Math.max(0, (available - blockH) / 2);

        int half = MenuLayout.split(panelW, 2);
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.bugreport.openMclogs"),
                        b -> openLink(MCLOGS_URL))
                .bounds(panelX, linkRowY, half, MenuLayout.ROW_H).build());
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.bugreport.openIssues"),
                        b -> openLink(ISSUES_URL))
                .bounds(panelX + panelW - half, linkRowY, half, MenuLayout.ROW_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX, MenuLayout.doneY(this.height), panelW, MenuLayout.ROW_H).build());
    }

    private void addParagraph(Component text, int width, int color) {
        for (FormattedCharSequence line : this.font.split(text, width)) {
            lines.add(new Line(line, color));
        }
    }

    private void addGap() {
        lines.add(new Line(null, 0));
    }

    private void openLink(String url) {
        this.minecraft.setScreen(new ConfirmLinkScreen(confirmed -> {
            if (confirmed) {
                Util.getPlatform().openUri(url);
            }
            this.minecraft.setScreen(this);
        }, url, true));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        float scale = 1.6f;
        g.pose().pushPose();
        g.pose().scale(scale, scale, scale);
        g.drawCenteredString(this.font, this.title, Math.round(this.width / 2 / scale),
                Math.round(MenuLayout.TITLE_Y / scale), MenuSkin.titleColor(0xFFFFFFFF));
        g.pose().popPose();

        int y = textTop;
        for (Line line : lines) {
            if (line.text() != null) {
                g.drawString(this.font, line.text(), this.width / 2 - this.font.width(line.text()) / 2, y, line.color());
                y += LINE_H;
            } else {
                y += PARAGRAPH_GAP;
            }
        }
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    private record Line(FormattedCharSequence text, int color) {}
}
