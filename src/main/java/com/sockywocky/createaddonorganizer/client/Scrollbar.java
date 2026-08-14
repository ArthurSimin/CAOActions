package com.sockywocky.createaddonorganizer.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class Scrollbar {

    public static final int WIDTH = 4;

    private static final int MIN_THUMB = 16;
    private static final int MIN_GRAB = 6;

    private final SmoothScroll scroll = new SmoothScroll();

    private int trackX;
    private int top;
    private int trackH;
    private int width = WIDTH;
    private int step = 24;
    private double contentHeight;
    private boolean dragging;
    private double grabOffset;

    public Scrollbar width(int value) {
        this.width = value;
        return this;
    }

    public Scrollbar step(int value) {
        this.step = value;
        return this;
    }

    public void bounds(int trackX, int top, int trackH) {
        this.trackX = trackX;
        this.top = top;
        this.trackH = trackH;
        clamp();
    }

    public void content(double height) {
        this.contentHeight = height;
        clamp();
    }

    public double max() {
        return Math.max(0, contentHeight - trackH);
    }

    public boolean scrollable() {
        return max() > 0;
    }

    public double target() {
        return scroll.target();
    }

    public void setTarget(double value) {
        scroll.setTarget(Mth.clamp(value, 0, max()));
    }

    public void jumpTo(double value) {
        scroll.jumpTo(Mth.clamp(value, 0, max()));
    }

    public void reset() {
        scroll.jumpTo(0);
    }

    public int offset() {
        return (int) Math.round(scroll.advance());
    }

    public int displayed() {
        return (int) Math.round(scroll.displayed());
    }

    private void clamp() {
        scroll.setTarget(Mth.clamp(scroll.target(), 0, max()));
    }

    private int thumbHeight() {
        if (contentHeight <= 0) {
            return trackH;
        }
        return Mth.clamp((int) ((double) trackH * trackH / contentHeight), MIN_THUMB, trackH);
    }

    private int thumbTop(double offset) {
        double range = max();
        if (range <= 0) {
            return top;
        }
        return top + (int) ((trackH - thumbHeight()) * (offset / range));
    }

    public boolean overTrack(double mouseX, double mouseY) {
        int grab = Math.max(width, MIN_GRAB);
        return scrollable() && mouseX >= trackX + width - grab && mouseX < trackX + width
                && mouseY >= top && mouseY < top + trackH;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!overTrack(mouseX, mouseY)) {
            return false;
        }
        int thumbH = thumbHeight();
        int thumbY = thumbTop(scroll.displayed());
        if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
            grabOffset = mouseY - thumbY;
        } else {
            grabOffset = thumbH / 2.0;
            dragTo(mouseY);
        }
        dragging = true;
        return true;
    }

    public boolean mouseDragged(double mouseY) {
        if (!dragging) {
            return false;
        }
        dragTo(mouseY);
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    private void dragTo(double mouseY) {
        int usable = Math.max(1, trackH - thumbHeight());
        double fraction = (mouseY - top - grabOffset) / usable;
        double before = scroll.target();
        double value = Mth.clamp(fraction * max(), 0, max());
        scroll.jumpTo(value);
        if ((int) before != (int) value) {
            Sfx.scroll();
        }
    }

    public boolean wheel(double mouseX, double mouseY, double amount, int areaX, int areaW) {
        if (mouseX < areaX || mouseX >= areaX + areaW || mouseY < top || mouseY >= top + trackH) {
            return false;
        }
        return wheel(amount);
    }

    public boolean wheel(double amount) {
        double before = scroll.target();
        setTarget(before - amount * step);
        Sfx.scrolled(before, scroll.target(), scrollable());
        return true;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY) {
        if (!scrollable()) {
            return;
        }
        boolean lit = dragging || overTrack(mouseX, mouseY);
        paint(g, trackX, top, width, trackH, thumbTop(scroll.displayed()), thumbHeight(), lit);
    }

    public static void paint(GuiGraphics g, int x, int y, int width, int height, int thumbY, int thumbH,
            boolean lit) {
        if (GlassSkin.vanilla()) {
            g.fill(x, y, x + width, y + height, 0xFF000000);
            g.fill(x, thumbY, x + width, thumbY + thumbH, 0xFF808080);
            g.fill(x, thumbY, x + width - 1, thumbY + thumbH - 1, lit ? 0xFFFFFFFF : 0xFFC0C0C0);
            return;
        }
        MenuSkin.scrollTrack(g, x, y, width, height, 1f);
        MenuSkin.scrollThumb(g, x, thumbY, width, thumbH, 1f);
        if (lit) {
            g.fill(x, thumbY, x + width, thumbY + thumbH, 0x30FFFFFF);
        }
    }
}
