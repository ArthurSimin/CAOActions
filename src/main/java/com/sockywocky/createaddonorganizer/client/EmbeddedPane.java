package com.sockywocky.createaddonorganizer.client;

public interface EmbeddedPane {

    void embedInto(int x, int y, int width, int height, Runnable onDone);

    default void onEmbeddedChanged(Runnable listener) {
    }
}
