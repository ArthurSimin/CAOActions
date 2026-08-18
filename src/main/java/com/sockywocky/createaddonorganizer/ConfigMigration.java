package com.sockywocky.createaddonorganizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;

import net.neoforged.fml.loading.FMLPaths;

public final class ConfigMigration {

    private static final String FILE_NAME = createaddonorganizer.MODID + "-common.toml";

    private static final Map<String, String> MOVES = new LinkedHashMap<>();

    static {
        move("classicOrganizerLayout", "menus");
        move("showCollapseToggle", "creative");
        move("stickySectionBanners", "creative");
        move("cacheItemIcons", "performance");
        move("menuFramerate", "performance");
        move("editorHintSeen", "saved");
        move("bannerEditorPreviewTop", "menus");
        move("gradientCellSize", "menus");

        moveFrom("appearance", "defaultBannerColor", "appearance.banner");
        moveFrom("appearance", "defaultBannerGradient", "appearance.banner");
        moveFrom("appearance", "showAllBanners", "appearance.banner");
        moveFrom("appearance", "tintedTextBox", "appearance.box");
        moveFrom("appearance", "defaultBoxColor", "appearance.box");
        moveFrom("appearance", "defaultBoxDarken", "appearance.box");
        moveFrom("appearance", "defaultBoxOpacity", "appearance.box");
        moveFrom("appearance", "defaultTextColor", "appearance.text");
        moveFrom("appearance", "defaultTextGradient", "appearance.text");
        moveFrom("appearance", "twoToneText", "appearance.text");
        moveFrom("appearance", "defaultTextSecondaryColor", "appearance.text");
        moveFrom("appearance", "defaultTextSecondaryGradient", "appearance.text");
        moveFrom("appearance", "defaultTwoToneSplit", "appearance.text");
        moveFrom("appearance", "defaultScrollCutoff", "appearance.text");
        moveFrom("appearance", "titleTextShadow", "appearance.text");
        moveFrom("appearance", "defaultTextShadowColor", "appearance.text");
        moveFrom("appearance", "defaultTextOutlineColor", "appearance.text");
        moveFrom("appearance", "defaultTextOutlineGradient", "appearance.text");

        moveFrom("appearance", "fetchOnlineBanners", "online");
        moveFrom("appearance", "bannerManifestUrl", "online");
        moveFrom("appearance", "bannerPoolsManifestUrl", "online");
        moveFrom("appearance", "fetchOnlineBoxTextures", "online");
        moveFrom("appearance", "boxManifestUrl", "online");

        moveFrom("appearance", "sectionColors", "saved");
        moveFrom("appearance", "banners", "saved");
        moveFrom("appearance", "animatedBanners", "saved");
        moveFrom("appearance", "extraBannerPool", "saved");
        moveFrom("appearance", "boxColors", "saved");
        moveFrom("appearance", "boxTextures", "saved");
        moveFrom("appearance", "boxDarkens", "saved");
        moveFrom("appearance", "boxOpacities", "saved");
        moveFrom("appearance", "textColors", "saved");
        moveFrom("appearance", "textSecondaryColors", "saved");
        moveFrom("appearance", "textSplits", "saved");
        moveFrom("appearance", "scrollCutoffs", "saved");
        moveFrom("appearance", "titleTextShadowSections", "saved");
        moveFrom("appearance", "textOutlineColors", "saved");
        moveFrom("appearance", "textShadowColors", "saved");
        moveFrom("appearance", "highlightColors", "saved");

        moveFrom("organization", "sectionOrder", "saved");
        moveFrom("organization", "sectionNames", "saved");
        moveFrom("organization", "collapsedSections", "saved");

        moveFrom("interface", "indexPanelStyle", "creative");
        moveFrom("interface", "arrangerStyle", "menus");
        moveFrom("interface", "menuStyle", "menus");
        moveFrom("interface", "menuStyleTransparent", "menus");
        moveFrom("interface", "menuAccentHue", "menus");
        moveFrom("interface", "arrangerLayout", "menus");
        moveFrom("interface", "tabOrder", "saved");

        MOVES.put("sounds.enabled", "sounds.allSounds");
        MOVES.put("animations.enabled", "animations.allAnimations");
    }

    private ConfigMigration() {}

    private static void move(String key, String newSection) {
        MOVES.put(key, newSection + "." + key);
    }

    private static void moveFrom(String oldSection, String key, String newSection) {
        MOVES.put(oldSection + "." + key, newSection + "." + key);
    }

    private static final String MENU_FRAMERATE = "performance.menuFramerate";
    private static final int RETIRED_MENU_FRAMERATE = 120;

    private static boolean retireMenuFramerateBoost(CommentedFileConfig config) {
        Object raw = config.getRaw(MENU_FRAMERATE);
        if (!(raw instanceof Number number) || number.intValue() != RETIRED_MENU_FRAMERATE) {
            return false;
        }
        config.set(MENU_FRAMERATE, 60);
        createaddonorganizer.LOGGER.info("[CAO] menuFramerate was still at the old default of {}; it now defaults "
                + "to vanilla's 60 because raising it can make menus beat against V-Sync -- set it back if you "
                + "prefer the smoother menus", RETIRED_MENU_FRAMERATE);
        return true;
    }

    public static void run() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (CommentedFileConfig config = CommentedFileConfig.builder(file).sync().build()) {
            config.load();
            int moved = 0;
            for (Map.Entry<String, String> entry : MOVES.entrySet()) {
                if (!config.contains(entry.getKey()) || config.contains(entry.getValue())) {
                    continue;
                }
                config.set(entry.getValue(), config.getRaw(entry.getKey()));
                config.remove(entry.getKey());
                moved++;
            }
            boolean retired = retireMenuFramerateBoost(config);
            if (moved > 0 || retired) {
                config.save();
            }
            if (moved > 0) {
                createaddonorganizer.LOGGER.info("[CAO] moved {} setting(s) into the reorganised config layout",
                        moved);
            }
        } catch (Throwable t) {
            createaddonorganizer.LOGGER.warn("[CAO] could not migrate {} to the new layout; settings that moved "
                    + "section will fall back to their defaults", FILE_NAME, t);
        }
    }
}
