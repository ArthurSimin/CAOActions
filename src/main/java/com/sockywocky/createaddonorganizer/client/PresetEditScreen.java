package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;

import com.sockywocky.createaddonorganizer.client.Presets.PresetData;
import com.sockywocky.createaddonorganizer.client.Presets.PresetRef;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.createaddonorganizer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PresetEditScreen extends Screen {
    private static final int PAD = 10;
    private static final int HEADER_H = 18;
    private static final int FOOTER_GAP = 6;

    private final Screen parent;
    private final PresetRef ref;

    private int chromeX;
    private int chromeY;
    private int chromeW;
    private int chromeH;
    private int headerY;
    private int dividerY;

    public PresetEditScreen(Screen parent, PresetRef ref) {
        super(Component.literal(ref.name()));
        this.parent = parent;
        this.ref = ref;
    }

    @Override
    protected void init() {
        int panelW = MenuLayout.panelWidth(this.width);
        int panelX = MenuLayout.panelX(this.width);
        boolean canSave = !ref.builtin() || DevMode.isUnlocked();
        boolean canDelete = !ref.builtin();
        int actions = 2 + (canSave ? 1 : 0) + (canDelete ? 1 : 0);

        chromeW = panelW + PAD * 2;
        chromeH = PAD * 2 + HEADER_H + actions * (MenuLayout.ROW_H + MenuLayout.GAP)
                + FOOTER_GAP + MenuLayout.ROW_H;
        chromeX = panelX - PAD;
        chromeY = (this.height - chromeH) / 2;
        headerY = chromeY + PAD;

        int y = headerY + HEADER_H;
        y = action(y, panelX, panelW, "createaddonorganizer.colors.presets.apply", this::apply,
                GlassButton.Style.BOX);
        y = action(y, panelX, panelW, "createaddonorganizer.colors.presets.export", this::exportPreset,
                GlassButton.Style.BOX);
        if (canSave) {
            y = action(y, panelX, panelW, "createaddonorganizer.colors.presets.saveCurrent", this::saveCurrent,
                    GlassButton.Style.BOX);
        }
        if (canDelete) {
            y = action(y, panelX, panelW, "createaddonorganizer.colors.presets.delete", this::confirmDelete,
                    GlassButton.Style.DANGER);
        }

        dividerY = y + 2;
        addRenderableWidget(new GlassButton(panelX, y + FOOTER_GAP, panelW, MenuLayout.ROW_H,
                Component.translatable("gui.done"), b -> onClose()).style(GlassButton.Style.ACCENT));
    }

    private int action(int y, int x, int width, String key, Runnable onPress, GlassButton.Style style) {
        addRenderableWidget(new GlassButton(x, y, width, MenuLayout.ROW_H, Component.translatable(key),
                b -> onPress.run()).style(style));
        return MenuLayout.nextRow(y);
    }

    private void apply() {
        PresetData data = Presets.load(ref.ref());
        if (data == null) {
            return;
        }
        Presets.applyToConfig(data);
        Presets.applyLive();
        Notice.show(Component.translatable("createaddonorganizer.colors.presets.applied", ref.name()), Notice.GREEN);
    }

    private void saveCurrent() {
        try {
            if (ref.builtin()) {
                Presets.overwriteBuiltin(ref.ref(), Presets.captureCurrent(ref.name()));
            } else {
                Presets.overwrite(ref.ref(), Presets.captureCurrent(ref.name()));
            }
            Notice.show(Component.translatable("createaddonorganizer.colors.presets.saveCurrent.success"), Notice.GREEN);
        } catch (Presets.DevWriteException e) {
            Notice.show(Component.translatable("createaddonorganizer.devmode.writeFailed", e.getMessage()), Notice.RED);
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to save preset {}", ref.ref(), e);
        }
    }

    private void exportPreset() {
        PresetData data = Presets.load(ref.ref());
        if (data == null) {
            return;
        }
        Presets.chooseExportFile(Presets.suggestedFileName(ref.name())).ifPresent(path -> {
            try {
                Presets.exportToFile(path, data);
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.export.success"), Notice.GREEN);
            } catch (IOException e) {
                createaddonorganizer.LOGGER.warn("[CAO] failed to export preset {}", ref.ref(), e);
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.export.failed", e.getMessage()), Notice.RED);
            }
        });
    }

    private void confirmDelete() {
        this.minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                Presets.delete(ref.ref());
                Notice.show(Component.translatable("createaddonorganizer.colors.presets.deleted"), Notice.RED);
                onClose();
            } else {
                this.minecraft.setScreen(this);
            }
        }, Component.translatable("createaddonorganizer.colors.presets.delete.title"),
                Component.translatable("createaddonorganizer.colors.presets.delete.message", ref.name())));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        GlassSkin.panel(g, chromeX, chromeY, chromeW, chromeH);
        GlassSkin.header(g, this.font, this.title, chromeX + PAD, headerY, chromeW - PAD * 2, 1f);
        GlassSkin.divider(g, chromeX + PAD, dividerY, chromeW - PAD * 2, 1f);
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> parent, Config.SWOOSH_BACK);
    }
}
