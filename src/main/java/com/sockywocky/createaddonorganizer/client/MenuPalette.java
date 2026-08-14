package com.sockywocky.createaddonorganizer.client;

public record MenuPalette(
        int overlayNoWorld, int overlayInWorld, int opaqueBackground,
        int boxBackground, int boxBackgroundHover,
        int idleTop, int idleBottom, int hoverTop, int hoverBottom,
        int disabledTop, int disabledBottom, int clickTop, int clickBottom,
        int text, int textHover, int textOff, int header, int hint,
        int scrollTrack, int scrollThumb, int listBackground, int rule, int rowStripe,
        int booleanOn, int booleanOff, int accent, int selectedBackground,
        int titleBandBackground, int titleBandTop, int titleBandBottom,
        int titleTop, int titleMid, int titleBottom,
        boolean cog, boolean nativeBoxes, boolean editIcon, boolean titleBand) {

    public static final MenuPalette CREATE = new MenuPalette(
            0x90282C34, 0xB0282C34, 0xFF282C34,
            0xDD000000, 0xDD000000,
            0xDD8AB6D6, 0x908AB6D6, 0xFF9ABBD3, 0xD09ABBD3,
            0x80909090, 0x60909090, 0xFFFFFFFF, 0xEEFFFFFF,
            0xFF96B7E0, 0xFFFFFFFF, 0xFF909090, 0xFF5391E1, 0xFF96B7E0,
            0x60000000, 0xDD8AB6D6, 0x60000000, 0xDD8AB6D6, 0x14FFFFFF,
            0xFF5BD85B, 0xFFD85B5B, 0xFF9ABBD3, 0xFF141A22,
            0xDD000000, 0xDDA8A8A8, 0x90A8A8A8,
            0xFFC69FBC, 0xFFF6B8BB, 0xFFFBF994,
            true, true, true, true);

    public static final MenuPalette ESSENTIAL = new MenuPalette(
            0xE8181818, 0xF0181818, 0xFF181818,
            0xFF323232, 0xFF474747,
            0xFF474747, 0xFF474747, 0xFF0A82FD, 0xFF0A82FD,
            0xFF2A2A2A, 0xFF2A2A2A, 0xFF6F6F6F, 0xFF6F6F6F,
            0xFFBFBFBF, 0xFFE5E5E5, 0xFF757575, 0xFF0A82FD, 0xFF999999,
            0xFF232323, 0xFF0A82FD, 0xFF232323, 0xCC0A82FD, 0x120A82FD,
            0xFF1299FF, 0xFF757575, 0xFF0A82FD, 0xFF121E30,
            0xFF323232, 0xFF474747, 0xFF474747,
            0xFFE4E4E4, 0xFFE4E4E4, 0xFFE4E4E4,
            false, false, false, false);

    public static final MenuPalette MODERN = new MenuPalette(
            0x8C0A0A0A, 0xA00A0A0A, 0xFF0A0A0A,
            0x66000000, 0x665A5A5A,
            0x4DFFFFFF, 0x4DFFFFFF, 0x99FFFFFF, 0x99FFFFFF,
            0x1FFFFFFF, 0x1FFFFFFF, 0xCCFFFFFF, 0xCCFFFFFF,
            0xFFD8D8D8, 0xFFFFFFFF, 0xFF6E6E6E, 0xFFC89A3E, 0xFF9A9A9A,
            0x33000000, 0xCCC89A3E, 0x8C000000, 0x59C89A3E, 0x14C89A3E,
            0xFFE3B255, 0xFF757575, 0xFFC89A3E, 0x38C89A3E,
            0x59000000, 0x59C89A3E, 0x00C89A3E,
            0xFFF6E7C8, 0xFFE9C98C, 0xFFC89A3E,
            false, false, false, true);

    public static MenuPalette lerp(MenuPalette from, MenuPalette to, float t) {
        if (t >= 0.998f) {
            return to;
        }
        if (t <= 0.002f) {
            return from;
        }
        return new MenuPalette(
                MenuSkin.mixColor(from.overlayNoWorld, to.overlayNoWorld, t),
                MenuSkin.mixColor(from.overlayInWorld, to.overlayInWorld, t),
                MenuSkin.mixColor(from.opaqueBackground, to.opaqueBackground, t),
                MenuSkin.mixColor(from.boxBackground, to.boxBackground, t),
                MenuSkin.mixColor(from.boxBackgroundHover, to.boxBackgroundHover, t),
                MenuSkin.mixColor(from.idleTop, to.idleTop, t),
                MenuSkin.mixColor(from.idleBottom, to.idleBottom, t),
                MenuSkin.mixColor(from.hoverTop, to.hoverTop, t),
                MenuSkin.mixColor(from.hoverBottom, to.hoverBottom, t),
                MenuSkin.mixColor(from.disabledTop, to.disabledTop, t),
                MenuSkin.mixColor(from.disabledBottom, to.disabledBottom, t),
                MenuSkin.mixColor(from.clickTop, to.clickTop, t),
                MenuSkin.mixColor(from.clickBottom, to.clickBottom, t),
                MenuSkin.mixColor(from.text, to.text, t),
                MenuSkin.mixColor(from.textHover, to.textHover, t),
                MenuSkin.mixColor(from.textOff, to.textOff, t),
                MenuSkin.mixColor(from.header, to.header, t),
                MenuSkin.mixColor(from.hint, to.hint, t),
                MenuSkin.mixColor(from.scrollTrack, to.scrollTrack, t),
                MenuSkin.mixColor(from.scrollThumb, to.scrollThumb, t),
                MenuSkin.mixColor(from.listBackground, to.listBackground, t),
                MenuSkin.mixColor(from.rule, to.rule, t),
                MenuSkin.mixColor(from.rowStripe, to.rowStripe, t),
                MenuSkin.mixColor(from.booleanOn, to.booleanOn, t),
                MenuSkin.mixColor(from.booleanOff, to.booleanOff, t),
                MenuSkin.mixColor(from.accent, to.accent, t),
                MenuSkin.mixColor(from.selectedBackground, to.selectedBackground, t),
                MenuSkin.mixColor(from.titleBandBackground, to.titleBandBackground, t),
                MenuSkin.mixColor(from.titleBandTop, to.titleBandTop, t),
                MenuSkin.mixColor(from.titleBandBottom, to.titleBandBottom, t),
                MenuSkin.mixColor(from.titleTop, to.titleTop, t),
                MenuSkin.mixColor(from.titleMid, to.titleMid, t),
                MenuSkin.mixColor(from.titleBottom, to.titleBottom, t),
                t < 0.5f ? from.cog : to.cog,
                t < 0.5f ? from.nativeBoxes : to.nativeBoxes,
                t < 0.5f ? from.editIcon : to.editIcon,
                t < 0.5f ? from.titleBand : to.titleBand);
    }
}
