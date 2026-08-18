package com.sockywocky.createaddonorganizer.client;

import java.util.List;

import com.sockywocky.createaddonorganizer.TabLayout;

public final class ItemGroupColors {

    private static final int[] PALETTE = {
            0x55C8E0,
            0xF2A65A,
            0x9BD46A,
            0xC98BE0,
            0xE0655F,
            0x6FA8F5,
            0xE7D45C,
            0x5FD9B0,
            0xEE87B4,
            0x8FA0B8,
    };

    private ItemGroupColors() {}

    public static int rgbOf(TabLayout layout, String groupId) {
        return PALETTE[slotOf(layout, groupId)];
    }

    public static int tint(TabLayout layout, String groupId) {
        return withAlpha(rgbOf(layout, groupId), 0x38);
    }

    public static int edge(TabLayout layout, String groupId) {
        return withAlpha(rgbOf(layout, groupId), 0xB0);
    }

    public static int slotEdge(TabLayout layout, String groupId) {
        return withAlpha(rgbOf(layout, groupId), 0xC0);
    }

    public static int iconEdge(TabLayout layout, String groupId) {
        return withAlpha(lighten(rgbOf(layout, groupId)), 0xFF);
    }

    private static int slotOf(TabLayout layout, String groupId) {
        if (groupId == null) {
            return 0;
        }
        if (layout != null) {
            List<TabLayout.ItemGroup> groups = layout.safeItemGroups();
            for (int i = 0; i < groups.size(); i++) {
                if (groupId.equals(groups.get(i).id())) {
                    return i % PALETTE.length;
                }
            }
        }
        return Math.floorMod(groupId.hashCode(), PALETTE.length);
    }

    private static int lighten(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (blend(r) << 16) | (blend(g) << 8) | blend(b);
    }

    private static int blend(int channel) {
        return Math.min(0xFF, channel + (0xFF - channel) * 6 / 10);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }
}
