package dev.mcexec.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Draggable MC Exec workspace. */
public final class ConsoleScreen extends Screen {
    private enum Tab { EXECUTOR, CONSOLE, EXAMPLES, SETTINGS }
    private static String draft;
    private static String fileName = "editor";
    private static int savedX = -1, savedY = -1, savedW = 440, savedH = 260;
    private int wx, wy, ww, wh, scroll;
    private boolean dragging;
    private double dragX, dragY;
    private Tab tab = Tab.EXECUTOR;
    private MultiLineEditBox editor;
    private EditBox nameBox;
    private final List<AbstractWidget> floatingWidgets = new ArrayList<>();
    private int pickingKey = -1;
    private int examplePage;
    private int categoryIndex;
    private static final int SIDE = 76, HEADER = 23;

    public ConsoleScreen() {
        super(Component.literal("MC Exec"));
        if (draft == null) {
            try { draft = ScriptStorage.load(); }
            catch (IOException e) { draft = "api.local('Hello!')"; MCExecClient.runtime().error("Load failed", e); }
        }
    }

    @Override
    protected void init() {
        ww = Math.min(savedW, Math.max(240, width - 8));
        wh = Math.min(savedH, Math.max(170, height - 8));
        wx = savedX < 0 ? (width - ww) / 2 : Math.max(0, Math.min(savedX, width - ww));
        wy = savedY < 0 ? (height - wh) / 2 : Math.max(0, Math.min(savedY, height - wh));
        layoutWidgets();
    }

    private void layoutWidgets() {
        for (AbstractWidget w : floatingWidgets) removeWidget(w);
        floatingWidgets.clear();
        editor = null;
        nameBox = null;
        button("Executor", wx + 6, wy + 34, 64, 17, tab == Tab.EXECUTOR, () -> switchTab(Tab.EXECUTOR));
        button("Console", wx + 6, wy + 55, 64, 17, tab == Tab.CONSOLE, () -> switchTab(Tab.CONSOLE));
        button("Examples", wx + 6, wy + 76, 64, 17, tab == Tab.EXAMPLES, () -> switchTab(Tab.EXAMPLES));
        button("Settings", wx + 6, wy + 97, 64, 17, tab == Tab.SETTINGS, () -> switchTab(Tab.SETTINGS));

        int cx = wx + SIDE + 8;
        int cw = ww - SIDE - 16;
        if (tab == Tab.EXECUTOR) {
            int controlsY = wy + wh - 25;
            int nameY = controlsY - 22;
            int editorH = Math.max(35, nameY - (wy + HEADER + 7) - 5);
            editor = MultiLineEditBox.builder()
                    .setX(cx).setY(wy + HEADER + 7)
                    .setPlaceholder(Component.literal("// Groovy / Java"))
                    .setTextColor(UISettings.color(5))
                    .setShowBackground(false)
                    .build(font, cw, editorH, Component.literal("Code"));
            ((CodeEditorMarker) editor).mcexec$setSyntax(true);
            editor.setCharacterLimit(100000);
            editor.setLineLimit(2000);
            editor.setValue(draft);
            editor.setValueListener(value -> draft = value);
            attach(editor);
            int fileButtonW = 37;
            button("Save", cx, nameY, fileButtonW, 16, false, this::persist);
            button("Load", cx + fileButtonW + 3, nameY, fileButtonW, 16, false, this::reload);
            nameBox = new EditBox(font, cx + 2 * (fileButtonW + 3), nameY,
                    Math.max(45, cw - 2 * (fileButtonW + 3)), 16, Component.literal("Filename"));
            nameBox.setMaxLength(64);
            nameBox.setValue(fileName);
            nameBox.setResponder(value -> fileName = value);
            attach(nameBox);
            int bx = cx;
            int bw = Math.max(35, (cw - 6) / 3);
            button("Run", bx, controlsY, bw, 17, true, this::runScript);
            bx += bw + 3;
            button("Stop All", bx, controlsY, bw, 17, false, () -> MCExecClient.api().stopAll());
            bx += bw + 3;
            button("Clear", bx, controlsY, bw, 17, false, () -> {
                draft = "";
                editor.setValue("");
            });
        } else if (tab == Tab.CONSOLE) {
            button("Clear logs", cx, wy + wh - 25, 64, 17, false, () -> { MCExecClient.runtime().clearLog(); scroll = 0; });
            button("Copy all", cx + 68, wy + wh - 25, 60, 17, false, () -> {
                minecraft.keyboardHandler.setClipboard(MCExecClient.runtime().allLogs());
                MCExecClient.runtime().log("Logs copied to clipboard");
            });
        } else if (tab == Tab.EXAMPLES) {
            List<ScriptStorage.Example> examples = filteredExamples();
            int cardH = 30;
            int count = Math.max(1, (wh - HEADER - 65) / (cardH + 4));
            examplePage = Math.min(examplePage, Math.max(0, (examples.size() - 1) / count));
            int start = examplePage * count;
            button("<", cx, wy + HEADER + 5, 20, 16, false, () -> changeCategory(-1));
            button(">", wx + ww - 28, wy + HEADER + 5, 20, 16, false, () -> changeCategory(1));
            for (int i = start; i < Math.min(examples.size(), start + count); i++) {
                ScriptStorage.Example example = examples.get(i);
                int y = wy + HEADER + 27 + (i - start) * (cardH + 4);
                button("Open", wx + ww - 56, y + 7, 43, 16, true, () -> openExample(example.id()));
            }
            button("<", cx, wy + wh - 25, 22, 17, false, () -> { examplePage = Math.max(0, examplePage - 1); layoutWidgets(); });
            button(">", cx + 26, wy + wh - 25, 22, 17, false, () -> { examplePage++; layoutWidgets(); });
        } else if (tab == Tab.SETTINGS) {
            int x = cx, y = wy + HEADER + 9;
            button("Theme: " + UISettings.THEMES[UISettings.theme], x, y, cw, 17, false, () -> {
                UISettings.theme = (UISettings.theme + 1) % UISettings.THEMES.length;
                UISettings.save(); layoutWidgets();
            });
            y += 23;
            button("UI Scale: " + (UISettings.scale == 0 ? "Minecraft" : UISettings.scale + "x"),
                    x, y, cw, 17, false, () -> {
                        UISettings.scale = (UISettings.scale + 1) % 5;
                        UISettings.save();
                        UISettings.applyScale();
                        layoutWidgets();
                    });
            y += 23;
            button("Syntax: " + UISettings.SYNTAX[UISettings.syntax], x, y, cw, 17, false, () -> {
                UISettings.syntax = (UISettings.syntax + 1) % UISettings.SYNTAX.length;
                UISettings.save(); layoutWidgets();
            });
            y += 23;
            button("Toggle: " + (pickingKey == 0 ? "Press a key..." : UISettings.keyName(UISettings.toggle)),
                    x, y, cw - 24, 17, false, () -> { pickingKey = 0; layoutWidgets(); });
            if (UISettings.toggle >= 0) button("x", x + cw - 20, y, 20, 17, false, () -> {
                UISettings.toggle = -1; MCExecClient.refreshKeys(); UISettings.save(); layoutWidgets();
            });
            y += 23;
            button("Stop All: " + (pickingKey == 1 ? "Press a key..." : UISettings.keyName(UISettings.stop)),
                    x, y, cw - 24, 17, false, () -> { pickingKey = 1; layoutWidgets(); });
            if (UISettings.stop >= 0) button("x", x + cw - 20, y, 20, 17, false, () -> {
                UISettings.stop = -1; MCExecClient.refreshKeys(); UISettings.save(); layoutWidgets();
            });
            y += 23;
            button("Dim background: " + (UISettings.dim ? "On" : "Off"),
                    x, y, cw, 17, false, () -> {
                        UISettings.dim = !UISettings.dim; UISettings.save(); layoutWidgets();
                    });
        }
    }


    @Override
    public boolean keyPressed(KeyEvent event) {
        if (pickingKey >= 0) {
            int code = event.key();
            if (pickingKey == 0) UISettings.toggle = code;
            else UISettings.stop = code;
            pickingKey = -1;
            MCExecClient.refreshKeys();
            UISettings.save();
            layoutWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    private List<ScriptStorage.Example> filteredExamples() {
        String selected = ScriptStorage.categories().get(categoryIndex);
        return ScriptStorage.examples().stream()
                .filter(e -> selected.equals("All") || selected.equals(e.category())).toList();
    }
    private void changeCategory(int direction) {
        int n = ScriptStorage.categories().size();
        categoryIndex = (categoryIndex + direction + n) % n;
        examplePage = 0;
        layoutWidgets();
    }
    private void runScript() {
        try {
            String id = ScriptStorage.safeName(fileName);
            ScriptStorage.saveNamed(id, draft);
            MCExecClient.runtime().execute(draft, id);
        } catch (IOException ex) { MCExecClient.runtime().error("Could not save/run script", ex); }
    }

    private void attach(AbstractWidget w) { floatingWidgets.add(w); addRenderableWidget(w); }
    private void button(String label, int x, int y, int w, int h, boolean active, Runnable action) {
        attach(new FlatButton(label, x, y, w, h, active, action));
    }
    private void switchTab(Tab next) { tab = next; scroll = 0; pickingKey = -1; layoutWidgets(); }
    private void persist() {
        try {
            ScriptStorage.saveNamed(fileName, draft);
            MCExecClient.runtime().log("Saved " + ScriptStorage.safeName(fileName) + ".groovy");
        } catch (IOException e) { MCExecClient.runtime().error("Save failed", e); }
    }
    private void reload() {
        try {
            draft = ScriptStorage.loadNamed(fileName);
            if (editor != null) editor.setValue(draft);
            MCExecClient.runtime().log("Reloaded " + ScriptStorage.safeName(fileName));
        } catch (IOException e) { MCExecClient.runtime().error("Reload failed", e); }
    }
    private void openExample(String name) {
        try {
            draft = ScriptStorage.example(name);
            fileName = name;
            switchTab(Tab.EXECUTOR);
        } catch (IOException e) { MCExecClient.runtime().error("Example failed", e); }
    }

    private List<ScriptRuntime.LogEntry> wrappedLogs(int availableWidth) {
        List<ScriptRuntime.LogEntry> wrapped = new ArrayList<>();
        for (ScriptRuntime.LogEntry entry : MCExecClient.runtime().snapshotColored()) {
            String remaining = entry.text();
            if (remaining.isEmpty()) {
                wrapped.add(entry);
                continue;
            }
            while (!remaining.isEmpty()) {
                String visible = font.plainSubstrByWidth(remaining, availableWidth);
                if (visible.isEmpty()) visible = remaining.substring(0, 1);
                wrapped.add(new ScriptRuntime.LogEntry(visible, entry.level()));
                remaining = remaining.substring(visible.length());
            }
        }
        return wrapped;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = wx + SIDE;
        int right = wx + ww;
        if (tab == Tab.CONSOLE && mouseX >= left && mouseX <= right
                && mouseY >= wy + HEADER && mouseY <= wy + wh - 28) {
            int visibleRows = Math.max(1, (wh - HEADER - 42) / 11);
            int maxScroll = Math.max(0, wrappedLogs(Math.max(25, ww - SIDE - 35)).size() - visibleRows);
            scroll = Math.max(0, Math.min(maxScroll, scroll + (int) Math.round(scrollY * 4)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && event.x() >= wx && event.x() < wx + ww
                && event.y() >= wy && event.y() < wy + HEADER) {
            dragging = true;
            dragX = event.x() - wx;
            dragY = event.y() - wy;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            int nx = Math.max(0, Math.min(width - ww, (int)(event.x() - dragX)));
            int ny = Math.max(0, Math.min(height - wh, (int)(event.y() - dragY)));
            int shiftX = nx - wx, shiftY = ny - wy;
            wx = nx; wy = ny;
            savedX = wx; savedY = wy;
            for (AbstractWidget w : floatingWidgets) { w.setX(w.getX() + shiftX); w.setY(w.getY() + shiftY); }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) { dragging = false; return true; }
        return super.mouseReleased(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        if (UISettings.dim) g.fill(0, 0, width, height, 0x44050811);
        g.fill(wx - 2, wy - 2, wx + ww + 2, wy + wh + 2, UISettings.color(0));
        g.fill(wx, wy, wx + ww, wy + wh, UISettings.color(0));
        g.fill(wx, wy, wx + ww, wy + HEADER, UISettings.color(1));
        g.fill(wx, wy + HEADER, wx + SIDE, wy + wh, UISettings.color(2));
        g.fill(wx + SIDE, wy + HEADER, wx + SIDE + 1, wy + wh, UISettings.color(3));
        g.text(font, "MC Exec", wx + 9, wy + 7, UISettings.color(5), false);
        int cx = wx + SIDE + 8;
        int cw = ww - SIDE - 16;
        if (tab == Tab.EXECUTOR) {
            g.fill(cx - 2, wy + HEADER + 5, wx + ww - 7, wy + wh - 49, UISettings.color(2));
        } else if (tab == Tab.CONSOLE) {
            int top = wy + HEADER + 8, bottom = wy + wh - 34;
            int textWidth = Math.max(25, cw - 19);
            int visibleRows = Math.max(1, (bottom - top) / 11);
            List<ScriptRuntime.LogEntry> lines = wrappedLogs(textWidth);
            int maxScroll = Math.max(0, lines.size() - visibleRows);
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            int firstRow = Math.max(0, lines.size() - visibleRows - scroll);
            g.fill(cx - 2, top - 4, wx + ww - 8, wy + wh - 30, UISettings.color(2));
            g.enableScissor(cx, top, wx + ww - 18, bottom);
            for (int i = firstRow; i < Math.min(lines.size(), firstRow + visibleRows); i++) {
                ScriptRuntime.LogEntry entry = lines.get(i);
                int color = switch (entry.level()) {
                    case ERROR -> 0xFFFF657B;
                    case WARNING -> 0xFFFFC16A;
                    case SUCCESS -> 0xFF6DE3A2;
                    case EXECUTION -> 0xFF73C7FF;
                    case SCRIPT -> 0xFFD8BEFF;
                    case SYSTEM -> UISettings.color(6);
                };
                g.text(font, entry.text(), cx + 4, top + (i - firstRow) * 11, color, false);
            }
            g.disableScissor();
            int trackX = wx + ww - 18;
            int trackY = top;
            int trackH = Math.max(4, bottom - top);
            g.fill(trackX, trackY, trackX + 3, trackY + trackH, UISettings.color(3));
            if (lines.size() > visibleRows) {
                int thumbH = Math.max(12, visibleRows * trackH / lines.size());
                int thumbY = trackY + (maxScroll - scroll) * (trackH - thumbH) / maxScroll;
                g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, UISettings.color(4));
            }
        } else if (tab == Tab.EXAMPLES) {
            List<ScriptStorage.Example> examples = filteredExamples();
            String category = ScriptStorage.categories().get(categoryIndex);
            g.text(font, "Category: " + category, cx + 24, wy + HEADER + 9, 0xFFB4C9F0, false);
            int count = Math.max(1, (wh - HEADER - 65) / 34);
            int start = examplePage * count;
            int end = Math.min(examples.size(), start + count);
            for (int i = start; i < end; i++) {
                ScriptStorage.Example example = examples.get(i);
                int y = wy + HEADER + 27 + (i - start) * 34;
                g.fill(cx, y, wx + ww - 8, y + 30, UISettings.color(7));
                int maxWidth = Math.max(40, ww - SIDE - 83);
                g.text(font, font.plainSubstrByWidth(example.title(), maxWidth),
                        cx + 6, y + 4, 0xFFE4EEFF, false);
                g.text(font, font.plainSubstrByWidth(example.description(), maxWidth),
                        cx + 6, y + 17, 0xFF98A9C6, false);
            }
            int maxPage = Math.max(1, (examples.size() + count - 1) / count);
            g.text(font, (examplePage + 1) + "/" + maxPage,
                    cx + 55, wy + wh - 21, UISettings.color(6), false);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void onClose() { minecraft.gui.setScreen(null); }
    @Override public void removed() { UISettings.restoreScale(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
}
