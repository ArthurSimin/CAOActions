package com.sockywocky.createaddonorganizer;

import com.mojang.blaze3d.platform.InputConstants;
import com.sockywocky.createaddonorganizer.client.ClearCacheCooldown;
import com.sockywocky.createaddonorganizer.client.ClientRegistries;
import com.sockywocky.createaddonorganizer.client.DevMode;
import com.sockywocky.createaddonorganizer.client.FpsMonitor;
import com.sockywocky.createaddonorganizer.client.IconAtlas;
import com.sockywocky.createaddonorganizer.client.LoadingSpinner;
import com.sockywocky.createaddonorganizer.client.MenuPixels;
import com.sockywocky.createaddonorganizer.Config;
import com.sockywocky.createaddonorganizer.client.MenuSkin;
import com.sockywocky.createaddonorganizer.client.ScreenSwoosh;
import com.sockywocky.createaddonorganizer.client.TextEditScreen;
import com.sockywocky.createaddonorganizer.client.TextEditorOverlay;
import com.sockywocky.createaddonorganizer.client.ModBannerCatalog;
import com.sockywocky.createaddonorganizer.client.Notice;
import com.sockywocky.createaddonorganizer.client.RemoteBannerPools;
import com.sockywocky.createaddonorganizer.client.RemoteBanners;
import com.sockywocky.createaddonorganizer.client.RemoteBoxTextures;
import com.sockywocky.createaddonorganizer.client.RemoteCache;
import com.sockywocky.createaddonorganizer.client.SectionColorsScreen;
import com.sockywocky.createaddonorganizer.client.TabEditorScreen;
import com.sockywocky.createaddonorganizer.client.UpdateCheck;
import com.sockywocky.createaddonorganizer.mixin.CreativeModeInventoryScreenAccessor;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = createaddonorganizer.MODID, dist = Dist.CLIENT)
public class createaddonorganizerClient {

    private static final int REQUIRED_STABLE_TICKS = 5;

    private static final int ARROW_W = 16;
    private static final int ARROW_H = 28;
    private static final int ARROW_MARGIN = 8;
    private static final int CLEAR_CACHE_W = 100;
    private static final int CLEAR_CACHE_H = 20;
    private static final int CLEAR_CACHE_MARGIN = 6;

    private static final int MAX_ORGANIZE_ATTEMPTS = 10;

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping("key.createaddonorganizer.openConfig",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.createaddonorganizer");

    private static final KeyMapping EDIT_TAB_KEY = new KeyMapping("key.createaddonorganizer.editTab",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.createaddonorganizer");

    private static final KeyMapping EDIT_TEXT_KEY = new KeyMapping("key.createaddonorganizer.editText",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F6, "key.categories.createaddonorganizer");

    private static final KeyMapping TEXT_OVERLAY_KEY = new KeyMapping("key.createaddonorganizer.textOverlay",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F7, "key.categories.createaddonorganizer");

    private static Button clearCacheButton;
    private static String clearCacheLabel;

    private static boolean done = false;
    private static boolean remoteSyncStarted = false;
    private static ClientLevel lastSeenLevel;
    private static int stableTicks = 0;
    private static int organizeAttempts = 0;

    public createaddonorganizerClient(ModContainer container, IEventBus modEventBus) {

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> {
                    ScreenSwoosh.reveal(Config.SWOOSH_CONFIG_MENU);
                    return new SectionColorsScreen(parent, modContainer);
                });

        modEventBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.register(OPEN_CONFIG_KEY);
            event.register(EDIT_TAB_KEY);
            event.register(EDIT_TEXT_KEY);
            event.register(TEXT_OVERLAY_KEY);
        });
        modEventBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) manager -> {
                    IconAtlas.invalidate();
                    AddonGroups.invalidate();
                    MenuPixels.invalidate();
                    ModBannerCatalog.invalidate();
                }));

        NeoForge.EVENT_BUS.addListener(createaddonorganizerClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(createaddonorganizerClient::onScreenRender);
        NeoForge.EVENT_BUS.addListener(createaddonorganizerClient::onHudRender);
        NeoForge.EVENT_BUS.addListener(createaddonorganizerClient::onScreenInit);
        NeoForge.EVENT_BUS.addListener(createaddonorganizerClient::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre event) ->
                MenuSkin.bumpCog(event.getScrollDeltaY()));
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        clearCacheButton = null;
        clearCacheLabel = null;
        if (!MenuSkin.isModConfigScreen(screen)) {
            return;
        }
        boolean root = screen instanceof ConfigurationScreen || DevMode.isTopConfigSection(screen);
        if (root) {
            clearCacheButton = Button.builder(
                            Component.translatable("createaddonorganizer.colors.credits.clearCache"),
                            b -> RemoteCache.clearAllReporting())
                    .bounds(CLEAR_CACHE_MARGIN, CLEAR_CACHE_MARGIN, CLEAR_CACHE_W, CLEAR_CACHE_H)
                    .build();
            refreshClearCacheButton();
            event.addListener(clearCacheButton);
        }
        event.addListener(Button.builder(Component.literal(">"), b -> screen.onClose())
                .bounds(screen.width - ARROW_W - ARROW_MARGIN, screen.height / 2 - ARROW_H / 2, ARROW_W, ARROW_H)
                .tooltip(Tooltip.create(root
                        ? Component.translatable("createaddonorganizer.colors.title")
                        : Component.translatable("gui.back")))
                .build());
    }

    private static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (handleTextEditorKey(event.getScreen(), event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        if (screen.getFocused() instanceof EditBox box && box.canConsumeInput()) {
            return;
        }
        if (!EDIT_TAB_KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            return;
        }
        CreativeModeTab tab = CreativeModeInventoryScreenAccessor.getSelectedTab();
        ResourceLocation tabId = tab == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (tabId == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new TabEditorScreen(screen, tabId));
        event.setCanceled(true);
    }

    private static boolean handleTextEditorKey(Screen screen, int keyCode, int scanCode) {
        if (screen instanceof TextEditScreen || !TextEditorOverlay.enabled()) {
            return false;
        }
        InputConstants.Key key = InputConstants.getKey(keyCode, scanCode);
        if (EDIT_TEXT_KEY.isActiveAndMatches(key)) {
            TextEditorOverlay.editHovered();
            return true;
        }
        if (TEXT_OVERLAY_KEY.isActiveAndMatches(key)) {
            TextEditorOverlay.toggleBoxes();
            return true;
        }
        return false;
    }

    private static void refreshClearCacheButton() {
        if (clearCacheButton == null) {
            return;
        }
        boolean ready = ClearCacheCooldown.ready();
        String label = ready ? "" : ClearCacheCooldown.remainingLabel();
        if (label.equals(clearCacheLabel)) {
            return;
        }
        clearCacheLabel = label;
        clearCacheButton.active = ready;
        clearCacheButton.setTooltip(Tooltip.create(ready
                ? Component.translatable("createaddonorganizer.colors.credits.clearCache.tooltip")
                : Component.translatable("createaddonorganizer.colors.credits.clearCache.cooldown",
                        ClearCacheCooldown.remainingLabel())));
    }

    private static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        refreshClearCacheButton();
        MenuSkin.endFrame();
        LoadingSpinner.renderPreview(event.getGuiGraphics(), mc);
        Notice.render(event.getGuiGraphics(), mc);
        FpsMonitor.render(event.getGuiGraphics(), mc);
        TextEditorOverlay.render(event.getGuiGraphics(), mc);
    }

    private static void onHudRender(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            FpsMonitor.render(event.getGuiGraphics(), mc);
            TextEditorOverlay.render(event.getGuiGraphics(), mc);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_CONFIG_KEY.consumeClick()) {
            ModList.get().getModContainerById(createaddonorganizer.MODID)
                    .ifPresent(modContainer -> {
                        ScreenSwoosh.reveal(Config.SWOOSH_CONFIG_MENU);
                        mc.setScreen(new SectionColorsScreen(mc.screen, modContainer));
                    });
        }
        ScreenSwoosh.update();
        DevMode.tick(mc);
        TextEditorOverlay.tick();
        while (EDIT_TEXT_KEY.consumeClick()) {
            TextEditorOverlay.editHovered();
        }
        while (TEXT_OVERLAY_KEY.consumeClick()) {
            TextEditorOverlay.toggleBoxes();
        }
        LoadingSpinner.tickPreview(mc);
        MenuSkin.tickCog();
        if (!remoteSyncStarted) {
            remoteSyncStarted = true;
            ClientRegistries.warmUp();
            RemoteBanners.loadCacheFromDisk();
            RemoteBanners.syncAsync();
            RemoteBannerPools.loadCacheFromDisk();
            RemoteBannerPools.syncAsync();
            RemoteBoxTextures.loadCacheFromDisk();
            RemoteBoxTextures.syncAsync();
            UpdateCheck.syncAsync();
        }
        if (done) {
            return;
        }
        if (mc.level == null || mc.player == null) {
            lastSeenLevel = null;
            stableTicks = 0;
            return;
        }
        if (mc.level == lastSeenLevel) {
            stableTicks++;
        } else {
            lastSeenLevel = mc.level;
            stableTicks = 1;
        }
        if (stableTicks < REQUIRED_STABLE_TICKS) {
            return;
        }

        organizeAttempts++;
        boolean succeeded = createaddonorganizer.organize(ClientRegistries.displayParams());
        if (succeeded || organizeAttempts >= MAX_ORGANIZE_ATTEMPTS) {
            done = true;
        } else {
            stableTicks = 0;
        }
    }
}
