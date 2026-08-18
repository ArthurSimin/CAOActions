package com.sockywocky.createaddonorganizer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GuardLog {

    private static final Map<String, AtomicInteger> SEEN = new ConcurrentHashMap<>();
    private static final int REPEAT_LIMIT = 3;

    private GuardLog() {}

    public static void report(String message, Throwable t) {
        String key = signatureOf(t);
        int count = SEEN.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        if (count == 1) {
            createaddonorganizer.LOGGER.error("[CAO] " + message, t);
            return;
        }
        if (count <= REPEAT_LIMIT) {
            createaddonorganizer.LOGGER.warn("[CAO] {} -- same fault as before ({}); stack trace omitted",
                    message, describe(t));
            return;
        }
        if (count == REPEAT_LIMIT + 1) {
            createaddonorganizer.LOGGER.warn("[CAO] {} -- this fault ({}) keeps repeating; further "
                    + "occurrences will not be logged", message, describe(t));
        }
    }

    public static void reset() {
        SEEN.clear();
    }

    private static String describe(Throwable t) {
        String msg = t.getMessage();
        return msg == null ? t.getClass().getName() : t.getClass().getSimpleName() + ": " + msg;
    }

    private static String signatureOf(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        StringBuilder key = new StringBuilder(root.getClass().getName());
        StackTraceElement[] frames = root.getStackTrace();
        for (int i = 0; i < frames.length && i < 4; i++) {
            key.append('|').append(frames[i].getClassName()).append('.').append(frames[i].getMethodName());
        }
        return key.toString();
    }
}
