package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

public class NewPresetScreen extends Screen {
    private static final int PAD = 10;
    private static final int HEADER_H = 18;
    private static final int PROMPT_H = 14;

    private final Screen parent;
    private EditBox nameBox;
    private GlassButton okButton;

    private int chromeX;
    private int chromeY;
    private int chromeW;
    private int chromeH;
    private int headerY;
    private int promptY;
    private int fieldX;
    private int fieldY;
    private int fieldW;

    public NewPresetScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.colors.presets.saveNew"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);

        chromeW = panelW + PAD * 2;
        chromeH = PAD * 2 + HEADER_H + PROMPT_H + (MenuLayout.ROW_H + MenuLayout.GAP) + MenuLayout.ROW_H;
        chromeX = panelX - PAD;
        chromeY = (this.height - chromeH) / 2;
        headerY = chromeY + PAD;
        promptY = headerY + HEADER_H;

        fieldX = panelX;
        fieldY = promptY + PROMPT_H;
        fieldW = panelW;

        nameBox = new EditBox(this.font, fieldX + 6, fieldY, fieldW - 12, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.presets.saveNew"));
        nameBox.setMaxLength(48);
        nameBox.setBordered(false);
        nameBox.setResponder(s -> okButton.active = !s.trim().isEmpty());
        addWidget(nameBox);
        setInitialFocus(nameBox);

        int buttonY = MenuLayout.nextRow(fieldY);
        int half = MenuLayout.split(panelW, 2);
        okButton = new GlassButton(panelX, buttonY, half, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.ok"), b -> confirm())
                .style(GlassButton.Style.ACCENT);
        okButton.active = false;
        addRenderableWidget(okButton);
        addRenderableWidget(new GlassButton(MenuLayout.splitX(panelX, panelW, 2, 1), buttonY, half, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.cancel"), b -> onClose()));
    }

    private void confirm() {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        try {
            Presets.save(Presets.captureCurrent(name));
            Notice.show(Component.translatable("createaddonorganizer.colors.presets.created", name), Notice.GREEN);
            onClose();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save new preset", e);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && okButton.active) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        GlassSkin.panel(g, chromeX, chromeY, chromeW, chromeH);
        GlassSkin.header(g, this.font, this.title, chromeX + PAD, headerY, chromeW - PAD * 2, 1f);
        g.drawString(this.font, Component.translatable("createaddonorganizer.colors.presets.namePrompt"),
                chromeX + PAD, promptY, GlassSkin.bodyTextColor(), GlassSkin.shadow());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        GlassSkin.widgetBox(g, fieldX, fieldY, fieldW, MenuLayout.ROW_H, nameBox.isFocused());
        g.pose().pushPose();
        g.pose().translate(0f, (MenuLayout.ROW_H - 8) / 2f, 0f);
        nameBox.render(g, mouseX, mouseY, partialTick);
        g.pose().popPose();
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }
}
