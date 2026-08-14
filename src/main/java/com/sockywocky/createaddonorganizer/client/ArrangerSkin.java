package com.sockywocky.createaddonorganizer.client;

public record ArrangerSkin(int panel, int panelEdgeLight, int panelEdgeDark, int title,
        int slot, int slotEdgeLight, int slotEdgeDark,
        int tabIdle, int tabIdleEdge, int tabSelected, int tabSelectedEdge,
        int tabLocked, int tabLockedEdge, int scrollTrack, int scrollThumb) {

    public static final ArrangerSkin OUTLINE = new ArrangerSkin(
            0xDB000000, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
            0x8C000000, 0x33FFFFFF, 0x33FFFFFF,
            0xCC000000, 0x80FFFFFF, 0xF0000000, 0xFFFFFFFF,
            0x8C000000, 0x38FFFFFF, 0x1AFFFFFF, 0xFFFFFFFF);

    public static final ArrangerSkin DARK = new ArrangerSkin(
            0xFF262C33, 0xFF3D4650, 0xFF12161A, 0xFFB9C2CA,
            0xFF2B3037, 0xFF464D56, 0xFF14181C,
            0xFF343B43, 0xFF4A525C, 0xFF262C33, 0xFF6E7B88,
            0xFF22272D, 0xFF30373F, 0xFF0D1013, 0xFF5C656F);

    public static final ArrangerSkin VANILLA = new ArrangerSkin(
            0xFFC6C6C6, 0xFFFFFFFF, 0xFF555555, 0xFF404040,
            0xFF8B8B8B, 0xFFFFFFFF, 0xFF373737,
            0xFF8B8B8B, 0xFFB8B8B8, 0xFFC6C6C6, 0xFFFFFFFF,
            0xFF6F6F6F, 0xFF909090, 0xFF000000, 0xFFC0C0C0);

    public static ArrangerSkin current() {
        ArrangerSkin base = switch (com.sockywocky.createaddonorganizer.Config.arrangerStyle()) {
            case VANILLA -> VANILLA;
            case DARK -> DARK;
            default -> OUTLINE;
        };
        if (!MenuSkin.active()) {
            return base;
        }
        return lerp(base, MenuSkin.arrangerSkin(), MenuSkin.blend());
    }

    private static ArrangerSkin lerp(ArrangerSkin from, ArrangerSkin to, float t) {
        if (t >= 0.998f) {
            return to;
        }
        return new ArrangerSkin(
                MenuSkin.mixColor(from.panel, to.panel, t),
                MenuSkin.mixColor(from.panelEdgeLight, to.panelEdgeLight, t),
                MenuSkin.mixColor(from.panelEdgeDark, to.panelEdgeDark, t),
                MenuSkin.mixColor(from.title, to.title, t),
                MenuSkin.mixColor(from.slot, to.slot, t),
                MenuSkin.mixColor(from.slotEdgeLight, to.slotEdgeLight, t),
                MenuSkin.mixColor(from.slotEdgeDark, to.slotEdgeDark, t),
                MenuSkin.mixColor(from.tabIdle, to.tabIdle, t),
                MenuSkin.mixColor(from.tabIdleEdge, to.tabIdleEdge, t),
                MenuSkin.mixColor(from.tabSelected, to.tabSelected, t),
                MenuSkin.mixColor(from.tabSelectedEdge, to.tabSelectedEdge, t),
                MenuSkin.mixColor(from.tabLocked, to.tabLocked, t),
                MenuSkin.mixColor(from.tabLockedEdge, to.tabLockedEdge, t),
                MenuSkin.mixColor(from.scrollTrack, to.scrollTrack, t),
                MenuSkin.mixColor(from.scrollThumb, to.scrollThumb, t));
    }
}
