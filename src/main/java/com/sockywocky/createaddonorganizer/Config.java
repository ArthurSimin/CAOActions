package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sockywocky.createaddonorganizer.client.BannerTextures;
import com.sockywocky.createaddonorganizer.client.ColorSpec;
import com.sockywocky.createaddonorganizer.client.ColorUtil;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final Set<String> BUILTIN_INCLUDE = Set.of(
            "bits_n_bobs:bnb_based",
            "bits_n_bobs:bnb_palettes",
            "bits_n_bobs:bnb_deco",
            "create_more_automation:create_more_automation");

    private static final Map<String, String> BUILTIN_ROUTES = Map.of(
            "bits_n_bobs:bnb_palettes", "create:palettes",
            "bits_n_bobs:bnb_deco", "create:palettes",
            "railways:palettes", "create:palettes");

    private static final Set<String> BUILTIN_EXCLUDE = Set.of();

    static {
        BUILDER.comment("Which addon tabs get absorbed into Create, and where.")
                .push("absorption");
    }

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_INCLUDE = BUILDER
            .comment("Tab IDs to always absorb under Create, even without a Create dependency.")
            .defineListAllowEmpty("forceInclude", List.of(), () -> "somemod:main", Config::isValidTabId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> FORCE_EXCLUDE = BUILDER
            .comment("Tab IDs to never absorb; they keep their own standalone tab.")
            .defineListAllowEmpty("forceExclude", List.of(), () -> "somemod:main", Config::isValidTabId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ROUTES = BUILDER
            .comment("Fold a tab into a chosen Create parent tab. Format: \"<addonTabId> > <parentTabId>\",",
                    "e.g. \"somemod:deco > create:palettes\".")
            .defineListAllowEmpty("routes", List.of(), () -> "somemod:deco > create:palettes", Config::isValidRoute);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_MAIN_SECTIONS = BUILDER
            .comment("Tabs promoted to hub status (can have others folded into them) even without routes",
                    "pointing at them yet. Managed via shift+\"+\" in the creative menu.")
            .defineListAllowEmpty("extraMainSections", List.of(), () -> "somemod:main", Config::isValidTabId);

    static {
        BUILDER.pop();
        BUILDER.comment("Default look of section banners, their contrast boxes, and their title text.",
                        "Per-section overrides made in the editors are stored under \"saved\".")
                .push("appearance");
    }

    public static final ModConfigSpec.BooleanValue RAINBOW_MODE = BUILDER
            .comment("Compute banner/text colours live as a red-to-violet gradient by tab position, instead",
                    "of using the manual colour lists. Enabled by the Rainbow preset.")
            .define("rainbowMode", false);

    static {
        BUILDER.comment("The coloured bar drawn at the top of each section.")
                .push("banner");
    }

    public static final ModConfigSpec.IntValue DEFAULT_BANNER_COLOR = BUILDER
            .comment("Default section banner colour (ARGB int).")
            .defineInRange("defaultBannerColor", 0xFF262626, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_BANNER_GRADIENT = BUILDER
            .comment("Optional gradient for the default banner, as \"<secondHex>|<DIRECTION>|<STYLE>\"",
                    "(DIRECTION: VERTICAL/HORIZONTAL/DIAGONAL_UP/DIAGONAL_DOWN, STYLE: SMOOTH/DITHER_2X2/",
                    "DITHER_4X4/DITHER_TRICOLOR/DITHER_QUADCOLOR). Empty (default) means a flat colour.")
            .define("defaultBannerGradient", "");

    public static final ModConfigSpec.BooleanValue SHOW_ALL_BANNERS = BUILDER
            .comment("Ignore curated banner pools and always show the full gallery.")
            .define("showAllBanners", false);

    static {
        BUILDER.pop();
        BUILDER.comment("The contrast box drawn behind a section's title text.")
                .push("box");
    }

    public static final ModConfigSpec.BooleanValue TINTED_TEXT_BOX = BUILDER
            .comment("Draw a semi-transparent box behind section title text for contrast.")
            .define("tintedTextBox", true);

    public static final ModConfigSpec.IntValue DEFAULT_BOX_COLOR = BUILDER
            .comment("Default tinted-box colour (ARGB int).")
            .defineInRange("defaultBoxColor", 0x64000000, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue DEFAULT_BOX_DARKEN = BUILDER
            .comment("How much to darken a per-section contrast-box IMAGE when rendered (0 = no darkening,",
                    "1 = fully black). Only applies while a box texture is set.")
            .defineInRange("defaultBoxDarken", 0.0, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_BOX_OPACITY = BUILDER
            .comment("Opacity of a per-section contrast-box IMAGE when rendered (0 = fully transparent,",
                    "1 = fully opaque). Only applies while a box texture is set.")
            .defineInRange("defaultBoxOpacity", 1.0, 0.0, 1.0);

    static {
        BUILDER.pop();
        BUILDER.comment("Section title text: colour, shading, outline and shadow.")
                .push("text");
    }

    public static final ModConfigSpec.IntValue DEFAULT_TEXT_COLOR = BUILDER
            .comment("Default section title text colour (ARGB int).")
            .defineInRange("defaultTextColor", 0xFFFFFFFF, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_TEXT_GRADIENT = BUILDER
            .comment("Optional gradient for the default text colour, as \"<secondHex>|<DIRECTION>\".",
                    "Text gradients always render smooth.")
            .define("defaultTextGradient", "");

    public static final ModConfigSpec.BooleanValue TWO_TONE_TEXT = BUILDER
            .comment("Shade title text two-tone: primary colour on top, secondary on bottom of each glyph.")
            .define("twoToneText", true);

    public static final ModConfigSpec.IntValue DEFAULT_TEXT_SECONDARY_COLOR = BUILDER
            .comment("Default secondary text colour (ARGB int).")
            .defineInRange("defaultTextSecondaryColor", 0xFFCCCCCC, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_TEXT_SECONDARY_GRADIENT = BUILDER
            .comment("Optional gradient for the default secondary text colour, as \"<secondHex>|<DIRECTION>\".")
            .define("defaultTextSecondaryGradient", "");

    public static final ModConfigSpec.DoubleValue DEFAULT_TWO_TONE_SPLIT = BUILDER
            .comment("Default vertical split of two-tone text, as a fraction of glyph height from the top",
                    "(0 = secondary, 1 = primary).")
            .defineInRange("defaultTwoToneSplit", 0.55, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_SCROLL_CUTOFF = BUILDER
            .comment("Fraction of the banner's title width text must exceed before scrolling. 1.0 (default)",
                    "scrolls only on true overflow; lower values make shorter titles scroll too.")
            .defineInRange("defaultScrollCutoff", 1.0, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue TITLE_TEXT_SHADOW = BUILDER
            .comment("Draw title text with the vanilla drop shadow by default. Per-section overrides take",
                    "precedence.")
            .define("titleTextShadow", true);

    public static final ModConfigSpec.IntValue DEFAULT_TEXT_SHADOW_COLOR = BUILDER
            .comment("Default custom drop-shadow colour (ARGB int), used once a section's shadow is",
                    "unlinked from its text colour.")
            .defineInRange("defaultTextShadowColor", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue DEFAULT_TEXT_OUTLINE_COLOR = BUILDER
            .comment("Default title text outline colour (ARGB int). Starting colour in the banner editor's",
                    "Outline panel.")
            .defineInRange("defaultTextOutlineColor", 0xFF000000, Integer.MIN_VALUE, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_TEXT_OUTLINE_GRADIENT = BUILDER
            .comment("Optional gradient for the default outline colour, as \"<secondHex>|<DIRECTION>\".")
            .define("defaultTextOutlineGradient", "");

    static {
        BUILDER.pop();
        BUILDER.pop();
        BUILDER.comment("How organized tabs behave inside the creative menu itself.")
                .push("creative");
    }

    public enum IndexPanelStyle { VANILLA, DARK, REFURBISHED, BACKPORT, UNIQUE_DARK, ADAPTIVE }

    public static final ModConfigSpec.EnumValue<IndexPanelStyle> INDEX_PANEL_STYLE = BUILDER
            .comment("Visual style of the section-index panel: VANILLA (light",
                    "raised panel, default), DARK (flat dark panel), REFURBISHED (beveled side tabs),",
                    "BACKPORT (compact textured panel), UNIQUE_DARK (flat near-black panel after the",
                    "Unique Dark resource pack), ADAPTIVE (takes its colours from whatever creative-menu",
                    "texture the loaded resource pack provides).")
            .defineEnum("indexPanelStyle", IndexPanelStyle.VANILLA);

    public static final ModConfigSpec.BooleanValue GRID_BRIDGE = BUILDER
            .comment("Draw the one-pixel lines that bridge the gaps the item grid leaves along the top and",
                    "bottom of a section banner. Their colours are taken from the creative menu's own",
                    "texture, so they follow a resource pack; turn this off to leave the grid untouched.")
            .define("gridBridge", true);

    public static final ModConfigSpec.BooleanValue SHOW_COLLAPSE_TOGGLE = BUILDER
            .comment("Show Fancy Tab Sections' collapse/expand button on each banner. Off by default.")
            .define("showCollapseToggle", false);

    public static final ModConfigSpec.BooleanValue ITEM_GROUP_MARKERS = BUILDER
            .comment("Draw a small +/- marker on the slot of an item group, so a closed group reads as one",
                    "you can open rather than an ordinary item.")
            .define("itemGroupMarkers", true);

    public static final ModConfigSpec.BooleanValue STICKY_SECTION_BANNERS = BUILDER
            .comment("Keep each section's banner pinned to the top while scrolling its items, instead of scrolling away.")
            .define("stickySectionBanners", false);

    static {
        BUILDER.pop();
        BUILDER.comment("This mod's own screens: the section list, tab studio, arranger and editors.")
                .push("menus");
    }

    public static final ModConfigSpec.BooleanValue CLASSIC_ORGANIZER_LAYOUT = BUILDER
            .comment("Use the classic (pre-1.3) organizer menu: centered column, per-row Edit button, no search or sidebar.")
            .define("classicOrganizerLayout", false);

    public enum SidebarSide { LEFT, RIGHT }

    public static final ModConfigSpec.EnumValue<SidebarSide> COLORS_SIDEBAR_SIDE = BUILDER
            .comment("Which edge of the section list screen keeps its tool sidebar: LEFT (default,",
                    "matching the All Settings screen, which also keeps its sidebar on the left) or",
                    "RIGHT. Ignored by the classic organizer layout, which has no sidebar.")
            .defineEnum("colorsSidebarSide", SidebarSide.LEFT);

    public enum MenuStyle { DEFAULT, DEFAULT_MODERN, CREATE, ESSENTIAL }

    public static final ModConfigSpec.EnumValue<MenuStyle> MENU_STYLE = BUILDER
            .comment("Visual style of this mod's own config screens: DEFAULT (stock Minecraft widgets),",
                    "DEFAULT_MODERN (the settings menu's flat cards and gold accent, everywhere),",
                    "CREATE (flat dark panels with light outlines, after Create's config editor) or",
                    "ESSENTIAL (flat grey cards with a blue accent). Layout is identical for all.")
            .defineEnum("menuStyle", MenuStyle.DEFAULT);

    public static final ModConfigSpec.BooleanValue MENU_STYLE_TRANSPARENT = BUILDER
            .comment("Only used by the CREATE menu style. When true the panorama shows through a dark",
                    "tint; when false the background is a flat opaque slate, which reads closer to",
                    "Create's own config editor.")
            .define("menuStyleTransparent", true);

    public static final ModConfigSpec.IntValue MENU_ACCENT_HUE = BUILDER
            .comment("Hue in degrees for every accent -- button",
                    "outlines, text, rules, scrollbars and highlights. 205 is Create's own blue;",
                    "raise it toward 280 for purple, drop it toward 120 for green. Use -1 for a",
                    "neutral greyscale theme with no colour at all.")
            .defineInRange("menuAccentHue", 205, -1, 360);

    public static final ModConfigSpec.IntValue MENU_ACCENT_SATURATION_CREATE = BUILDER
            .comment("Saturation of every accent in the CREATE menu style, as a percentage of the",
                    "palette's own colours. 100 leaves them exactly as authored, 0 is greyscale and",
                    "200 doubles the intensity.")
            .defineInRange("menuAccentSaturationCreate", 100, 0, 200);

    public static final ModConfigSpec.IntValue MENU_ACCENT_SATURATION_ESSENTIAL = BUILDER
            .comment("Saturation of every accent in the ESSENTIAL menu style, as a percentage of the",
                    "palette's own colours. Defaults higher than the Create style because Essential's",
                    "palette is built around a much more vivid blue.")
            .defineInRange("menuAccentSaturationEssential", 145, 0, 200);

    public static final ModConfigSpec.IntValue MENU_ACCENT_SATURATION_DEFAULT = BUILDER
            .comment("Saturation of every accent in the DEFAULT menu style, as a percentage of the",
                    "accent colour. 100 leaves it exactly as authored, 0 is greyscale and 200",
                    "doubles the intensity.")
            .defineInRange("menuAccentSaturationDefault", 100, 0, 200);

    public static final ModConfigSpec.IntValue MENU_ACCENT_SATURATION_MODERN = BUILDER
            .comment("Saturation of every accent in the DEFAULT_MODERN menu style, as a percentage of",
                    "the palette's own colours. 100 leaves the gold exactly as authored.")
            .defineInRange("menuAccentSaturationModern", 100, 0, 200);

    public enum ArrangerStyle { OUTLINE, DARK, VANILLA }

    public static final ModConfigSpec.EnumValue<ArrangerStyle> ARRANGER_STYLE = BUILDER
            .comment("Visual style of the Tab Arranger screen: OUTLINE (black panels with white",
                    "outlines, default), DARK (dark bevelled panels), VANILLA (the stock creative",
                    "menu look).")
            .defineEnum("arrangerStyle", ArrangerStyle.OUTLINE);

    public enum ArrangerLayout { SCREEN, PAGES }

    public static final ModConfigSpec.EnumValue<ArrangerLayout> ARRANGER_LAYOUT = BUILDER
            .comment("Tab Arranger layout: SCREEN (a replica of the creative menu, one page at a",
                    "time, default) or PAGES (a scrolling grid of page cards, more pages at once).")
            .defineEnum("arrangerLayout", ArrangerLayout.SCREEN);

    public static final ModConfigSpec.BooleanValue BANNER_EDITOR_PREVIEW_TOP = BUILDER
            .comment("Where the banner preview sits in the editor: true = under the title, false = above",
                    "the OK/Cancel buttons.")
            .define("bannerEditorPreviewTop", false);

    public static final ModConfigSpec.IntValue GRADIENT_CELL_SIZE = BUILDER
            .comment("Pixel chunkiness of the hue/saturation/value gradients in the banner editor. 1 =",
                    "smooth, higher = blockier.")
            .defineInRange("gradientCellSize", 5, 1, 20);

    static {
        BUILDER.pop();
        BUILDER.comment("Rendering cost of this mod's screens. Lower these first if a menu feels slow.")
                .push("performance");
    }

    public static final ModConfigSpec.BooleanValue CACHE_ITEM_ICONS = BUILDER
            .comment("Render each item icon once into a cached texture instead of rebuilding its model every",
                    "frame. Large item grids get much faster. Turn this off if icons look wrong.")
            .define("cacheItemIcons", true);

    public static final ModConfigSpec.IntValue MENU_FRAMERATE = BUILDER
            .comment("Frame rate cap while a menu is open outside a world. Vanilla hard-codes this to 60,",
                    "which makes menu scrolling and animations feel choppy. Your video settings' Max",
                    "Framerate still applies as the upper bound. Set to 60 for vanilla behaviour.")
            .defineInRange("menuFramerate", 120, 60, 1000);

    static {
        BUILDER.pop();
        BUILDER.comment("Community banners and text banners fetched from the internet. Turn the two fetch",
                        "switches off to keep the mod fully offline.")
                .push("online");
    }

    public static final ModConfigSpec.BooleanValue FETCH_ONLINE_BANNERS = BUILDER
            .comment("Check GitHub once per launch for new/updated community banners and credits. Disable",
                    "for offline use.")
            .define("fetchOnlineBanners", true);

    public static final ModConfigSpec.ConfigValue<String> BANNER_MANIFEST_URL = BUILDER
            .comment("URL of the remote banner manifest (JSON). Used only when fetchOnlineBanners is true.")
            .define("bannerManifestUrl",
                    "https://cdn.jsdelivr.net/gh/SockyWocky7/createaddonorganizer@master/banners/index.json",
                    Config::isValidUrl);

    public static final ModConfigSpec.ConfigValue<String> BANNER_POOLS_MANIFEST_URL = BUILDER
            .comment("URL of the remote banner-pool manifest (JSON) -- curated banners per tab. Used only",
                    "when fetchOnlineBanners is true; edit the file at this URL to update pools without a",
                    "mod update.")
            .define("bannerPoolsManifestUrl",
                    "https://cdn.jsdelivr.net/gh/SockyWocky7/createaddonorganizer@master/banners/pools.json",
                    Config::isValidUrl);

    public static final ModConfigSpec.BooleanValue CHECK_FOR_UPDATES = BUILDER
            .comment("Ask Modrinth once per launch whether a newer build of this mod exists for your",
                    "Minecraft version, and show a notice in the settings menu if so. Nothing is",
                    "downloaded or installed -- the notice just opens the mod page.")
            .define("checkForUpdates", true);

    public static final ModConfigSpec.BooleanValue FETCH_ONLINE_BOX_TEXTURES = BUILDER
            .comment("Check GitHub once per launch for new/updated community text-banner textures. Disable",
                    "for offline use.")
            .define("fetchOnlineBoxTextures", true);

    public static final ModConfigSpec.ConfigValue<String> BOX_MANIFEST_URL = BUILDER
            .comment("URL of the remote text-banner manifest (JSON). Used only when fetchOnlineBoxTextures",
                    "is true.")
            .define("boxManifestUrl",
                    "https://cdn.jsdelivr.net/gh/SockyWocky7/createaddonorganizer@master/text_banners/index.json",
                    Config::isValidUrl);

    static {
        BUILDER.pop();
        BUILDER.comment("Sound effects played by this mod's own screens. Nothing here affects the game.")
                .push("sounds");
    }

    public enum SfxStyle { CLICKY, THOCKY }

    public static final ModConfigSpec.BooleanValue SFX_ENABLED = BUILDER
            .comment("Master switch for every sound below -- turn off for silent menus.")
            .define("allSounds", true);

    public static final ModConfigSpec.EnumValue<SfxStyle> SFX_STYLE = BUILDER
            .comment("Voicing for all of the sounds below: CLICKY (sharp, high-pitched clicks) or",
                    "THOCKY (deeper, muted wooden knocks).")
            .defineEnum("sfxStyle", SfxStyle.CLICKY);

    public static final ModConfigSpec.BooleanValue SFX_GRAB = BUILDER
            .comment("Click when picking up an item, section or tab to drag it.")
            .define("grab", true);

    public static final ModConfigSpec.BooleanValue SFX_RELEASE = BUILDER
            .comment("Click when dropping whatever was being dragged.")
            .define("release", true);

    public static final ModConfigSpec.BooleanValue SFX_PICKUP = BUILDER
            .comment("Rising pops as a shift-drag sweeps extra items into the carried stack.")
            .define("pickup", true);

    public static final ModConfigSpec.BooleanValue SFX_GRID_STEP = BUILDER
            .comment("Tick as a dragged item moves from one grid slot to the next.")
            .define("gridStep", true);

    public static final ModConfigSpec.BooleanValue SFX_SNAP = BUILDER
            .comment("Click when the tab editor's split bar snaps to a new column count.")
            .define("snap", true);

    public static final ModConfigSpec.BooleanValue SFX_SCROLL = BUILDER
            .comment("Faint tick per notch while scrolling a list or grid, or dragging a scrollbar.")
            .define("scroll", true);

    public static final ModConfigSpec.BooleanValue SFX_BLOCKED = BUILDER
            .comment("Refusal sounds: a two-note \"no\" for undo/redo with an empty history, and a soft low",
                    "thud for scrolling past the end of a list. The thud also needs scroll to be on.")
            .define("blocked", true);

    public static final ModConfigSpec.BooleanValue SFX_BIN_HOVER = BUILDER
            .comment("Lid creak as a dragged item enters the tab editor's bin.")
            .define("binHover", true);

    public static final ModConfigSpec.BooleanValue SFX_BIN_ITEM = BUILDER
            .comment("One clunk per item as it lands in the bin.")
            .define("binItem", true);

    public static final ModConfigSpec.BooleanValue SFX_BIN_CLOSE = BUILDER
            .comment("Lid slam once everything has landed in the bin.")
            .define("binClose", true);

    static {
        BUILDER.pop();
        BUILDER.comment("Motion in this mod's own screens. Turning one off makes that effect snap straight",
                        "to its finished state -- nothing is hidden, it just stops moving.")
                .push("animations");
    }

    public static final ModConfigSpec.BooleanValue ANIM_ENABLED = BUILDER
            .comment("Master switch for every animation below.")
            .define("allAnimations", true);

    public static final ModConfigSpec.BooleanValue ANIM_MENU_ENTRANCE = BUILDER
            .comment("The section list's opening sequence: the title sliding down, then buttons, rules and",
                    "rows fading in.")
            .define("menuEntrance", true);

    public static final ModConfigSpec.BooleanValue ANIM_TITLE_GLINT = BUILDER
            .comment("The shine that sweeps across the section list's title every few seconds.")
            .define("titleGlint", true);

    public static final ModConfigSpec.BooleanValue ANIM_SMOOTH_SCROLL = BUILDER
            .comment("Eased scrolling in lists and item grids. Off means each notch jumps straight to its",
                    "new position.")
            .define("smoothScroll", true);

    public static final ModConfigSpec.BooleanValue ANIM_ITEM_SLIDE = BUILDER
            .comment("Items and section rows gliding to their new places in the tab editor as a drag",
                    "reorders them.")
            .define("itemSlide", true);

    public static final ModConfigSpec.BooleanValue ANIM_ITEM_POP = BUILDER
            .comment("The brief pop an item makes when it lands in the tab editor's contents.")
            .define("itemPop", true);

    public static final ModConfigSpec.BooleanValue ANIM_BIN = BUILDER
            .comment("The tab editor's bin: the lid, items falling in, and the landing squash.")
            .define("bin", true);

    public static final ModConfigSpec.BooleanValue ANIM_PANE_RESIZE = BUILDER
            .comment("The tab editor's split bar easing into place after a resize instead of snapping.")
            .define("paneResize", true);

    public static final ModConfigSpec.BooleanValue ANIM_CONTROL_TIPS = BUILDER
            .comment("The tab editor's control hints fading between each other. Off leaves one hint on",
                    "screen; clicking it still moves to the next.")
            .define("controlTips", true);

    public static final ModConfigSpec.BooleanValue ANIM_STYLE_BLEND = BUILDER
            .comment("The crossfade when switching between menu styles.")
            .define("styleBlend", true);

    public static final ModConfigSpec.BooleanValue ANIM_BUTTON_HOVER = BUILDER
            .comment("Skinned buttons easing their outline colour on hover and click.")
            .define("buttonHover", true);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("The slide between menus: one leaves the way you are heading and the next arrives",
                        "from behind it. Timings are in milliseconds, travel is in GUI pixels.")
                .push("slide");
    }

    public enum SwooshExit { ACCELERATE, SMOOTH, LINEAR, WINDUP }

    public enum SwooshEnter { DECELERATE, OVERSHOOT, SPRING, LINEAR }

    public enum SwooshDirection { SIDEWAYS, UP, DOWN }

    public static final ModConfigSpec.BooleanValue SWOOSH_ENABLED = BUILDER
            .comment("Menus sliding out the way you are heading and the next one arriving from behind it.")
            .define("screenSwoosh", true);

    public static final ModConfigSpec.IntValue SWOOSH_OUT_MS = BUILDER
            .comment("How long the outgoing menu takes to leave, in milliseconds.")
            .defineInRange("swooshExitMillis", 160, 40, 600);

    public static final ModConfigSpec.IntValue SWOOSH_HOLD_MS = BUILDER
            .comment("The pause on an empty background between the two menus. Zero makes the swap land on",
                    "the same frame as the fade, which reads as a flicker.")
            .defineInRange("swooshHoldMillis", 40, 0, 300);

    public static final ModConfigSpec.IntValue SWOOSH_IN_MS = BUILDER
            .comment("How long the incoming menu takes to settle, in milliseconds.")
            .defineInRange("swooshEnterMillis", 220, 40, 800);

    public static final ModConfigSpec.IntValue SWOOSH_TRAVEL = BUILDER
            .comment("How far a menu travels, in GUI pixels, so it scales with the GUI scale.")
            .defineInRange("swooshTravel", 52, 4, 320);

    public static final ModConfigSpec.BooleanValue SWOOSH_FADE = BUILDER
            .comment("Fade the menu out as it leaves. Off slides it without fading.")
            .define("swooshFade", true);

    public static final ModConfigSpec.EnumValue<SwooshExit> SWOOSH_EXIT_CURVE = BUILDER
            .comment("How the outgoing menu picks up speed.")
            .defineEnum("swooshExitCurve", SwooshExit.ACCELERATE);

    public static final ModConfigSpec.EnumValue<SwooshEnter> SWOOSH_ENTER_CURVE = BUILDER
            .comment("How the incoming menu settles. The overshoot options carry it a few pixels past rest",
                    "and back.")
            .defineEnum("swooshEnterCurve", SwooshEnter.DECELERATE);

    public static final ModConfigSpec.EnumValue<SwooshDirection> SWOOSH_DEPTH_DIRECTION = BUILDER
            .comment("Which way screens reached without the arrows travel -- a section's editor, add section,",
                    "presets. UP sends the old screen upward and brings the new one from below, DOWN is the",
                    "reverse, SIDEWAYS makes them page like the arrows do.")
            .defineEnum("swooshDepthDirection", SwooshDirection.UP);

    public static final ModConfigSpec.BooleanValue SWOOSH_CONFIG_MENU = BUILDER
            .comment("The organizer menu sliding in when you open it. Leaving it never slides -- the menu",
                    "closes on the frame you press Done.")
            .define("swooshConfigMenu", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_ARROW_LEFT = BUILDER
            .comment("The left arrow, between the section list and the settings menu.")
            .define("swooshArrowLeft", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_ARROW_RIGHT = BUILDER
            .comment("The right arrow, to the Creative Tab Studio.")
            .define("swooshArrowRight", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_BANNER_EDITOR = BUILDER
            .comment("Opening a section's banner and colour editor.")
            .define("swooshBannerEditor", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_PRESETS = BUILDER
            .comment("The presets browser and its edit screens.")
            .define("swooshPresets", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_MENU_STYLE = BUILDER
            .comment("The menu style screen.")
            .define("swooshMenuStyle", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_CREDITS = BUILDER
            .comment("The banner credits and bug report screens.")
            .define("swooshCredits", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_ADD_SECTION = BUILDER
            .comment("The add-section picker.")
            .define("swooshAddSection", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_TAB_STUDIO = BUILDER
            .comment("Tab Studio's own screens: the arranger, the tab editor and the tab picker.")
            .define("swooshTabStudio", true);

    public static final ModConfigSpec.BooleanValue SWOOSH_BACK = BUILDER
            .comment("Going back out of a screen, which plays the same motion in reverse.")
            .define("swooshBack", true);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Written by the in-game editors -- banner colours, images, names, orders and the",
                        "like, keyed by tab ID. Editing these by hand works, but the editors are easier.")
                .push("saved");
    }

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SECTION_COLORS = BUILDER
            .comment("Per-section banner colours, keyed by tab ID. Format: \"<tabId> = <hex>\", optionally a",
                    "gradient: \"<tabId> = <hex1>|<hex2>|<DIRECTION>|<STYLE>\". Accepts #RRGGBB, #AARRGGBB, or",
                    "0x-prefixed hex.")
            .defineListAllowEmpty("sectionColors", List.of(), () -> "somemod:main = #4A4A4A", Config::isValidBannerColorSpecEntry);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BANNERS = BUILDER
            .comment("Per-section banner IMAGES, keyed by tab ID; overrides the colour. Format:",
                    "\"<tabId> = <ref>\" (\"res:ns:path\", \"file:name.png\", or \"remote:name.png\").",
                    "Managed by the in-game banner editor; banners are 160x17.")
            .defineListAllowEmpty("banners", List.of(),
                    () -> "somemod:main = res:createaddonorganizer:textures/banner/create1.png", Config::isValidBanner);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ANIMATED_BANNERS = BUILDER
            .comment("Marks a banner texture as animated (vertical strip of 17px frames), keyed by texture id.",
                    "Format: \"<textureId> = <frametime>\" (ticks). Bundled textures auto-detect via .mcmeta.",
                    "Managed by the in-game banner editor.")
            .defineListAllowEmpty("animatedBanners", List.of(),
                    () -> "createaddonorganizer:custom_banner/example = 2", Config::isValidAnimatedBanner);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALWAYS_ANIMATED_BANNERS = BUILDER
            .comment("Banner textures that keep animating while not hovered, keyed by texture id.",
                    "Format: \"<textureId> = true\". Banners absent from this list only animate on hover.")
            .defineListAllowEmpty("alwaysAnimatedBanners", List.of(),
                    () -> "createaddonorganizer:native_banner/example = true", Config::isValidSectionBoolean);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_BANNER_POOL = BUILDER
            .comment("Your own uploads added to a curated tab's banner pool, keyed by tab ID. Format:",
                    "\"<tabId> = <ref>\". Managed by the in-game banner editor.")
            .defineListAllowEmpty("extraBannerPool", List.of(),
                    () -> "somemod:main = file:example.png", Config::isValidBanner);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOX_COLORS = BUILDER
            .comment("Per-section tinted-box colours, keyed by tab ID. Same format as sectionColors; alpha",
                    "controls opacity.")
            .defineListAllowEmpty("boxColors", List.of(), () -> "somemod:main = #64000000", Config::isValidSectionColor);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOX_TEXTURES = BUILDER
            .comment("Per-section contrast-box IMAGES, keyed by tab ID (3-sliced horizontally so any width",
                    "fits). Format: \"<tabId> = <ref>\" (\"res:ns:path\" or \"file:name.png\"). Fixed 14px height.",
                    "Managed by the in-game box editor.")
            .defineListAllowEmpty("boxTextures", List.of(),
                    () -> "somemod:main = res:createaddonorganizer:textures/box/example.png", Config::isValidBanner);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOX_DARKENS = BUILDER
            .comment("Per-section contrast-box image darken overrides, keyed by tab ID.")
            .defineListAllowEmpty("boxDarkens", List.of(), () -> "somemod:main = 0.0", Config::isValidSectionFraction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BOX_OPACITIES = BUILDER
            .comment("Per-section contrast-box image opacity overrides, keyed by tab ID.")
            .defineListAllowEmpty("boxOpacities", List.of(), () -> "somemod:main = 1.0", Config::isValidSectionFraction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEXT_COLORS = BUILDER
            .comment("Per-section title text colours, keyed by tab ID. Same format as sectionColors, minus",
                    "the STYLE token.")
            .defineListAllowEmpty("textColors", List.of(), () -> "somemod:main = #FFFFFFFF", Config::isValidTextColorSpecEntry);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEXT_SECONDARY_COLORS = BUILDER
            .comment("Per-section secondary text colour overrides, keyed by tab ID. Only used while",
                    "twoToneText is on.")
            .defineListAllowEmpty("textSecondaryColors", List.of(), () -> "somemod:main = #FFCEA05A", Config::isValidTextColorSpecEntry);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEXT_SPLITS = BUILDER
            .comment("Per-section two-tone split overrides, keyed by tab ID. Format: \"<tabId> = <fraction>\".",
                    "Only used while twoToneText is on.")
            .defineListAllowEmpty("textSplits", List.of(), () -> "somemod:main = 0.56", Config::isValidSectionFraction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SCROLL_CUTOFFS = BUILDER
            .comment("Per-section scroll cutoff overrides, keyed by tab ID. Managed from the banner editor's",
                    "Primary text panel.")
            .defineListAllowEmpty("scrollCutoffs", List.of(), () -> "somemod:main = 1.0", Config::isValidSectionFraction);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TITLE_TEXT_SHADOW_SECTIONS = BUILDER
            .comment("Per-section drop-shadow on/off overrides, keyed by tab ID. Format: \"<tabId> = true\"",
                    "or \"false\". Managed from the banner editor's Shadow panel.")
            .defineListAllowEmpty("titleTextShadowSections", List.of(), () -> "somemod:main = true", Config::isValidSectionBoolean);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEXT_BOX_SECTIONS = BUILDER
            .comment("Per-section tinted title box on/off overrides, keyed by tab ID. Format: \"<tabId> = true\"",
                    "or \"false\". A section without an entry follows the global tintedTextBox toggle.")
            .defineListAllowEmpty("textBoxSections", List.of(), () -> "somemod:main = true", Config::isValidSectionBoolean);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEXT_OUTLINE_COLORS = BUILDER
            .comment("Per-section text outline colours, keyed by tab ID. An entry's presence enables the",
                    "outline for that section. Managed from the banner editor's Outline panel.")
            .defineListAllowEmpty("textOutlineColors", List.of(), () -> "somemod:main = #FF000000", Config::isValidTextColorSpecEntry);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEXT_SHADOW_COLORS = BUILDER
            .comment("Per-section custom drop-shadow colours, keyed by tab ID. An entry unlinks that shadow",
                    "from the primary text colour. Only drawn while titleTextShadow is on.")
            .defineListAllowEmpty("textShadowColors", List.of(), () -> "somemod:main = #FF000000", Config::isValidSectionColor);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIGHLIGHT_COLORS = BUILDER
            .comment("Accent colour for MAIN tabs only, keyed by tab ID. Config-screen only -- tints that",
                    "tab's row in the section list, no effect in-game.")
            .defineListAllowEmpty("highlightColors", List.of(), () -> "create:base = #4A90D9", Config::isValidSectionColor);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SECTION_ORDER = BUILDER
            .comment("Manual drag order of sections within each parent tab. Unlisted sections are appended",
                    "alphabetically.")
            .defineListAllowEmpty("sectionOrder", List.of(), () -> "somemod:main", Config::isValidTabId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SECTION_NAMES = BUILDER
            .comment("Custom display names, keyed by tab ID. Format: \"<tabId> = <name>\". Managed by",
                    "ctrl+click-to-rename in the section list. An entry with a blank name draws no title at",
                    "all; remove the entry to get the tab's own name back.")
            .defineListAllowEmpty("sectionNames", List.of(), () -> "somemod:main = My Custom Name", Config::isValidSectionName);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> COLLAPSED_SECTIONS = BUILDER
            .comment("Sections currently collapsed via the collapse toggle. Only relevant while",
                    "showCollapseToggle is on.")
            .defineListAllowEmpty("collapsedSections", List.of(), () -> "somemod:main", Config::isValidTabId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> OPEN_ITEM_GROUPS = BUILDER
            .comment("Item groups currently opened out in the creative menu, as \"<sectionId>#<groupId>\".",
                    "Groups start closed, so only the ones you have opened are listed. Managed by clicking",
                    "a group's slot in the creative menu.")
            .defineListAllowEmpty("openItemGroups", List.of(), () -> "somemod:main#g0", Config::isValidGroupKey);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> TAB_ORDER = BUILDER
            .comment("Manual order of creative tabs, as tab IDs. Unlisted tabs keep their registry",
                    "order and are appended after. Managed by the Tab Arranger.")
            .defineListAllowEmpty("tabOrder", List.of(), () -> "somemod:main", Config::isValidTabId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> NATIVE_SEEDED = BUILDER
            .comment("Sections whose look was copied in from the mod that drew them itself (Simulated-style",
                    "banners). Listed once the copy is made so a later launch never overwrites your edits.",
                    "Remove an ID to have its original banner, colours and title copied in again.")
            .defineListAllowEmpty("nativeSeededSections", List.of(), () -> "somemod:main", Config::isValidTabId);

    public static final ModConfigSpec.IntValue NATIVE_SEED_VERSION = BUILDER
            .comment("How the copy above was made. Raised when a fix changes what a faithful copy looks like,",
                    "which re-copies every section listed above once. Lower it by hand to force that re-copy.")
            .defineInRange("nativeSeedVersion", 0, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue BANNER_DRAW_DIAGNOSTICS = BUILDER
            .comment("Logs one line per section the first time its banner is drawn: which texture was bound,",
                    "how many frames it has, and the colours in play. For chasing a banner that will not show.")
            .define("bannerDrawDiagnostics", false);

    public static final ModConfigSpec.BooleanValue EDITOR_HINT_SEEN = BUILDER
            .comment("Set once the banner editor's preview hint is dismissed. Turn off to show it again.")
            .define("editorHintSeen", false);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Switches only the dev-mode overlay exposes. They stay in the file when dev mode is",
                        "off, but nothing reads them until it is unlocked.")
                .push("devmode");
    }

    public static final ModConfigSpec.BooleanValue DEV_FPS_DISPLAY = BUILDER
            .comment("The frame-time panel in the corner: current and worst frame, and where the time went.")
            .define("fpsDisplay", true);

    public static final ModConfigSpec.BooleanValue DEV_FAKE_UPDATE = BUILDER
            .comment("Pretend a newer version is on Modrinth, so the update prompt can be checked without",
                    "waiting for a real release.")
            .define("fakeUpdateAvailable", false);

    public static final ModConfigSpec.BooleanValue DEV_ALL_BANNER_POOLS = BUILDER
            .comment("Show every shipped banner in the picker instead of only the ones a section is allowed.")
            .define("unrestrictedBannerPools", false);

    public static final ModConfigSpec.BooleanValue DEV_TEXT_EDITOR = BUILDER
            .comment("Edit any on-screen wording in place. Hover a line and press the Edit Hovered Text key to",
                    "rewrite it; the overlay key outlines everything the editor can reach. Edits land in the",
                    "mod's own lang file when the game is running from source, otherwise in a config override.")
            .define("textEditor", true);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    public static ModConfigSpec spec() {
        return SPEC;
    }

    public static void save() {
        SPEC.save();
    }

    public static boolean checkForUpdates() {
        return CHECK_FOR_UPDATES.get();
    }

    public static boolean devFpsDisplay() {
        return DEV_FPS_DISPLAY.get();
    }

    public static boolean devFakeUpdate() {
        return DEV_FAKE_UPDATE.get();
    }

    public static boolean devAllBannerPools() {
        return DEV_ALL_BANNER_POOLS.get();
    }

    public static boolean devTextEditor() {
        return DEV_TEXT_EDITOR.get();
    }

    public static void resetAllToDefault() {
        Map<ModConfigSpec.ConfigValue<List<? extends String>>, List<String>> modDrawn = modDrawnEntries();
        applyAppearance(DEFAULT_BANNER_COLOR.getDefault(), DEFAULT_BANNER_GRADIENT.getDefault(), SECTION_COLORS.getDefault(),
                BANNERS.getDefault(), ANIMATED_BANNERS.getDefault(), TINTED_TEXT_BOX.getDefault(), DEFAULT_BOX_COLOR.getDefault(),
                BOX_COLORS.getDefault(), BOX_TEXTURES.getDefault(), DEFAULT_TEXT_COLOR.getDefault(), DEFAULT_TEXT_GRADIENT.getDefault(),
                TEXT_COLORS.getDefault(), TWO_TONE_TEXT.getDefault(), DEFAULT_TEXT_SECONDARY_COLOR.getDefault(),
                DEFAULT_TEXT_SECONDARY_GRADIENT.getDefault(), TEXT_SECONDARY_COLORS.getDefault(),
                HIGHLIGHT_COLORS.getDefault(), SHOW_ALL_BANNERS.getDefault(), EXTRA_BANNER_POOL.getDefault());
        applyOrganization(SECTION_ORDER.getDefault(), SECTION_NAMES.getDefault());
        applyAbsorption(FORCE_INCLUDE.getDefault(), FORCE_EXCLUDE.getDefault(), ROUTES.getDefault(),
                EXTRA_MAIN_SECTIONS.getDefault());
        setRainbowMode(RAINBOW_MODE.getDefault());
        COLLAPSED_SECTIONS.set(COLLAPSED_SECTIONS.getDefault());
        DEFAULT_TEXT_OUTLINE_COLOR.set(DEFAULT_TEXT_OUTLINE_COLOR.getDefault());
        DEFAULT_TEXT_OUTLINE_GRADIENT.set(DEFAULT_TEXT_OUTLINE_GRADIENT.getDefault());
        TEXT_OUTLINE_COLORS.set(TEXT_OUTLINE_COLORS.getDefault());
        applyAppearanceExtras(TITLE_TEXT_SHADOW.getDefault(), TITLE_TEXT_SHADOW_SECTIONS.getDefault(),
                DEFAULT_TEXT_SHADOW_COLOR.getDefault(), TEXT_SHADOW_COLORS.getDefault(),
                DEFAULT_SCROLL_CUTOFF.getDefault(), SCROLL_CUTOFFS.getDefault(),
                DEFAULT_TWO_TONE_SPLIT.getDefault(), TEXT_SPLITS.getDefault());
        DEFAULT_BOX_DARKEN.set(DEFAULT_BOX_DARKEN.getDefault());
        BOX_DARKENS.set(BOX_DARKENS.getDefault());
        DEFAULT_BOX_OPACITY.set(DEFAULT_BOX_OPACITY.getDefault());
        BOX_OPACITIES.set(BOX_OPACITIES.getDefault());
        TEXT_BOX_SECTIONS.set(TEXT_BOX_SECTIONS.getDefault());
        restoreEntries(modDrawn);
        SPEC.save();
    }

    public static boolean resetToPackDefaults() {
        Set<String> keys = new HashSet<>();
        for (ResourceLocation id : PackDefaults.touchedSections()) {
            if (!isNativeSeeded(id)) {
                keys.add(id.toString());
            }
        }
        boolean changed = false;
        if (!keys.isEmpty()) {
            for (ModConfigSpec.ConfigValue<List<? extends String>> value : keyedSectionValues()) {
                List<String> kept = new ArrayList<>();
                for (String entry : value.get()) {
                    String[] parts = entry.split("=", 2);
                    if (parts.length == 2 && keys.contains(parts[0].trim())) {
                        continue;
                    }
                    kept.add(entry);
                }
                if (kept.size() != value.get().size()) {
                    value.set(kept);
                    changed = true;
                }
            }
            List<String> routes = new ArrayList<>();
            for (String entry : ROUTES.get()) {
                String[] parts = entry.split(">", 2);
                if (parts.length == 2 && keys.contains(parts[0].trim())) {
                    continue;
                }
                routes.add(entry);
            }
            if (routes.size() != ROUTES.get().size()) {
                ROUTES.set(routes);
                changed = true;
            }
            for (ModConfigSpec.ConfigValue<List<? extends String>> value : List.of(FORCE_INCLUDE, FORCE_EXCLUDE)) {
                List<String> kept = new ArrayList<>();
                for (String entry : value.get()) {
                    if (!keys.contains(entry.trim())) {
                        kept.add(entry);
                    }
                }
                if (kept.size() != value.get().size()) {
                    value.set(kept);
                    changed = true;
                }
            }
        }
        if (PackDefaults.hasOrder() && !SECTION_ORDER.get().isEmpty()) {
            SECTION_ORDER.set(List.of());
            changed = true;
        }
        if (changed) {
            SPEC.save();
        }
        return changed;
    }

    private static Map<ModConfigSpec.ConfigValue<List<? extends String>>, List<String>> modDrawnEntries() {
        Set<String> keys = new HashSet<>();
        for (String id : NATIVE_SEEDED.get()) {
            keys.add(id);
        }
        Map<ModConfigSpec.ConfigValue<List<? extends String>>, List<String>> out = new IdentityHashMap<>();
        List<ModConfigSpec.ConfigValue<List<? extends String>>> keyed = new ArrayList<>(keyedSectionValues());
        keyed.add(EXTRA_BANNER_POOL);
        for (ModConfigSpec.ConfigValue<List<? extends String>> value : keyed) {
            List<String> kept = new ArrayList<>();
            for (String entry : value.get()) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2 && keys.contains(parts[0].trim())) {
                    kept.add(entry);
                }
            }
            if (!kept.isEmpty()) {
                out.put(value, kept);
            }
        }
        keepAnimationOf(keys, out);
        return out;
    }

    private static void keepAnimationOf(Set<String> keys,
            Map<ModConfigSpec.ConfigValue<List<? extends String>>, List<String>> out) {
        Set<String> textures = new HashSet<>();
        for (String entry : BANNERS.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length != 2 || !keys.contains(parts[0].trim())) {
                continue;
            }
            ResourceLocation texture = BannerTextures.resolve(parts[1].trim());
            if (texture != null) {
                textures.add(texture.toString());
            }
        }
        if (textures.isEmpty()) {
            return;
        }
        for (ModConfigSpec.ConfigValue<List<? extends String>> value : List.of(ANIMATED_BANNERS, ALWAYS_ANIMATED_BANNERS)) {
            List<String> kept = new ArrayList<>();
            for (String entry : value.get()) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2 && textures.contains(parts[0].trim())) {
                    kept.add(entry);
                }
            }
            if (!kept.isEmpty()) {
                out.put(value, kept);
            }
        }
    }

    private static void restoreEntries(Map<ModConfigSpec.ConfigValue<List<? extends String>>, List<String>> saved) {
        for (Map.Entry<ModConfigSpec.ConfigValue<List<? extends String>>, List<String>> e : saved.entrySet()) {
            Set<String> restored = new HashSet<>();
            for (String entry : e.getValue()) {
                restored.add(entry.split("=", 2)[0].trim());
            }
            List<String> merged = new ArrayList<>();
            for (String entry : e.getKey().get()) {
                if (!restored.contains(entry.split("=", 2)[0].trim())) {
                    merged.add(entry);
                }
            }
            merged.addAll(e.getValue());
            e.getKey().set(merged);
        }
    }

    public static void applyOrganization(List<? extends String> sectionOrder, List<? extends String> sectionNames) {
        SECTION_ORDER.set(sectionOrder);
        SECTION_NAMES.set(sectionNames);
        SPEC.save();
    }

    public static void applyAbsorption(List<? extends String> forceInclude, List<? extends String> forceExclude,
            List<? extends String> routes, List<? extends String> extraMainSections) {
        FORCE_INCLUDE.set(forceInclude);
        FORCE_EXCLUDE.set(forceExclude);
        ROUTES.set(routes);
        EXTRA_MAIN_SECTIONS.set(extraMainSections);
        SPEC.save();
    }

    public static void applyAppearance(int bannerColor, String bannerGradient, List<? extends String> sectionColors,
            List<? extends String> banners, List<? extends String> animatedBanners, boolean tintedBox, int boxColor,
            List<? extends String> boxColors, List<? extends String> boxTextures, int textColor, String textGradient,
            List<? extends String> textColors, boolean twoTone, int textSecondaryColor, String textSecondaryGradient,
            List<? extends String> textSecondaryColors, List<? extends String> highlightColors,
            boolean showAllBanners, List<? extends String> extraBannerPool) {
        DEFAULT_BANNER_COLOR.set(bannerColor);
        DEFAULT_BANNER_GRADIENT.set(bannerGradient);
        SECTION_COLORS.set(sectionColors);
        BANNERS.set(banners);
        ANIMATED_BANNERS.set(animatedBanners);
        TINTED_TEXT_BOX.set(tintedBox);
        DEFAULT_BOX_COLOR.set(boxColor);
        BOX_COLORS.set(boxColors);
        BOX_TEXTURES.set(boxTextures);
        DEFAULT_TEXT_COLOR.set(textColor);
        DEFAULT_TEXT_GRADIENT.set(textGradient);
        TEXT_COLORS.set(textColors);
        TWO_TONE_TEXT.set(twoTone);
        DEFAULT_TEXT_SECONDARY_COLOR.set(textSecondaryColor);
        DEFAULT_TEXT_SECONDARY_GRADIENT.set(textSecondaryGradient);
        TEXT_SECONDARY_COLORS.set(textSecondaryColors);
        HIGHLIGHT_COLORS.set(highlightColors);
        SHOW_ALL_BANNERS.set(showAllBanners);
        EXTRA_BANNER_POOL.set(extraBannerPool);
        SPEC.save();
    }

    public static void applyTextOutlineDefaults(int textOutlineColor, String textOutlineGradient,
            List<? extends String> textOutlineColors) {
        DEFAULT_TEXT_OUTLINE_COLOR.set(textOutlineColor);
        DEFAULT_TEXT_OUTLINE_GRADIENT.set(textOutlineGradient);
        TEXT_OUTLINE_COLORS.set(textOutlineColors);
        SPEC.save();
    }

    public static void applyAppearanceExtras(boolean titleTextShadow, List<? extends String> titleTextShadowSections,
            int textShadowColor, List<? extends String> textShadowColors,
            double scrollCutoff, List<? extends String> scrollCutoffs,
            double twoToneSplit, List<? extends String> textSplits) {
        TITLE_TEXT_SHADOW.set(titleTextShadow);
        TITLE_TEXT_SHADOW_SECTIONS.set(titleTextShadowSections);
        DEFAULT_TEXT_SHADOW_COLOR.set(textShadowColor);
        TEXT_SHADOW_COLORS.set(textShadowColors);
        DEFAULT_SCROLL_CUTOFF.set(scrollCutoff);
        SCROLL_CUTOFFS.set(scrollCutoffs);
        DEFAULT_TWO_TONE_SPLIT.set(twoToneSplit);
        TEXT_SPLITS.set(textSplits);
        SPEC.save();
    }

    public static String sectionNameOverride(ResourceLocation id) {
        String player = lookupValue(SECTION_NAMES.get(), id);
        return player != null ? player : PackDefaults.nameFor(id);
    }

    public static boolean hasOwnSectionName(ResourceLocation id) {
        return lookupValue(SECTION_NAMES.get(), id) != null;
    }

    public static void setSectionName(ResourceLocation id, String name) {
        List<String> updated = withoutEntry(SECTION_NAMES.get(), id);
        updated.add(id + " = " + name);
        SECTION_NAMES.set(updated);
        SPEC.save();
    }

    public static void clearSectionName(ResourceLocation id) {
        if (!hasOwnSectionName(id)) {
            return;
        }
        SECTION_NAMES.set(withoutEntry(SECTION_NAMES.get(), id));
        SPEC.save();
    }

    private static boolean isValidSectionName(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        return parts.length == 2 && ResourceLocation.tryParse(parts[0].trim()) != null;
    }

    private static boolean isValidTabId(final Object obj) {
        return obj instanceof String s && ResourceLocation.tryParse(s) != null;
    }

    private static boolean isValidGroupKey(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        int split = s.lastIndexOf('#');
        return split > 0 && split < s.length() - 1
                && ResourceLocation.tryParse(s.substring(0, split)) != null;
    }

    private static boolean isValidRoute(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split(">", 2);
        return parts.length == 2
                && ResourceLocation.tryParse(parts[0].trim()) != null
                && ResourceLocation.tryParse(parts[1].trim()) != null;
    }

    public static boolean isForceIncluded(ResourceLocation id) {
        if (BUILTIN_INCLUDE.contains(id.toString()) || contains(FORCE_INCLUDE.get(), id)) {
            return true;
        }
        return PackDefaults.includes(id) && !contains(FORCE_EXCLUDE.get(), id);
    }

    public static boolean isAskedForByName(ResourceLocation id) {
        return isForceIncluded(id) || lookupRoute(ROUTES.get(), id) != null
                || PackDefaults.routeFor(id) != null || AddonGroups.isMember(id);
    }

    public static boolean isForceExcluded(ResourceLocation id) {
        if (isBuiltinExcluded(id) || contains(FORCE_EXCLUDE.get(), id)) {
            return true;
        }
        return PackDefaults.excludes(id) && !contains(FORCE_INCLUDE.get(), id);
    }

    public static boolean isBuiltinExcluded(ResourceLocation id) {
        return BUILTIN_EXCLUDE.contains(id.toString());
    }

    public static boolean isBuiltinHub(ResourceLocation id) {
        return createaddonorganizer.CREATE_BASE.equals(id) || BUILTIN_ROUTES.containsValue(id.toString())
                || SimulatedSupport.isMainTab(id);
    }

    public static ResourceLocation parentFor(ResourceLocation id) {
        ResourceLocation userRoute = lookupRoute(ROUTES.get(), id);
        if (userRoute != null && !isForceExcluded(userRoute)) {
            return userRoute;
        }
        ResourceLocation packRoute = PackDefaults.routeFor(id);
        if (packRoute != null && !isForceExcluded(packRoute)) {
            return packRoute;
        }
        ResourceLocation groupHub = AddonGroups.hubFor(id);
        if (groupHub != null && !isForceExcluded(groupHub)) {
            return groupHub;
        }
        String builtin = BUILTIN_ROUTES.get(id.toString());
        if (builtin != null) {
            ResourceLocation builtinParent = ResourceLocation.parse(builtin);
            if (!isForceExcluded(builtinParent)) {
                return builtinParent;
            }
        }
        if (SimulatedSupport.isLoaded() && !isForceExcluded(SimulatedSupport.MAIN_TAB)
                && AddonDetection.dependsOn(id, SimulatedSupport.MOD_ID)
                && !AddonDetection.dependsOn(id, AddonDetection.CREATE)) {
            return SimulatedSupport.MAIN_TAB;
        }
        ResourceLocation fallback = createaddonorganizer.defaultHub();
        return fallback == null || isForceExcluded(fallback) ? null : fallback;
    }

    public static Set<ResourceLocation> allRouteTargets() {
        Set<ResourceLocation> targets = new HashSet<>();
        for (String entry : ROUTES.get()) {
            String[] parts = entry.split(">", 2);
            if (parts.length == 2) {
                ResourceLocation parent = ResourceLocation.tryParse(parts[1].trim());
                if (parent != null && !isForceExcluded(parent)) {
                    targets.add(parent);
                }
            }
        }
        for (ResourceLocation packTarget : PackDefaults.routeTargets()) {
            if (!isForceExcluded(packTarget)) {
                targets.add(packTarget);
            }
        }
        for (String parent : BUILTIN_ROUTES.values()) {
            ResourceLocation p = ResourceLocation.parse(parent);
            if (!isForceExcluded(p)) {
                targets.add(p);
            }
        }
        return targets;
    }

    public static void addForceInclude(ResourceLocation id) {
        if (contains(FORCE_INCLUDE.get(), id)) {
            return;
        }
        List<String> updated = new ArrayList<>(FORCE_INCLUDE.get());
        updated.add(id.toString());
        FORCE_INCLUDE.set(updated);
        SPEC.save();
    }

    public static void removeForceInclude(ResourceLocation id) {
        if (!contains(FORCE_INCLUDE.get(), id)) {
            return;
        }
        FORCE_INCLUDE.set(withoutValue(FORCE_INCLUDE.get(), id));
        SPEC.save();
    }

    public static void addForceExclude(ResourceLocation id) {
        if (contains(FORCE_EXCLUDE.get(), id)) {
            return;
        }
        List<String> updated = new ArrayList<>(FORCE_EXCLUDE.get());
        updated.add(id.toString());
        FORCE_EXCLUDE.set(updated);
        SPEC.save();
    }

    public static void removeForceExclude(ResourceLocation id) {
        if (!contains(FORCE_EXCLUDE.get(), id)) {
            return;
        }
        FORCE_EXCLUDE.set(withoutValue(FORCE_EXCLUDE.get(), id));
        SPEC.save();
    }

    public static void setRoute(ResourceLocation id, ResourceLocation newParent) {
        List<String> updated = withoutRoute(ROUTES.get(), id);
        updated.add(id + " > " + newParent);
        ROUTES.set(updated);
        SPEC.save();
    }

    public static void routeTo(ResourceLocation id, ResourceLocation hub) {
        if (hub != null && hub.equals(createaddonorganizer.defaultHub()) && PackDefaults.routeFor(id) == null) {
            clearRoute(id);
        } else if (hub != null) {
            setRoute(id, hub);
        }
    }

    public static void clearRoute(ResourceLocation id) {
        if (lookupRoute(ROUTES.get(), id) == null) {
            return;
        }
        ROUTES.set(withoutRoute(ROUTES.get(), id));
        SPEC.save();
    }

    public static List<ResourceLocation> subSectionsRoutedTo(ResourceLocation parent) {
        String target = parent.toString();
        List<ResourceLocation> out = new ArrayList<>();
        for (String entry : ROUTES.get()) {
            String[] parts = entry.split(">", 2);
            if (parts.length == 2 && target.equals(parts[1].trim())) {
                ResourceLocation id = ResourceLocation.tryParse(parts[0].trim());
                if (id != null) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    public static void clearRoutesTo(ResourceLocation parent) {
        String target = parent.toString();
        List<String> updated = new ArrayList<>();
        boolean changed = false;
        for (String entry : ROUTES.get()) {
            String[] parts = entry.split(">", 2);
            if (parts.length == 2 && target.equals(parts[1].trim())) {
                changed = true;
                continue;
            }
            updated.add(entry);
        }
        if (changed) {
            ROUTES.set(updated);
            SPEC.save();
        }
    }

    public static void addExtraMainSection(ResourceLocation id) {
        if (contains(EXTRA_MAIN_SECTIONS.get(), id)) {
            return;
        }
        List<String> updated = new ArrayList<>(EXTRA_MAIN_SECTIONS.get());
        updated.add(id.toString());
        EXTRA_MAIN_SECTIONS.set(updated);
        SPEC.save();
    }

    public static void removeExtraMainSection(ResourceLocation id) {
        if (!contains(EXTRA_MAIN_SECTIONS.get(), id)) {
            return;
        }
        EXTRA_MAIN_SECTIONS.set(withoutValue(EXTRA_MAIN_SECTIONS.get(), id));
        SPEC.save();
    }

    public static Set<ResourceLocation> extraMainSections() {
        Set<ResourceLocation> out = new HashSet<>();
        for (String entry : EXTRA_MAIN_SECTIONS.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry);
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    public static void setSectionOrder(List<ResourceLocation> ids) {
        List<String> updated = new ArrayList<>();
        for (ResourceLocation id : ids) {
            updated.add(id.toString());
        }
        SECTION_ORDER.set(updated);
        SPEC.save();
    }

    public static List<ResourceLocation> applyOrderStable(List<ResourceLocation> ids) {
        List<? extends String> order = effectiveSectionOrder();
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            index.put(order.get(i), i);
        }
        List<ResourceLocation> out = new ArrayList<>(ids);
        out.sort(Comparator.comparingInt(id -> index.getOrDefault(id.toString(), Integer.MAX_VALUE)));
        return out;
    }

    public static boolean isNativeSeeded(ResourceLocation id) {
        return contains(NATIVE_SEEDED.get(), id);
    }

    public static void markNativeSeeded(ResourceLocation id) {
        if (contains(NATIVE_SEEDED.get(), id)) {
            return;
        }
        List<String> updated = new ArrayList<>(NATIVE_SEEDED.get());
        updated.add(id.toString());
        NATIVE_SEEDED.set(updated);
        SPEC.save();
    }

    public static void clearNativeSeeded(ResourceLocation id) {
        if (!contains(NATIVE_SEEDED.get(), id)) {
            return;
        }
        NATIVE_SEEDED.set(withoutValue(NATIVE_SEEDED.get(), id));
        SPEC.save();
    }

    public static boolean bannerDrawDiagnostics() {
        return BANNER_DRAW_DIAGNOSTICS.get();
    }

    public static final int NATIVE_SEED_CURRENT = 3;

    public static boolean nativeSeedOutdated() {
        return NATIVE_SEED_VERSION.get() < NATIVE_SEED_CURRENT;
    }

    public static void finishNativeSeed() {
        if (!nativeSeedOutdated()) {
            return;
        }
        NATIVE_SEED_VERSION.set(NATIVE_SEED_CURRENT);
        SPEC.save();
    }

    private static List<? extends String> effectiveSectionOrder() {
        List<? extends String> order = SECTION_ORDER.get();
        return order.isEmpty() ? PackDefaults.orderStrings() : order;
    }

    public static boolean sectionOrderContains(ResourceLocation id) {
        return effectiveSectionOrder().contains(id.toString());
    }

    public static List<ResourceLocation> applyOrder(List<ResourceLocation> ids, Function<ResourceLocation, String> nameOf) {
        List<? extends String> order = effectiveSectionOrder();
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            index.put(order.get(i), i);
        }
        List<ResourceLocation> out = new ArrayList<>(ids);
        out.sort(Comparator.<ResourceLocation>comparingInt(id -> index.getOrDefault(id.toString(), Integer.MAX_VALUE))
                .thenComparing(nameOf, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    public static boolean rainbowMode() {
        return RAINBOW_MODE.get();
    }

    public static void setRainbowMode(boolean value) {
        RAINBOW_MODE.set(value);
        SPEC.save();
    }

    public static ColorSpec bannerColorFor(ResourceLocation id) {
        if (rainbowMode()) {
            List<ResourceLocation> ordered = rainbowOrder();
            return ColorSpec.solid(rainbowBannerColor(ordered.indexOf(id), ordered.size()));
        }
        ColorSpec override = lookupColorSpec(SECTION_COLORS.get(), id, true);
        if (override != null) {
            return override;
        }
        ColorSpec pack = packColorSpec(id);
        return pack != null ? pack : defaultBannerSpec();
    }

    private static ColorSpec packColorSpec(ResourceLocation id) {
        String raw = PackDefaults.colorFor(id);
        return raw == null ? null : parseColorSpecEntry(raw, true);
    }

    public static ColorSpec defaultBannerSpec() {
        return composeDefaultSpec(DEFAULT_BANNER_COLOR.get(), DEFAULT_BANNER_GRADIENT.get(), true);
    }

    public static boolean hasColorOverride(ResourceLocation id) {
        return lookupColorSpec(SECTION_COLORS.get(), id, true) != null;
    }

    public static void setSectionColor(ResourceLocation id, ColorSpec spec) {
        List<String> updated = withoutEntry(SECTION_COLORS.get(), id);
        updated.add(id + " = " + formatColorSpec(spec, true));
        SECTION_COLORS.set(updated);
        SPEC.save();
    }

    public static String formatHex(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    private static final String BANNER_NONE = "none";

    public static String bannerRefFor(ResourceLocation id) {
        String player = lookupValue(BANNERS.get(), id);
        if (player != null) {
            return BANNER_NONE.equals(player) ? null : player;
        }
        return PackDefaults.bannerFor(id);
    }

    public static boolean hasOwnBanner(ResourceLocation id) {
        return lookupValue(BANNERS.get(), id) != null;
    }

    public static boolean hasBanner(ResourceLocation id) {
        return bannerRefFor(id) != null;
    }

    public static boolean tintedTextBoxFor(ResourceLocation id) {
        Boolean override = lookupBoolean(TEXT_BOX_SECTIONS.get(), id);
        return override != null ? override : TINTED_TEXT_BOX.get();
    }

    public static void setTintedTextBoxFor(ResourceLocation id, boolean value) {
        List<String> updated = withoutEntry(TEXT_BOX_SECTIONS.get(), id);
        updated.add(id + " = " + value);
        TEXT_BOX_SECTIONS.set(updated);
        SPEC.save();
    }

    public static void clearTintedTextBoxFor(ResourceLocation id) {
        if (lookupBoolean(TEXT_BOX_SECTIONS.get(), id) == null) {
            return;
        }
        TEXT_BOX_SECTIONS.set(withoutEntry(TEXT_BOX_SECTIONS.get(), id));
        SPEC.save();
    }

    public static boolean tintedTextBox() {
        return TINTED_TEXT_BOX.get();
    }

    public static boolean showCollapseToggle() {
        return SHOW_COLLAPSE_TOGGLE.get();
    }

    public static boolean gridBridge() {
        return GRID_BRIDGE.get();
    }

    public static boolean stickySectionBanners() {
        return STICKY_SECTION_BANNERS.get();
    }

    public static boolean cacheItemIcons() {
        try {
            return !SPEC.isLoaded() || CACHE_ITEM_ICONS.get();
        } catch (Throwable t) {
            return true;
        }
    }

    public static int menuFramerate() {
        try {
            return SPEC.isLoaded() ? MENU_FRAMERATE.get() : 60;
        } catch (Throwable t) {
            return 60;
        }
    }

    public static boolean showItemGroupMarkers() {
        return ITEM_GROUP_MARKERS.get();
    }

    public static boolean isItemGroupOpen(String key) {
        if (key == null) {
            return false;
        }
        for (String entry : OPEN_ITEM_GROUPS.get()) {
            if (key.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    public static void setItemGroupOpen(String key, boolean open) {
        if (key == null || open == isItemGroupOpen(key)) {
            return;
        }
        List<String> updated = new ArrayList<>(OPEN_ITEM_GROUPS.get());
        if (open) {
            updated.add(key);
        } else {
            updated.remove(key);
        }
        OPEN_ITEM_GROUPS.set(updated);
        SPEC.save();
    }

    public static boolean isSectionCollapsed(ResourceLocation id) {
        return contains(COLLAPSED_SECTIONS.get(), id);
    }

    public static void setSectionCollapsed(ResourceLocation id, boolean collapsed) {
        if (collapsed == isSectionCollapsed(id)) {
            return;
        }
        List<String> updated;
        if (collapsed) {
            updated = new ArrayList<>(COLLAPSED_SECTIONS.get());
            updated.add(id.toString());
        } else {
            updated = withoutValue(COLLAPSED_SECTIONS.get(), id);
        }
        COLLAPSED_SECTIONS.set(updated);
        SPEC.save();
    }

    public static boolean classicOrganizerLayout() {
        return CLASSIC_ORGANIZER_LAYOUT.get();
    }

    public static void setTintedTextBox(boolean value) {
        TINTED_TEXT_BOX.set(value);
        SPEC.save();
    }

    public static int boxColorFor(ResourceLocation id) {
        Integer override = lookupColor(BOX_COLORS.get(), id);
        return override != null ? override : DEFAULT_BOX_COLOR.get();
    }

    public static void setBoxColor(ResourceLocation id, int argb) {
        List<String> updated = withoutEntry(BOX_COLORS.get(), id);
        updated.add(id + " = " + formatHex(argb));
        BOX_COLORS.set(updated);
        SPEC.save();
    }

    public static String boxTextureRefFor(ResourceLocation id) {
        return lookupValue(BOX_TEXTURES.get(), id);
    }

    public static boolean hasBoxTexture(ResourceLocation id) {
        return boxTextureRefFor(id) != null;
    }

    public static void setSectionBoxTexture(ResourceLocation id, String ref) {
        List<String> updated = withoutEntry(BOX_TEXTURES.get(), id);
        updated.add(id + " = " + ref);
        BOX_TEXTURES.set(updated);
        SPEC.save();
    }

    public static void clearSectionBoxTexture(ResourceLocation id) {
        if (boxTextureRefFor(id) == null) {
            return;
        }
        BOX_TEXTURES.set(withoutEntry(BOX_TEXTURES.get(), id));
        SPEC.save();
    }

    public static ColorSpec textColorFor(ResourceLocation id) {
        if (rainbowMode()) {
            List<ResourceLocation> ordered = rainbowOrder();
            return ColorSpec.solid(rainbowTextColor(ordered.indexOf(id), ordered.size()));
        }
        ColorSpec override = lookupColorSpec(TEXT_COLORS.get(), id, false);
        return override != null ? override : defaultTextSpec();
    }

    public static ColorSpec defaultTextSpec() {
        return composeDefaultSpec(DEFAULT_TEXT_COLOR.get(), DEFAULT_TEXT_GRADIENT.get(), false);
    }

    public static void setTextColor(ResourceLocation id, ColorSpec spec) {
        List<String> updated = withoutEntry(TEXT_COLORS.get(), id);
        updated.add(id + " = " + formatColorSpec(spec, false));
        TEXT_COLORS.set(updated);
        SPEC.save();
    }

    public static IndexPanelStyle indexPanelStyle() {
        return INDEX_PANEL_STYLE.get();
    }

    public static void setIndexPanelStyle(IndexPanelStyle style) {
        INDEX_PANEL_STYLE.set(style);
        SPEC.save();
    }

    public static SidebarSide colorsSidebarSide() {
        return COLORS_SIDEBAR_SIDE.get();
    }

    public static void setColorsSidebarSide(SidebarSide side) {
        COLORS_SIDEBAR_SIDE.set(side);
        SPEC.save();
    }

    public static ArrangerStyle arrangerStyle() {
        return ARRANGER_STYLE.get();
    }

    public static void setArrangerStyle(ArrangerStyle style) {
        ARRANGER_STYLE.set(style);
        SPEC.save();
    }

    public static MenuStyle menuStyle() {
        return MENU_STYLE.get();
    }

    public static void setMenuStyle(MenuStyle style) {
        MENU_STYLE.set(style);
        SPEC.save();
    }

    public static boolean menuStyleTransparent() {
        return MENU_STYLE_TRANSPARENT.get();
    }

    public static void setMenuStyleTransparent(boolean transparent) {
        MENU_STYLE_TRANSPARENT.set(transparent);
        SPEC.save();
    }

    public static final int MENU_ACCENT_NEUTRAL = -1;
    public static final int MENU_ACCENT_DEFAULT = 205;

    public static int menuAccentHue() {
        return MENU_ACCENT_HUE.get();
    }

    public static void setMenuAccentHue(int hue) {
        MENU_ACCENT_HUE.set(hue);
        SPEC.save();
    }

    public static void setMenuAccentHueLive(int hue) {
        MENU_ACCENT_HUE.set(hue);
    }

    public static final int MENU_SATURATION_DEFAULT = 100;
    public static final int MENU_SATURATION_MAX = 200;

    private static ModConfigSpec.IntValue accentSaturationFor(MenuStyle style) {
        return switch (style) {
            case ESSENTIAL -> MENU_ACCENT_SATURATION_ESSENTIAL;
            case DEFAULT_MODERN -> MENU_ACCENT_SATURATION_MODERN;
            case DEFAULT -> MENU_ACCENT_SATURATION_DEFAULT;
            default -> MENU_ACCENT_SATURATION_CREATE;
        };
    }

    public static int menuAccentSaturation() {
        return accentSaturationFor(menuStyle()).get();
    }

    public static void setMenuAccentSaturation(int percent) {
        accentSaturationFor(menuStyle()).set(Math.clamp(percent, 0, MENU_SATURATION_MAX));
        SPEC.save();
    }

    public static void setMenuAccentSaturationLive(int percent) {
        accentSaturationFor(menuStyle()).set(Math.clamp(percent, 0, MENU_SATURATION_MAX));
    }

    public static void resetMenuAccentSaturation() {
        ModConfigSpec.IntValue value = accentSaturationFor(menuStyle());
        value.set(value.getDefault());
        SPEC.save();
    }

    public static SfxStyle sfxStyle() {
        return SFX_STYLE.get();
    }

    public static boolean sfxOn(ModConfigSpec.BooleanValue toggle) {
        return SFX_ENABLED.get() && toggle.get();
    }

    public static boolean swooshOn(ModConfigSpec.BooleanValue route) {
        return animOn(SWOOSH_ENABLED) && route.get();
    }

    public static int swooshOutMs() {
        return SWOOSH_OUT_MS.get();
    }

    public static int swooshHoldMs() {
        return SWOOSH_HOLD_MS.get();
    }

    public static int swooshInMs() {
        return SWOOSH_IN_MS.get();
    }

    public static int swooshTravel() {
        return SWOOSH_TRAVEL.get();
    }

    public static SwooshDirection swooshDepthDirection() {
        return SWOOSH_DEPTH_DIRECTION.get();
    }

    public static void setSwooshDepthDirection(SwooshDirection value) {
        SWOOSH_DEPTH_DIRECTION.set(value);
    }

    public static boolean swooshFade() {
        return SWOOSH_FADE.get();
    }

    public static SwooshExit swooshExitCurve() {
        return SWOOSH_EXIT_CURVE.get();
    }

    public static SwooshEnter swooshEnterCurve() {
        return SWOOSH_ENTER_CURVE.get();
    }

    public static void setSwooshOutMs(int value) {
        SWOOSH_OUT_MS.set(Math.clamp(value, 40, 600));
    }

    public static void setSwooshHoldMs(int value) {
        SWOOSH_HOLD_MS.set(Math.clamp(value, 0, 300));
    }

    public static void setSwooshInMs(int value) {
        SWOOSH_IN_MS.set(Math.clamp(value, 40, 800));
    }

    public static void setSwooshTravel(int value) {
        SWOOSH_TRAVEL.set(Math.clamp(value, 4, 320));
    }

    public static void setSwooshFade(boolean value) {
        SWOOSH_FADE.set(value);
    }

    public static void setSwooshExitCurve(SwooshExit value) {
        SWOOSH_EXIT_CURVE.set(value);
    }

    public static void setSwooshEnterCurve(SwooshEnter value) {
        SWOOSH_ENTER_CURVE.set(value);
    }

    public static void saveSwoosh() {
        SPEC.save();
    }

    public static boolean animOn(ModConfigSpec.BooleanValue toggle) {
        return ANIM_ENABLED.get() && toggle.get();
    }

    public static void setSfxStyle(SfxStyle style) {
        SFX_STYLE.set(style);
        SPEC.save();
    }

    public static ArrangerLayout arrangerLayout() {
        return ARRANGER_LAYOUT.get();
    }

    public static void setArrangerLayout(ArrangerLayout layout) {
        ARRANGER_LAYOUT.set(layout);
        SPEC.save();
    }

    public static List<ResourceLocation> tabOrder() {
        List<ResourceLocation> out = new ArrayList<>();
        for (String entry : TAB_ORDER.get()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.trim());
            if (id != null) {
                out.add(id);
            }
        }
        return out;
    }

    public static void setTabOrder(List<ResourceLocation> ids) {
        List<String> out = new ArrayList<>(ids.size());
        for (ResourceLocation id : ids) {
            out.add(id.toString());
        }
        TAB_ORDER.set(out);
        SPEC.save();
    }

    public static boolean editorHintSeen() {
        return EDITOR_HINT_SEEN.get();
    }

    public static void setEditorHintSeen(boolean value) {
        EDITOR_HINT_SEEN.set(value);
        SPEC.save();
    }

    public static boolean bannerEditorPreviewTop() {
        return BANNER_EDITOR_PREVIEW_TOP.get();
    }

    public static int gradientCellSize() {
        return GRADIENT_CELL_SIZE.get();
    }

    public static ColorSpec textSecondaryColorFor(ResourceLocation id) {
        if (!TWO_TONE_TEXT.get()) {
            return null;
        }
        if (rainbowMode()) {
            List<ResourceLocation> ordered = rainbowOrder();
            return ColorSpec.solid(rainbowTextSecondaryColor(ordered.indexOf(id), ordered.size()));
        }
        ColorSpec override = lookupColorSpec(TEXT_SECONDARY_COLORS.get(), id, false);
        return override != null ? override : defaultTextSecondarySpec();
    }

    public static ColorSpec defaultTextSecondarySpec() {
        return composeDefaultSpec(DEFAULT_TEXT_SECONDARY_COLOR.get(), DEFAULT_TEXT_SECONDARY_GRADIENT.get(), false);
    }

    public static int rainbowBannerColor(int index, int total) {
        return 0xFF000000 | ColorUtil.hsvToRgb(rainbowHue(index, total), 0.65f, 0.55f);
    }

    public static int rainbowTextColor(int index, int total) {
        return 0xFF000000 | ColorUtil.hsvToRgb(rainbowHue(index, total), 0.25f, 1.0f);
    }

    public static int rainbowTextSecondaryColor(int index, int total) {
        return 0xFF000000 | ColorUtil.hsvToRgb(rainbowHue(index, total), 0.75f, 0.75f);
    }

    private static float rainbowHue(int index, int total) {
        if (index < 0 || total <= 1) {
            return 0f;
        }
        return (float) index / total;
    }

    private static final long RAINBOW_ORDER_CACHE_TTL_MS = 250;
    private static List<ResourceLocation> rainbowOrderCache = List.of();
    private static long rainbowOrderCacheAt = -RAINBOW_ORDER_CACHE_TTL_MS;

    private static List<ResourceLocation> rainbowOrder() {
        long now = System.currentTimeMillis();
        if (now - rainbowOrderCacheAt < RAINBOW_ORDER_CACHE_TTL_MS) {
            return rainbowOrderCache;
        }
        List<ResourceLocation> ordered = new ArrayList<>();
        for (SectionCatalog.Entry entry : SectionCatalog.colorables()) {
            if (!entry.readOnly()) {
                ordered.add(entry.id());
            }
        }
        rainbowOrderCache = ordered;
        rainbowOrderCacheAt = now;
        return ordered;
    }

    public static void setTextSecondaryColor(ResourceLocation id, ColorSpec spec) {
        List<String> updated = withoutEntry(TEXT_SECONDARY_COLORS.get(), id);
        updated.add(id + " = " + formatColorSpec(spec, false));
        TEXT_SECONDARY_COLORS.set(updated);
        SPEC.save();
    }

    public static void clearTextSecondaryColor(ResourceLocation id) {
        if (textSecondaryColorFor(id) == null) {
            return;
        }
        TEXT_SECONDARY_COLORS.set(withoutEntry(TEXT_SECONDARY_COLORS.get(), id));
        SPEC.save();
    }

    public static boolean titleTextShadow(ResourceLocation id) {
        Boolean override = lookupBoolean(TITLE_TEXT_SHADOW_SECTIONS.get(), id);
        return override != null ? override : TITLE_TEXT_SHADOW.get();
    }

    public static void setTitleTextShadow(ResourceLocation id, boolean shadow) {
        List<String> updated = withoutEntry(TITLE_TEXT_SHADOW_SECTIONS.get(), id);
        updated.add(id + " = " + shadow);
        TITLE_TEXT_SHADOW_SECTIONS.set(updated);
        SPEC.save();
    }

    public static ColorSpec textOutlineColorFor(ResourceLocation id) {
        return lookupColorSpec(TEXT_OUTLINE_COLORS.get(), id, false);
    }

    public static ColorSpec defaultTextOutlineSpec() {
        return composeDefaultSpec(DEFAULT_TEXT_OUTLINE_COLOR.get(), DEFAULT_TEXT_OUTLINE_GRADIENT.get(), false);
    }

    public static void setTextOutlineColor(ResourceLocation id, ColorSpec spec) {
        List<String> updated = withoutEntry(TEXT_OUTLINE_COLORS.get(), id);
        updated.add(id + " = " + formatColorSpec(spec, false));
        TEXT_OUTLINE_COLORS.set(updated);
        SPEC.save();
    }

    public static void clearTextOutlineColor(ResourceLocation id) {
        if (textOutlineColorFor(id) == null) {
            return;
        }
        TEXT_OUTLINE_COLORS.set(withoutEntry(TEXT_OUTLINE_COLORS.get(), id));
        SPEC.save();
    }

    public static Integer textShadowColorFor(ResourceLocation id) {
        return lookupColor(TEXT_SHADOW_COLORS.get(), id);
    }

    public static void setTextShadowColor(ResourceLocation id, int argb) {
        List<String> updated = withoutEntry(TEXT_SHADOW_COLORS.get(), id);
        updated.add(id + " = " + formatHex(argb));
        TEXT_SHADOW_COLORS.set(updated);
        SPEC.save();
    }

    public static void clearTextShadowColor(ResourceLocation id) {
        if (textShadowColorFor(id) == null) {
            return;
        }
        TEXT_SHADOW_COLORS.set(withoutEntry(TEXT_SHADOW_COLORS.get(), id));
        SPEC.save();
    }

    public static float twoToneSplitFor(ResourceLocation id) {
        Float override = lookupFraction(TEXT_SPLITS.get(), id);
        return override != null ? override : DEFAULT_TWO_TONE_SPLIT.get().floatValue();
    }

    public static void setTwoToneSplit(ResourceLocation id, float fraction) {
        List<String> updated = withoutEntry(TEXT_SPLITS.get(), id);
        updated.add(id + " = " + fraction);
        TEXT_SPLITS.set(updated);
        SPEC.save();
    }

    public static void clearTwoToneSplit(ResourceLocation id) {
        if (lookupFraction(TEXT_SPLITS.get(), id) == null) {
            return;
        }
        TEXT_SPLITS.set(withoutEntry(TEXT_SPLITS.get(), id));
        SPEC.save();
    }

    public static float scrollCutoffFor(ResourceLocation id) {
        Float override = lookupFraction(SCROLL_CUTOFFS.get(), id);
        return override != null ? override : DEFAULT_SCROLL_CUTOFF.get().floatValue();
    }

    public static void setScrollCutoff(ResourceLocation id, float fraction) {
        List<String> updated = withoutEntry(SCROLL_CUTOFFS.get(), id);
        updated.add(id + " = " + fraction);
        SCROLL_CUTOFFS.set(updated);
        SPEC.save();
    }

    public static float boxDarkenFor(ResourceLocation id) {
        Float override = lookupFraction(BOX_DARKENS.get(), id);
        return override != null ? override : DEFAULT_BOX_DARKEN.get().floatValue();
    }

    public static void setBoxDarken(ResourceLocation id, float fraction) {
        List<String> updated = withoutEntry(BOX_DARKENS.get(), id);
        updated.add(id + " = " + fraction);
        BOX_DARKENS.set(updated);
        SPEC.save();
    }

    public static float boxOpacityFor(ResourceLocation id) {
        Float override = lookupFraction(BOX_OPACITIES.get(), id);
        return override != null ? override : DEFAULT_BOX_OPACITY.get().floatValue();
    }

    public static void setBoxOpacity(ResourceLocation id, float fraction) {
        List<String> updated = withoutEntry(BOX_OPACITIES.get(), id);
        updated.add(id + " = " + fraction);
        BOX_OPACITIES.set(updated);
        SPEC.save();
    }

    public static Integer highlightColorFor(ResourceLocation id) {
        return lookupColor(HIGHLIGHT_COLORS.get(), id);
    }

    public static void setHighlightColor(ResourceLocation id, int argb) {
        List<String> updated = withoutEntry(HIGHLIGHT_COLORS.get(), id);
        updated.add(id + " = " + formatHex(argb));
        HIGHLIGHT_COLORS.set(updated);
        SPEC.save();
    }

    public static void clearHighlightColor(ResourceLocation id) {
        if (highlightColorFor(id) == null) {
            return;
        }
        HIGHLIGHT_COLORS.set(withoutEntry(HIGHLIGHT_COLORS.get(), id));
        SPEC.save();
    }

    public static boolean showAllBanners() {
        return SHOW_ALL_BANNERS.get();
    }

    public static void setShowAllBanners(boolean value) {
        SHOW_ALL_BANNERS.set(value);
        SPEC.save();
    }

    public static boolean fetchOnlineBanners() {
        return FETCH_ONLINE_BANNERS.get();
    }

    public static String bannerManifestUrl() {
        return BANNER_MANIFEST_URL.get();
    }

    public static String bannerPoolsManifestUrl() {
        return BANNER_POOLS_MANIFEST_URL.get();
    }

    public static boolean fetchOnlineBoxTextures() {
        return FETCH_ONLINE_BOX_TEXTURES.get();
    }

    public static String boxManifestUrl() {
        return BOX_MANIFEST_URL.get();
    }

    public static List<String> extraPoolFor(ResourceLocation id) {
        String key = id.toString();
        List<String> out = new ArrayList<>();
        for (String entry : EXTRA_BANNER_POOL.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0].trim())) {
                out.add(parts[1].trim());
            }
        }
        return out;
    }

    public static void addExtraPoolEntry(ResourceLocation id, String ref) {
        if (extraPoolFor(id).contains(ref)) {
            return;
        }
        List<String> updated = new ArrayList<>(EXTRA_BANNER_POOL.get());
        updated.add(id + " = " + ref);
        EXTRA_BANNER_POOL.set(updated);
        SPEC.save();
    }

    public static void removeExtraPoolEntriesForRef(String ref) {
        List<String> updated = new ArrayList<>();
        boolean changed = false;
        for (String entry : EXTRA_BANNER_POOL.get()) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && ref.equals(parts[1].trim())) {
                changed = true;
                continue;
            }
            updated.add(entry);
        }
        if (changed) {
            EXTRA_BANNER_POOL.set(updated);
            SPEC.save();
        }
    }

    public static void setSectionBanner(ResourceLocation id, String ref) {
        List<String> updated = withoutEntry(BANNERS.get(), id);
        updated.add(id + " = " + ref);
        BANNERS.set(updated);
        SPEC.save();
    }

    public static void setSectionBanners(Map<ResourceLocation, String> refs) {
        List<String> updated = new ArrayList<>(BANNERS.get());
        for (Map.Entry<ResourceLocation, String> e : refs.entrySet()) {
            updated = withoutEntry(updated, e.getKey());
            updated.add(e.getKey() + " = " + e.getValue());
        }
        BANNERS.set(updated);
        SPEC.save();
    }

    public static void clearSectionBanner(ResourceLocation id) {
        String own = lookupValue(BANNERS.get(), id);
        if (PackDefaults.bannerFor(id) != null) {
            if (BANNER_NONE.equals(own)) {
                return;
            }
            List<String> updated = withoutEntry(BANNERS.get(), id);
            updated.add(id + " = " + BANNER_NONE);
            BANNERS.set(updated);
            SPEC.save();
            return;
        }
        if (own == null) {
            return;
        }
        BANNERS.set(withoutEntry(BANNERS.get(), id));
        SPEC.save();
    }

    public static Integer animatedFrameTicks(ResourceLocation texture) {
        String raw = lookupValue(ANIMATED_BANNERS.get(), texture);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void setAnimatedBanner(ResourceLocation texture, int frameTicks) {
        List<String> updated = withoutEntry(ANIMATED_BANNERS.get(), texture);
        updated.add(texture + " = " + frameTicks);
        ANIMATED_BANNERS.set(updated);
        SPEC.save();
    }

    public static boolean bannerAlwaysAnimates(ResourceLocation texture) {
        Boolean override = lookupBoolean(ALWAYS_ANIMATED_BANNERS.get(), texture);
        return override != null && override;
    }

    public static void setBannerAlwaysAnimates(ResourceLocation texture, boolean always) {
        List<String> updated = withoutEntry(ALWAYS_ANIMATED_BANNERS.get(), texture);
        if (always) {
            updated.add(texture + " = true");
        }
        ALWAYS_ANIMATED_BANNERS.set(updated);
        SPEC.save();
    }

    public static void clearAnimatedBanner(ResourceLocation texture) {
        if (animatedFrameTicks(texture) == null) {
            return;
        }
        ANIMATED_BANNERS.set(withoutEntry(ANIMATED_BANNERS.get(), texture));
        SPEC.save();
    }

    private static boolean isValidBanner(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        return parts.length == 2
                && ResourceLocation.tryParse(parts[0].trim()) != null
                && !parts[1].trim().isEmpty();
    }

    private static boolean isValidUrl(final Object obj) {
        return obj instanceof String s && (s.startsWith("http://") || s.startsWith("https://"));
    }

    private static boolean isValidAnimatedBanner(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        if (parts.length != 2 || ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        try {
            return Integer.parseInt(parts[1].trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static List<String> withoutEntry(List<? extends String> list, ResourceLocation id) {
        String key = id.toString();
        List<String> out = new ArrayList<>();
        for (String entry : list) {
            String[] parts = entry.split("=", 2);
            if (parts.length == 2 && key.equals(parts[0].trim())) {
                continue;
            }
            out.add(entry);
        }
        return out;
    }

    private static List<ModConfigSpec.ConfigValue<List<? extends String>>> keyedSectionValues() {
        return List.of(SECTION_COLORS, BANNERS, BOX_COLORS, BOX_TEXTURES, TEXT_COLORS, TEXT_SECONDARY_COLORS,
                TEXT_OUTLINE_COLORS, HIGHLIGHT_COLORS, SECTION_NAMES, BOX_DARKENS, BOX_OPACITIES,
                TEXT_SPLITS, SCROLL_CUTOFFS, TEXT_SHADOW_COLORS, TITLE_TEXT_SHADOW_SECTIONS, TEXT_BOX_SECTIONS);
    }

    public static void purgeTabSectionConfig(ResourceLocation tabId) {
        for (ModConfigSpec.ConfigValue<List<? extends String>> value : keyedSectionValues()) {
            List<String> kept = new ArrayList<>();
            for (String entry : value.get()) {
                int at = entry.indexOf('=');
                ResourceLocation key = at < 0 ? null : ResourceLocation.tryParse(entry.substring(0, at).trim());
                if (key != null && tabId.equals(TabLayout.ownerOfSectionId(key))) {
                    continue;
                }
                kept.add(entry);
            }
            value.set(kept);
        }
        SPEC.save();
    }

    public static void purgeSectionConfig(ResourceLocation id) {
        for (ModConfigSpec.ConfigValue<List<? extends String>> value : keyedSectionValues()) {
            value.set(withoutEntry(value.get(), id));
        }
        List<ModConfigSpec.ConfigValue<List<? extends String>>> plain = List.of(
                SECTION_ORDER, COLLAPSED_SECTIONS, TAB_ORDER,
                FORCE_INCLUDE, FORCE_EXCLUDE, EXTRA_MAIN_SECTIONS);
        for (ModConfigSpec.ConfigValue<List<? extends String>> value : plain) {
            value.set(withoutValue(value.get(), id));
        }
        String target = id.toString();
        List<String> routes = new ArrayList<>();
        for (String entry : ROUTES.get()) {
            String[] parts = entry.split(">", 2);
            if (parts.length == 2 && (target.equals(parts[0].trim()) || target.equals(parts[1].trim()))) {
                continue;
            }
            routes.add(entry);
        }
        ROUTES.set(routes);
        SPEC.save();
    }

    private static List<String> withoutValue(List<? extends String> list, ResourceLocation id) {
        String target = id.toString();
        List<String> out = new ArrayList<>();
        for (String entry : list) {
            if (!target.equals(entry)) {
                out.add(entry);
            }
        }
        return out;
    }

    private static List<String> withoutRoute(List<? extends String> list, ResourceLocation id) {
        String target = id.toString();
        List<String> out = new ArrayList<>();
        for (String entry : list) {
            String[] parts = entry.split(">", 2);
            if (parts.length == 2 && target.equals(parts[0].trim())) {
                continue;
            }
            out.add(entry);
        }
        return out;
    }

    private static boolean isValidSectionColor(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        return parts.length == 2
                && ResourceLocation.tryParse(parts[0].trim()) != null
                && parseColor(parts[1]) != null;
    }

    private static boolean isValidBannerColorSpecEntry(final Object obj) {
        return isValidColorSpecEntry(obj, true);
    }

    private static boolean isValidTextColorSpecEntry(final Object obj) {
        return isValidColorSpecEntry(obj, false);
    }

    private static boolean isValidColorSpecEntry(final Object obj, boolean supportsStyle) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        return parts.length == 2
                && ResourceLocation.tryParse(parts[0].trim()) != null
                && parseColorSpecEntry(parts[1].trim(), supportsStyle) != null;
    }

    private static boolean isValidSectionBoolean(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        return parts.length == 2
                && ResourceLocation.tryParse(parts[0].trim()) != null
                && ("true".equalsIgnoreCase(parts[1].trim()) || "false".equalsIgnoreCase(parts[1].trim()));
    }

    private static boolean isValidSectionFraction(final Object obj) {
        if (!(obj instanceof String s)) {
            return false;
        }
        String[] parts = s.split("=", 2);
        if (parts.length != 2 || ResourceLocation.tryParse(parts[0].trim()) == null) {
            return false;
        }
        try {
            float f = Float.parseFloat(parts[1].trim());
            return f >= 0f && f <= 1f;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static final int INDEX_CACHE_LIMIT = 48;
    private static final Map<List<? extends String>, Map<String, String>> INDEX_CACHE = new IdentityHashMap<>();

    private static synchronized Map<String, String> index(List<? extends String> list, char separator) {
        Map<String, String> cached = INDEX_CACHE.get(list);
        if (cached != null) {
            return cached;
        }
        Map<String, String> built = new HashMap<>();
        for (String entry : list) {
            int at = entry.indexOf(separator);
            if (at < 0) {
                continue;
            }
            built.putIfAbsent(entry.substring(0, at).trim(), entry.substring(at + 1).trim());
        }
        if (INDEX_CACHE.size() >= INDEX_CACHE_LIMIT) {
            INDEX_CACHE.clear();
        }
        INDEX_CACHE.put(list, built);
        return built;
    }

    private static String lookupValue(List<? extends String> list, ResourceLocation id) {
        return index(list, '=').get(id.toString());
    }

    private static Integer lookupColor(List<? extends String> list, ResourceLocation id) {
        String raw = lookupValue(list, id);
        return raw == null ? null : parseColor(raw);
    }

    private static ColorSpec lookupColorSpec(List<? extends String> list, ResourceLocation id, boolean supportsStyle) {
        String raw = lookupValue(list, id);
        return raw == null ? null : parseColorSpecEntry(raw, supportsStyle);
    }

    private static ColorSpec composeDefaultSpec(int color1, String gradientSuffix, boolean supportsStyle) {
        if (gradientSuffix == null || gradientSuffix.isEmpty()) {
            return ColorSpec.solid(color1);
        }
        ColorSpec parsed = parseColorSpecEntry(formatHex(color1) + "|" + gradientSuffix, supportsStyle);
        return parsed != null ? parsed : ColorSpec.solid(color1);
    }

    public static String formatColorSpec(ColorSpec spec, boolean includeStyle) {
        if (!spec.isGradient()) {
            return formatHex(spec.color1());
        }
        String out = formatHex(spec.color1()) + "|" + formatHex(spec.color2()) + "|" + spec.direction().name();
        if (includeStyle) {
            out += "|" + spec.style().name();
        }
        return out;
    }

    public static ColorSpec parseColorSpecEntry(String raw, boolean supportsStyle) {
        String[] parts = raw.split("\\|");
        if (parts.length == 1) {
            Integer color1 = parseColor(parts[0]);
            return color1 != null ? ColorSpec.solid(color1) : null;
        }
        if (parts.length != 3 && parts.length != 4) {
            return null;
        }
        if (parts.length == 4 && !supportsStyle) {
            return null;
        }
        Integer color1 = parseColor(parts[0]);
        Integer color2 = parseColor(parts[1]);
        if (color1 == null || color2 == null) {
            return null;
        }
        ColorSpec.Direction direction;
        try {
            direction = ColorSpec.Direction.valueOf(parts[2].trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
        ColorSpec.Style style = ColorSpec.Style.SMOOTH;
        if (parts.length == 4) {
            try {
                style = ColorSpec.Style.valueOf(parts[3].trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return new ColorSpec(color1, color2, direction, style);
    }

    private static Boolean lookupBoolean(List<? extends String> list, ResourceLocation id) {
        String raw = lookupValue(list, id);
        return raw == null ? null : Boolean.parseBoolean(raw);
    }

    private static Float lookupFraction(List<? extends String> list, ResourceLocation id) {
        String raw = lookupValue(list, id);
        if (raw == null) {
            return null;
        }
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseColor(String raw) {
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        } else if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        if (s.isEmpty() || s.length() > 8) {
            return null;
        }
        try {
            long value = Long.parseLong(s, 16);
            if (s.length() <= 6) {
                value |= 0xFF000000L;
            }
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static ResourceLocation lookupRoute(List<? extends String> routes, ResourceLocation id) {
        String raw = index(routes, '>').get(id.toString());
        return raw == null ? null : ResourceLocation.tryParse(raw);
    }

    private static boolean contains(List<? extends String> list, ResourceLocation id) {
        String target = id.toString();
        for (String entry : list) {
            if (target.equals(entry)) {
                return true;
            }
        }
        return false;
    }
}

