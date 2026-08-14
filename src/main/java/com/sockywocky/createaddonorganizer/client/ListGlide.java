package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.components.AbstractSelectionList;

public final class ListGlide {

    private final SmoothScroll scroll = new SmoothScroll();
    private boolean init;
    private double displayed;

    public void beforeRender(AbstractSelectionList<?> list) {
        double current = list.getScrollAmount();
        if (!init || Math.abs(current - displayed) > 0.01d) {
            init = true;
            scroll.jumpTo(current);
        }
        displayed = scroll.advance();
        list.setScrollAmount(displayed);
    }

    public double target() {
        return scroll.target();
    }

    public void beginScroll(AbstractSelectionList<?> list) {
        list.setScrollAmount(scroll.target());
    }

    public void endScroll(AbstractSelectionList<?> list) {
        scroll.setTarget(list.getScrollAmount());
        list.setScrollAmount(displayed);
    }
}
