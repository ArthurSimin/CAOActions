package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.sockywocky.createaddonorganizer.client.Presets.PresetData;
import com.sockywocky.createaddonorganizer.client.Presets.PresetRef;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PresetsScreen extends Screen implements EmbeddedPane {
    private static final int PAD = 10;
    private static final float TITLE_SCALE = 1.6f;

    private final Screen parent;
    private int panelW = MenuLayout.PANEL_W;
    private PresetList list;
    private String renamingRef;
    private EditBox renameBox;
    private RenameIconButton renameConfirm;
    private RenameIconButton renameCancel;

    private static final int BAND_H = 34;

    private boolean embedded;
    private int embedX;
    private int embedY;
    private int embedW;
    private int embedH;
    private Runnable onEmbeddedDone;
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

    private Screen back() {
        return embedded ? parent : this;
    }

    private int chromeX;
    private int chromeY;
    private int chromeW;
    private int chromeH;

    public PresetsScreen(Screen parent) {
        super(Component.translatable("createaddonorganizer.colors.presets.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = embedded ? Math.min(MenuLayout.PANEL_W, areaW() - PAD * 4) : MenuLayout.panelWidth(this.width);
        int panelX = embedded ? areaX() + (areaW() - panelW) / 2 : MenuLayout.panelX(this.width);
        int half = MenuLayout.split(panelW, 2);

        chromeX = embedded ? areaX() : panelX - PAD;
        chromeW = embedded ? areaW() : panelW + PAD * 2;
        chromeY = embedded ? embedY : MenuLayout.ROW_1 - PAD;
        chromeH = embedded ? embedH : this.height - 6 - chromeY;
        int rowTop = embedded ? chromeY + BAND_H : MenuLayout.ROW_1;

        addRenderableWidget(new GlassButton(panelX, rowTop, half, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.presets.saveNew"),
                b -> ScreenSwoosh.drill(() -> new NewPresetScreen(back()), Config.SWOOSH_PRESETS)));
        addRenderableWidget(new GlassButton(panelX + panelW - half, rowTop, half, MenuLayout.ROW_H,
                Component.translatable("createaddonorganizer.colors.presets.import"), b -> importPreset()));

        int doneY = chromeY + chromeH - PAD - MenuLayout.ROW_H;
        int listTop = rowTop + MenuLayout.ROW_H + 8;
        int listBottom = doneY - 8;

        list = new PresetList(this.minecraft, panelW, listBottom - listTop, listTop, 24);
        list.setX(panelX);
        for (PresetRef ref : Presets.gallery()) {
            list.add(ref);
        }
        addRenderableWidget(list);

        addRenderableWidget(new GlassButton(panelX, doneY, panelW, MenuLayout.ROW_H,
                Component.translatable("gui.done"), b -> onClose()).style(GlassButton.Style.ACCENT));
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
                Math.round((embedded ? chromeY + 5 : MenuLayout.TITLE_Y) / TITLE_SCALE),
                GlassSkin.titleTextColor(), GlassSkin.shadow());
        g.pose().popPose();

        Component description = Component.translatable("createaddonorganizer.colors.presets.description");
        g.drawString(this.font, description, areaCenterX() - this.font.width(description) / 2,
                embedded ? chromeY + 21 : MenuLayout.DESC_Y, GlassSkin.bodyTextColor(), GlassSkin.shadow());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renamingRef != null) {
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
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (renameBox != null && renameBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (renamingRef != null) {
            if (renameConfirm.mouseClicked(mouseX, mouseY, button)
                    || renameCancel.mouseClicked(mouseX, mouseY, button)
                    || renameBox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            cancelRename();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void startRename(PresetRef ref) {
        renamingRef = ref.ref();
        renameBox = new EditBox(this.font, 0, 0, 100, 20, Component.empty());
        renameBox.setMaxLength(64);
        renameBox.setValue(ref.name());
        renameBox.setHighlightPos(0);
        renameBox.setFocused(true);
        renameConfirm = new RenameIconButton(true, Component.translatable("createaddonorganizer.colors.ok"),
                b -> confirmRename());
        renameCancel = new RenameIconButton(false, Component.translatable("createaddonorganizer.colors.cancel"),
                b -> cancelRename());
    }

    private void confirmRename() {
        if (renamingRef == null) {
            return;
        }
        String ref = renamingRef;
        String name = renameBox.getValue().trim();
        if (!name.isEmpty()) {
            Presets.rename(ref, name);
            list.updateName(ref, name);
        }
        cancelRename();
    }

    private void cancelRename() {
        renamingRef = null;
        renameBox = null;
        renameConfirm = null;
        renameCancel = null;
    }

    private void importPreset() {
        Presets.chooseImportFile().ifPresent(path -> {
            PresetData data = Presets.loadExternal(path);
            if (data == null) {
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.import.failed"), Notice.RED);
                return;
            }
            try {
                Presets.save(data);
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.import.success", data.name()), Notice.GREEN);
                this.rebuildWidgets();
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to import preset {}", path, e);
            }
        });
    }

    private void applyWithConfirm(PresetRef ref) {
        Component message = Component.translatable("createaddonorganizer.colors.presets.applyConfirm.message");
        if ("Rainbow".equalsIgnoreCase(ref.name())) {
            message = message.copy().append("\n\n").append(
                    Component.translatable("createaddonorganizer.colors.presets.rainbowWarning")
                            .withStyle(ChatFormatting.RED));
        }
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PresetData data = Presets.load(ref.ref());
                if (data != null) {
                    Presets.applyToConfig(data);
                    Presets.applyLive();
                    Notice.show(Component.translatable("createaddonorganizer.colors.presets.applied", ref.name()), Notice.GREEN);
                }
            }
            this.minecraft.setScreen(back());
        }, Component.translatable("createaddonorganizer.colors.presets.applyConfirm.title"), message));
    }

    @Override
    public void onClose() {
        if (embedded) {
            onEmbeddedDone.run();
            return;
        }
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }

    private class PresetList extends ContainerObjectSelectionList<PresetList.Row> {
        private final ListGlide glide = new ListGlide();

        PresetList(Minecraft mc, int width, int height, int top, int itemHeight) {
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

        void add(PresetRef ref) {
            addEntry(new Row(ref));
        }

        void updateName(String ref, String newName) {
            for (Row row : children()) {
                if (row.ref.ref().equals(ref)) {
                    row.ref = new PresetRef(ref, newName);
                }
            }
        }

        @Override
        public int getRowWidth() {
            return PresetsScreen.this.panelW - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + this.getWidth() - 6;
        }

        private class Row extends ContainerObjectSelectionList.Entry<Row> {
            private PresetRef ref;
            private final Button edit;

            Row(PresetRef ref) {
                this.ref = ref;
                this.edit = MenuSkin.markEdit(Button.builder(Component.translatable("createaddonorganizer.colors.edit"),
                                b -> ScreenSwoosh.drill(() -> new PresetEditScreen(PresetsScreen.this.back(), ref),
                                        Config.SWOOSH_PRESETS))
                        .size(44, 20).build());
            }

            @Override
            public List<? extends GuiEventListener> children() {
                return List.of(edit);
            }

            @Override
            public List<? extends NarratableEntry> narratables() {
                return List.of(edit);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (super.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (button == 0 && Screen.hasControlDown() && !ref.builtin()) {
                    PresetsScreen.this.startRename(ref);
                    return true;
                }
                if (button == 0) {
                    applyWithConfirm(ref);
                    return true;
                }
                return false;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight,
                    int mouseX, int mouseY, boolean hovered, float partialTick) {
                boolean renaming = ref.ref().equals(renamingRef);
                if (hovered && !renaming) {
                    int accent = GlassSkin.accent();
                    g.fill(left, top, left + rowWidth, top + rowHeight, MenuSkin.fade(accent, 0.12f));
                    g.fill(left, top, left + 2, top + rowHeight, accent);
                }

                int textY = top + (rowHeight - 8) / 2;
                String tag = ref.builtin()
                        ? Component.translatable("createaddonorganizer.colors.presets.builtin").getString() : "";

                edit.setX(left + rowWidth - edit.getWidth());
                edit.setY(top + (rowHeight - 20) / 2);
                int labelX = edit.getX() - 10 - font.width(tag);

                if (renaming) {
                    renameCancel.setX(edit.getX() - 22);
                    renameCancel.setY(edit.getY());
                    renameConfirm.setX(renameCancel.getX() - 22);
                    renameConfirm.setY(edit.getY());
                    renameBox.setX(left + 6);
                    renameBox.setY(edit.getY());
                    renameBox.setWidth(renameConfirm.getX() - 10 - left);
                    renameBox.render(g, mouseX, mouseY, partialTick);
                    renameConfirm.render(g, mouseX, mouseY, partialTick);
                    renameCancel.render(g, mouseX, mouseY, partialTick);
                } else if ("Rainbow".equalsIgnoreCase(ref.name())) {
                    drawRainbowName(g, ref.name(), left + 6, textY);
                    if (!tag.isEmpty()) {
                        g.drawString(font, tag, labelX, textY, GlassSkin.bodyTextColor(), GlassSkin.shadow());
                    }
                } else {
                    int nameColor = "Images".equalsIgnoreCase(ref.name()) ? 0xFFFFFF55
                            : hovered ? GlassSkin.titleTextColor() : GlassSkin.rowTextColor();
                    g.drawString(font, ref.name(), left + 6, textY, nameColor, GlassSkin.shadow());
                    if (!tag.isEmpty()) {
                        g.drawString(font, tag, labelX, textY, GlassSkin.bodyTextColor(), GlassSkin.shadow());
                    }
                }
                edit.render(g, mouseX, mouseY, partialTick);
            }

            private void drawRainbowName(GuiGraphics g, String name, int x, int y) {
                int cursorX = x;
                for (int i = 0; i < name.length(); i++) {
                    String ch = name.substring(i, i + 1);
                    float hue = (float) i / name.length();
                    int color = 0xFF000000 | ColorUtil.hsvToRgb(hue, 1f, 1f);
                    g.drawString(font, ch, cursorX, y, color, GlassSkin.shadow());
                    cursorX += font.width(ch);
                }
            }
        }
    }
}
