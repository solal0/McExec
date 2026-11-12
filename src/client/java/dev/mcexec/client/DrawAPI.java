package dev.mcexec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Managed 2D HUD drawing, extracted through Fabric's 26.3 HUD pipeline. */
public final class DrawAPI {
    private final ScriptRuntime runtime;
    private final Map<ScriptSession, Map<String, Consumer<Canvas>>> primitives = new LinkedHashMap<>();

    DrawAPI(ScriptRuntime runtime) { this.runtime = runtime; }

    private void put(String id, Consumer<Canvas> drawing) {
        ScriptSession session = runtime.requireSession();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Drawing id must not be blank");
        if (!primitives.containsKey(session)) {
            primitives.put(session, new LinkedHashMap<>());
            session.onStop(() -> primitives.remove(session));
        }
        primitives.get(session).put(id, drawing);
    }
    public void text(String id, int x, int y, String value, int argb) {
        put(id, c -> c.text(x, y, value, argb));
    }
    public void rect(String id, int x, int y, int width, int height, int argb) {
        put(id, c -> c.rect(x, y, width, height, argb));
    }
    public void outline(String id, int x, int y, int width, int height, int argb) {
        put(id, c -> c.outline(x, y, width, height, argb));
    }
    public void centeredText(String id, int centerX, int y, String value, int argb) {
        put(id, c -> c.centeredText(centerX, y, value, argb));
    }
    public void line(String id, int x1, int y1, int x2, int y2, int argb) {
        put(id, c -> c.line(x1, y1, x2, y2, argb));
    }
    public void remove(String id) {
        ScriptSession owner = runtime.requireSession();
        Map<String, Consumer<Canvas>> list = primitives.get(owner);
        if (list != null) list.remove(id);
    }
    public void clear() {
        ScriptSession owner = runtime.requireSession();
        Map<String, Consumer<Canvas>> list = primitives.get(owner);
        if (list != null) list.clear();
    }
    void clearAll() { primitives.clear(); }

    void extract(GuiGraphicsExtractor g) {
        if (Minecraft.getInstance().player == null) return;
        Canvas canvas = new Canvas(g);
        for (Map< String, Consumer<Canvas>> own : new ArrayList<>(primitives.values())) {
            for (Consumer<Canvas> operation : new ArrayList<>(own.values())) {
                try { operation.accept(canvas); }
                catch (Throwable t) { runtime.error("Drawing failed", t); }
            }
        }
    }

    /** Per-frame drawing handle. Valid only during the HUD extraction callback. */
    public static final class Canvas {
        private final GuiGraphicsExtractor g;
        Canvas(GuiGraphicsExtractor graphics) { g = graphics; }
        public int width() { return g.guiWidth(); }
        public int height() { return g.guiHeight(); }
        public GuiGraphicsExtractor graphics() { return g; }
        public void text(int x, int y, String label, int argb) {
            g.text(Minecraft.getInstance().font, label, x, y, argb, false);
        }
        public void centeredText(int centerX, int y, String label, int argb) {
            text(centerX - Minecraft.getInstance().font.width(label) / 2, y, label, argb);
        }
        public void outline(int x, int y, int width, int height, int argb) {
            if (width <= 0 || height <= 0) return;
            rect(x, y, width, 1, argb);
            rect(x, y + height - 1, width, 1, argb);
            rect(x, y, 1, height, argb);
            rect(x + width - 1, y, 1, height, argb);
        }
        public void rect(int x, int y, int width, int height, int argb) {
            if (width > 0 && height > 0) g.fill(x, y, x + width, y + height, argb);
        }
        public void line(int x1, int y1, int x2, int y2, int argb) {
            int length = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
            if (length > 4096) throw new IllegalArgumentException("HUD line exceeds 4096 pixels");
            if (length == 0) { rect(x1, y1, 1, 1, argb); return; }
            for (int i = 0; i <= length; i++) {
                int x = x1 + (int)Math.round((double)(x2 - x1) * i / length);
                int y = y1 + (int)Math.round((double)(y2 - y1) * i / length);
                rect(x, y, 1, 1, argb);
            }
        }
    }
}
