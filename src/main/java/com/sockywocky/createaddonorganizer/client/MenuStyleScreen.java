package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.sockywocky.createaddonorganizer.Config;

public class MenuStyleScreen extends Screen implements EmbeddedPane {

    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 20;
    private static final int ROW_GAP = 30;

    private final Screen returnTo;
    private final List<Button> styleButtons = new ArrayList<>();
    private CycleButton<Boolean> transparencyButton;
    private Button accentArrow;
    private int firstRowY;
    private int accentRowY;
    private int buttonW = BUTTON_W;
    private boolean embedded;
    private int embedX;
    private int embedY;
    private int embedW;
    private int embedH;
    private Runnable onEmbeddedDone;
    private static boolean accentExpanded;

    @Override
    public void embedInto(int x, int y, int width, int height, Runnable onDone) {
        this.embedded = true;
        this.embedX = x;
        this.embedY = y;
        this.embedW = width;
        this.embedH = height;
        this.onEmbeddedDone = onDone;
    }

    private int centerX() {
        return embedded ? embedX + embedW / 2 : this.width / 2;
    }

    private int centerY() {
        return embedded ? embedY + embedH / 2 : this.height / 2;
    }

    private int doneY() {
        return embedded ? embedY + embedH - 28 : this.height - 28;
    }

    public MenuStyleScreen(Screen returnTo) {
        super(Component.translatable("createaddonorganizer.style.title"));
        this.returnTo = returnTo;
    }

    private static String key(Config.MenuStyle style) {
        return "createaddonorganizer.style." + style.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void init() {
        styleButtons.clear();
        accentArrow = null;
        Config.MenuStyle[] styles = Config.MenuStyle.values();
        firstRowY = centerY() - (styles.length * ROW_GAP) / 2 - 10;
        buttonW = embedded ? Math.max(80, Math.min(BUTTON_W, embedW - 24)) : BUTTON_W;

        int x = centerX() - buttonW / 2;
        for (int i = 0; i < styles.length; i++) {
            Config.MenuStyle style = styles[i];
            Button button = Button.builder(Component.translatable(key(style)), b -> select(style))
                    .bounds(x, firstRowY + i * ROW_GAP, buttonW, BUTTON_H)
                    .build();
            button.active = Config.menuStyle() != style;
            styleButtons.add(addRenderableWidget(button));

        }

        accentRowY = firstRowY + styles.length * ROW_GAP + 8;
        int toggleX = x + BUTTON_H + 4;
        int toggleW = buttonW - BUTTON_H - 4;

        accentArrow = addRenderableWidget(Button.builder(Component.empty(), b -> toggleAccent())
                .bounds(x, accentRowY, BUTTON_H, BUTTON_H)
                .build());

        transparencyButton = addRenderableWidget(CycleButton.onOffBuilder(Config.menuStyleTransparent())
                .create(toggleX, accentRowY, toggleW, BUTTON_H,
                        Component.translatable("createaddonorganizer.style.transparency"),
                        (button, value) -> Config.setMenuStyleTransparent(value)));
        transparencyButton.active = Config.menuStyle() != Config.MenuStyle.DEFAULT;

        if (accentArrow != null && accentExpanded) {
            addRenderableWidget(new AccentHueSlider(x, sliderY(), buttonW, 14));
            addRenderableWidget(new AccentSaturationSlider(x, saturationSliderY(), buttonW, 14));
        }

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x, doneY(), buttonW, BUTTON_H).build());
    }

    private int sliderY() {
        return accentRowY + BUTTON_H + 6;
    }

    private int saturationSliderY() {
        return sliderY() + 18;
    }

    private void toggleAccent() {
        accentExpanded = !accentExpanded;
        if (!accentExpanded) {
            Config.setMenuAccentHue(Config.MENU_ACCENT_DEFAULT);
            Config.resetMenuAccentSaturation();
        }
        rebuildWidgets();
    }

    private void select(Config.MenuStyle style) {
        if (Config.menuStyle() == style) {
            return;
        }
        Config.setMenuStyle(style);
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        if (embedded) {
            onEmbeddedDone.run();
            return;
        }
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!embedded) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, centerX(), firstRowY - 30,
                MenuSkin.titleColor(0xFFFFFFFF));
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.style.hint"),
                centerX(), firstRowY - 16, MenuSkin.mutedColor(0xFFAAAAAA));

        if (accentArrow != null) {
            int argb = MenuSkin.buttonTextColor(true, accentArrow.isHoveredOrFocused());
            int cx = accentArrow.getX() + accentArrow.getWidth() / 2;
            int cy = accentArrow.getY() + accentArrow.getHeight() / 2;
            if (!MenuSkin.arrowIcon(g, cx, cy, accentExpanded ? 90f : 0f, argb)) {
                g.drawCenteredString(this.font, accentExpanded ? "v" : ">", cx, cy - 4, argb);
            }
            if (accentExpanded) {
                int sliderY = sliderY();
                g.fill(centerX() + buttonW / 2 + 8, sliderY, centerX() + buttonW / 2 + 22,
                        sliderY + 14, MenuSkin.accentSwatch());
                int saturationY = saturationSliderY();
                g.drawString(this.font, Component.literal(Config.menuAccentSaturation() + "%"),
                        centerX() + buttonW / 2 + 8, saturationY + 3,
                        MenuSkin.mutedColor(0xFFAAAAAA));
            }
        }

        Config.MenuStyle current = Config.menuStyle();
        Config.MenuStyle[] styles = Config.MenuStyle.values();
        for (int i = 0; i < styles.length && i < styleButtons.size(); i++) {
            if (styles[i] != current) {
                continue;
            }
            Button button = styleButtons.get(i);
            g.drawString(this.font, Component.translatable("createaddonorganizer.style.current"),
                    button.getX() + button.getWidth() + 8, button.getY() + 6,
                    MenuSkin.mutedColor(0xFFAAAAAA));
        }
    }
}
