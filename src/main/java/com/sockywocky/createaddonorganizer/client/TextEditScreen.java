package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class TextEditScreen extends Screen {

    private static final int PLACEHOLDER_LIMIT = 4096;
    private static final int KEY_ROW_H = 12;

    private final Screen returnTo;
    private final List<String> keys;

    private int index;
    private MultiLineEditBox editor;

    public TextEditScreen(Screen returnTo, List<String> keys, int index) {
        super(Component.translatable("createaddonorganizer.textEditor.title"));
        this.returnTo = returnTo;
        this.keys = keys;
        this.index = Math.max(0, Math.min(index, keys.size() - 1));
    }

    private String key() {
        return keys.get(index);
    }

    @Override
    protected void init() {
        int panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);
        int buttonsY = MenuLayout.doneY(this.height);
        int editorY = MenuLayout.ROW_1 + KEY_ROW_H * 2 + MenuLayout.GAP;
        int editorH = Math.max(3 * this.font.lineHeight + 8, buttonsY - MenuLayout.GAP * 2 - editorY);

        if (keys.size() > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> step(-1))
                    .bounds(panelX - MenuLayout.ROW_H - MenuLayout.GAP, MenuLayout.ROW_1 - 4,
                            MenuLayout.ROW_H, MenuLayout.ROW_H)
                    .build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> step(1))
                    .bounds(panelX + panelW + MenuLayout.GAP, MenuLayout.ROW_1 - 4,
                            MenuLayout.ROW_H, MenuLayout.ROW_H)
                    .build());
        }

        editor = new MultiLineEditBox(this.font, panelX, editorY, panelW, editorH,
                Component.translatable("createaddonorganizer.textEditor.placeholder"),
                Component.translatable("createaddonorganizer.textEditor.title"));
        editor.setCharacterLimit(PLACEHOLDER_LIMIT);
        editor.setValue(LangEditor.current(key()));
        addRenderableWidget(editor);
        setInitialFocus(editor);

        int third = MenuLayout.split(panelW, 3);
        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.textEditor.save"),
                        b -> save())
                .bounds(panelX, buttonsY, third, MenuLayout.ROW_H).build());
        Button revert = Button.builder(Component.translatable("createaddonorganizer.textEditor.revert"),
                        b -> revert())
                .bounds(MenuLayout.splitX(panelX, panelW, 3, 1), buttonsY, third, MenuLayout.ROW_H).build();
        revert.active = LangEditor.isEdited(key());
        addRenderableWidget(revert);
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(MenuLayout.splitX(panelX, panelW, 3, 2), buttonsY, third, MenuLayout.ROW_H).build());
    }

    private void step(int delta) {
        index = Math.floorMod(index + delta, keys.size());
        rebuildWidgets();
    }

    private void save() {
        try {
            LangEditor.Result result = LangEditor.set(key(), editor.getValue());
            Notice.show(Component.translatable(result.target() == LangEditor.Target.SOURCE
                            ? "createaddonorganizer.textEditor.savedToSource"
                            : "createaddonorganizer.textEditor.savedToOverride",
                    result.file().getFileName().toString()), Notice.GREEN);
            onClose();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not save text for {}", key(), e);
            Notice.show(Component.translatable("createaddonorganizer.textEditor.saveFailed",
                    String.valueOf(e.getMessage())), Notice.RED);
        }
    }

    private void revert() {
        try {
            LangEditor.revert(key());
            Notice.show(Component.translatable("createaddonorganizer.textEditor.reverted"), Notice.GREEN);
            onClose();
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] could not revert text for {}", key(), e);
            Notice.show(Component.translatable("createaddonorganizer.textEditor.saveFailed",
                    String.valueOf(e.getMessage())), Notice.RED);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_S && hasControlDown()) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(returnTo);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);
        GlassSkin.panel(g, panelX - MenuLayout.GAP * 2, MenuLayout.TITLE_Y - MenuLayout.GAP * 2,
                panelW + MenuLayout.GAP * 4,
                MenuLayout.doneY(this.height) + MenuLayout.ROW_H + MenuLayout.GAP * 2 - MenuLayout.TITLE_Y
                        + MenuLayout.GAP * 2);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int panelX = MenuLayout.panelX(this.width);
        int panelW = MenuLayout.panelWidth(this.width);

        g.drawCenteredString(this.font, this.title, this.width / 2, MenuLayout.TITLE_Y, GlassSkin.titleTextColor());

        String counter = keys.size() > 1 ? (index + 1) + "/" + keys.size() + "  " : "";
        g.drawString(this.font, counter + trim(key(), panelW - this.font.width(counter)),
                panelX, MenuLayout.ROW_1, GlassSkin.headingColor(), GlassSkin.shadow());

        g.drawString(this.font, trim(destination(), panelW), panelX, MenuLayout.ROW_1 + KEY_ROW_H,
                GlassSkin.mutedTextColor(), GlassSkin.shadow());
    }

    private String destination() {
        Path source = LangEditor.owned(key()) ? LangEditor.sourceFile() : null;
        if (source != null) {
            return translated("createaddonorganizer.textEditor.targetSource", source.getFileName().toString());
        }
        return translated("createaddonorganizer.textEditor.targetOverride", LangEditor.languageCode() + ".json");
    }

    private static String translated(String key, Object arg) {
        return Component.translatable(key, arg).getString();
    }

    private String trim(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(8, maxWidth - this.font.width("..."))) + "...";
    }
}
