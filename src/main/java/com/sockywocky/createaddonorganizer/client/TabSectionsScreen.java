package com.sockywocky.createaddonorganizer.client;

import java.util.ArrayList;
import java.util.List;

import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import com.sockywocky.createaddonorganizer.Config;

public class TabSectionsScreen extends Screen {

    private static final int CELL = 18;
    private static final int HEADER_H = 20;

    private final Screen returnTo;
    private final ResourceLocation tabId;
    private final Component tabName;

    private final List<Block> blocks = new ArrayList<>();

    private int areaX;
    private int areaY;
    private int areaW;
    private int areaH;
    private final Scrollbar bar = new Scrollbar().width(6).step(CELL);
    private int contentHeight;

    private record Block(ResourceLocation id, Component title, List<ItemStack> items, int y, int height) {}

    public TabSectionsScreen(Screen returnTo, ResourceLocation tabId, Component tabName) {
        super(Component.translatable("createaddonorganizer.sections.title", tabName));
        this.returnTo = returnTo;
        this.tabId = tabId;
        this.tabName = tabName;
    }

    @Override
    protected void init() {
        areaX = 12;
        areaY = 40;
        areaW = this.width - 24;
        areaH = this.height - areaY - 34;

        addRenderableWidget(Button.builder(Component.literal("<"), b -> onClose())
                .bounds(8, 8, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());

        rebuild();
    }

    private void rebuild() {
        blocks.clear();
        int cols = Math.max(1, (areaW - 12) / CELL);
        int y = 0;

        List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(tabId);
        if (sections != null && !sections.isEmpty()) {
            for (Section<?> section : sections) {
                List<ItemStack> stacks = stacksOf(section);
                int rows = Math.max(1, (stacks.size() + cols - 1) / cols);
                int h = HEADER_H + rows * CELL + 4;
                blocks.add(new Block(section.id(), CaoSection.titleOf(section), stacks, y, h));
                y += h;
            }
        } else {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(tabId);
            List<ItemStack> stacks = tab == null || tab.getDisplayItems() == null
                    ? List.of() : new ArrayList<>(tab.getDisplayItems());
            int rows = Math.max(1, (stacks.size() + cols - 1) / cols);
            int h = HEADER_H + rows * CELL + 4;
            blocks.add(new Block(tabId, tabName, stacks, y, h));
            y += h;
        }
        contentHeight = y;
        bar.bounds(areaX + areaW - 6, areaY, areaH);
        bar.content(contentHeight);
    }

    private static List<ItemStack> stacksOf(Section<?> section) {
        try {
            if (section instanceof CaoSection cao) {
                List<ItemStack> stacks = cao.items().getStacks();
                if (stacks != null && !stacks.isEmpty()) {
                    return stacks;
                }
            }
        } catch (Throwable ignored) {
            return List.of();
        }
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(section.id());
        if (tab != null && tab.getDisplayItems() != null) {
            return new ArrayList<>(tab.getDisplayItems());
        }
        return List.of();
    }

    private int editX() {
        return areaX + areaW - 12
                - this.font.width(Component.translatable("createaddonorganizer.sections.editItems"));
    }

    private Block blockAt(double mouseY) {
        int localY = (int) mouseY - areaY + bar.displayed();
        for (Block block : blocks) {
            if (localY >= block.y() && localY < block.y() + block.height()) {
                return block;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && bar.mouseClicked(mouseX, mouseY)) {
            return true;
        }
        if (button == 0 && mouseX >= areaX && mouseX < areaX + areaW
                && mouseY >= areaY && mouseY < areaY + areaH) {
            Block block = blockAt(mouseY);
            if (block != null) {
                int localY = (int) mouseY - areaY + bar.displayed();
                if (localY < block.y() + HEADER_H) {
                    if (mouseX >= editX()) {
                        this.minecraft.setScreen(new TabEditorScreen(this, block.id()));
                    } else {
                        this.minecraft.setScreen(
                                new ColorPickerScreen(this, block.id(), block.title(), false));
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return bar.wheel(scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (bar.mouseDragged(mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        bar.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(returnTo);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        int totalItems = 0;
        for (Block block : blocks) {
            totalItems += block.items().size();
        }
        g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.sections.summary",
                blocks.size(), totalItems), this.width / 2, 26, 0xFFAAAAAA);
        if (this.minecraft.level == null) {
            g.drawCenteredString(this.font, Component.translatable("createaddonorganizer.tabs.noWorld"),
                    this.width / 2, this.height - 40, 0xFFFFAA55);
        }

        g.fill(areaX, areaY, areaX + areaW, areaY + areaH, 0x8C000000);
        g.enableScissor(areaX, areaY, areaX + areaW, areaY + areaH);

        int cols = Math.max(1, (areaW - 12) / CELL);
        int offset = bar.offset();
        ItemStack hovered = ItemStack.EMPTY;
        for (Block block : blocks) {
            int by = areaY + block.y() - offset;
            if (by + block.height() < areaY || by > areaY + areaH) {
                continue;
            }
            int bannerW = Math.min(areaW - 8, 220);
            boolean onHeaderRow = mouseY >= by && mouseY < by + 16;
            boolean editHover = onHeaderRow && mouseX >= editX();
            boolean headerHover = onHeaderRow && !editHover
                    && mouseX >= areaX + 4 && mouseX < areaX + 4 + bannerW;
            g.fill(areaX + 4, by, areaX + 4 + bannerW, by + 16,
                    0xFF000000 | (Config.bannerColorFor(block.id()).color1() & 0x00FFFFFF));
            if (headerHover) {
                g.fill(areaX + 4, by, areaX + 4 + bannerW, by + 16, 0x30FFFFFF);
            }
            g.drawString(this.font, block.title(), areaX + 8, by + 4,
                    Config.textColorFor(block.id()).color1());
            Component edit = Component.translatable("createaddonorganizer.sections.edit");
            g.drawString(this.font, edit, areaX + 4 + bannerW + 6, by + 4, headerHover ? 0xFFFFE37A : 0xFF808080);
            Component editItems = Component.translatable("createaddonorganizer.sections.editItems");
            g.drawString(this.font, editItems, editX(), by + 4, editHover ? 0xFFFFE37A : 0xFF9AA8B4);

            for (int i = 0; i < block.items().size(); i++) {
                int ix = areaX + 4 + (i % cols) * CELL;
                int iy = by + HEADER_H + (i / cols) * CELL;
                if (iy + CELL < areaY || iy > areaY + areaH) {
                    continue;
                }
                g.fill(ix, iy, ix + CELL, iy + CELL, 0x30000000);
                g.fill(ix, iy, ix + CELL, iy + 1, 0x30FFFFFF);
                g.fill(ix, iy, ix + 1, iy + CELL, 0x30FFFFFF);
                ItemStack stack = block.items().get(i);
                SafeIcon.render(g, stack, ix + 1, iy + 1);
                if (mouseX >= ix && mouseX < ix + CELL && mouseY >= iy && mouseY < iy + CELL) {
                    g.fill(ix + 1, iy + 1, ix + CELL - 1, iy + CELL - 1, 0x60FFFFFF);
                    hovered = stack;
                }
            }
        }
        g.disableScissor();

        bar.render(g, mouseX, mouseY);

        if (!hovered.isEmpty() && mouseY >= areaY && mouseY < areaY + areaH) {
            g.renderTooltip(this.font, hovered.getHoverName(), mouseX, mouseY);
        }
    }
}
