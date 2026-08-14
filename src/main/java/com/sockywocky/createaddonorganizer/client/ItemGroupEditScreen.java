package com.sockywocky.createaddonorganizer.client;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.TabLayout;

public class ItemGroupEditScreen extends Screen {
    private static final int PANEL_W = 240;
    private static final int ROW = 24;

    private final Screen returnTo;
    private final String groupId;
    private final Consumer<TabLayout> onApply;

    private TabLayout layout;
    private EditBox titleBox;
    private int panelX;
    private int panelY;

    public ItemGroupEditScreen(Screen returnTo, TabLayout layout, String groupId, Consumer<TabLayout> onApply) {
        super(Component.translatable("createaddonorganizer.group.title"));
        this.returnTo = returnTo;
        this.layout = layout;
        this.groupId = groupId;
        this.onApply = onApply;
    }

    private TabLayout.ItemGroup group() {
        return layout == null ? null : layout.itemGroup(groupId);
    }

    @Override
    protected void init() {
        TabLayout.ItemGroup group = group();
        if (group == null) {
            onClose();
            return;
        }
        panelX = (this.width - PANEL_W) / 2;
        panelY = Math.max(40, this.height / 2 - 78);

        titleBox = new EditBox(this.font, panelX, panelY + 26, PANEL_W, 18,
                Component.translatable("createaddonorganizer.group.name"));
        titleBox.setMaxLength(48);
        titleBox.setValue(group.displayTitle());
        titleBox.setResponder(value -> apply(group().withTitle(value)));
        addRenderableWidget(titleBox);

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.group.icon"),
                        b -> pickIcon())
                .bounds(panelX, panelY + 26 + ROW + 20, PANEL_W, 20)
                .tooltip(Tooltip.create(Component.translatable("createaddonorganizer.group.icon.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.group.iconFirst"),
                        b -> apply(group().withIcon(null)))
                .bounds(panelX, panelY + 26 + ROW * 2 + 20, PANEL_W / 2 - 2, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("createaddonorganizer.tabs.ungroup"),
                        b -> dissolve())
                .bounds(panelX + PANEL_W / 2 + 2, panelY + 26 + ROW * 2 + 20, PANEL_W / 2 - 2, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(panelX, panelY + 26 + ROW * 3 + 24, PANEL_W, 20).build());
    }

    private void pickIcon() {
        this.minecraft.setScreen(new ItemPickerScreen(this, id -> {
            TabLayout.ItemGroup group = group();
            if (group != null) {
                apply(group.withIcon(id));
            }
        }));
    }

    private void apply(TabLayout.ItemGroup updated) {
        if (updated == null || layout == null) {
            return;
        }
        layout = layout.withGroupChanged(updated);
        onApply.accept(layout);
    }

    private void dissolve() {
        if (layout == null) {
            return;
        }
        layout = layout.withGroupDissolved(groupId);
        onApply.accept(layout);
        Sfx.snap();
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void onClose() {
        ScreenSwoosh.surface(() -> returnTo, Config.SWOOSH_BACK);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        TabLayout.ItemGroup group = group();
        if (group == null) {
            return;
        }
        g.drawCenteredString(this.font, this.title, this.width / 2, panelY - 22, 0xFFFFFFFF);

        List<String> members = layout.membersOf(groupId);
        g.drawString(this.font, Component.translatable("createaddonorganizer.group.name"),
                panelX, panelY + 14, MenuSkin.bodyColor(0xFF8A9AA8));

        int previewY = panelY + 26 + ROW + 2;
        g.drawString(this.font, Component.translatable("createaddonorganizer.group.members", members.size()),
                panelX, previewY - 12, MenuSkin.bodyColor(0xFF8A9AA8));

        String iconId = layout.iconItemOf(groupId);
        int x = panelX;
        for (String member : members) {
            if (x + 18 > panelX + PANEL_W) {
                break;
            }
            TabEditorScreen.drawCell(g, x, previewY, member.equals(iconId));
            ItemStack stack = ItemLibrary.stackOf(member);
            SafeIcon.render(g, stack, x + 1, previewY + 1);
            x += 18;
        }
    }
}
