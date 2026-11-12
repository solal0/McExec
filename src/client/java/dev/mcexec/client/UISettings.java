package dev.mcexec.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import java.nio.file.*;
import java.util.Properties;

final class UISettings {
    static final String[] THEMES = {"Deep Dark", "Dark", "Light", "Ocean", "Forest", "Rose"};
    static final String[] SYNTAX = {"High Contrast", "Neon", "Pastel", "Monochrome"};
    static int theme = 0, syntax = 0, scale = 0;
    static int toggle = InputConstants.KEY_RSHIFT, stop = InputConstants.KEY_F10;
    static boolean dim = true;
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mcexec-ui.properties");
    private static int oldScale = -1;
    static void load() {
        Properties p = new Properties();
        try (var in = Files.newInputStream(FILE)) { p.load(in); } catch (Exception ignored) {}
        theme = bounded(p.getProperty("theme"), 0, THEMES.length - 1, 0);
        syntax = bounded(p.getProperty("syntax"), 0, SYNTAX.length - 1, 0);
        scale = bounded(p.getProperty("scale"), 0, 4, 0);
        toggle = bounded(p.getProperty("toggle"), -1, 1024, toggle);
        stop = bounded(p.getProperty("stop"), -1, 1024, stop);
        dim = Boolean.parseBoolean(p.getProperty("dim", "true"));
    }
    private static int bounded(String v, int min, int max, int fallback) {
        try { int n = Integer.parseInt(v); return n >= min && n <= max ? n : fallback; }
        catch (Exception ex) { return fallback; }
    }
    static void save() {
        Properties p = new Properties();
        p.setProperty("theme", String.valueOf(theme));
        p.setProperty("syntax", String.valueOf(syntax));
        p.setProperty("scale", String.valueOf(scale));
        p.setProperty("toggle", String.valueOf(toggle));
        p.setProperty("stop", String.valueOf(stop));
        p.setProperty("dim", String.valueOf(dim));
        try { Files.createDirectories(FILE.getParent()); try (var out = Files.newOutputStream(FILE)) { p.store(out, "McExec UI"); } }
        catch (Exception e) { MCExecClient.runtime().error("Settings save failed", e); }
    }
    static int color(int part) {
        int[][] palettes = {
            {0xFF0B0B0D,0xFF141416,0xFF101012,0xFF252529,0xFF37373D,0xFFF1F1F3,0xFFACACB3,0xFF1B1B20},
            {0xFF12151C,0xFF202630,0xFF191F28,0xFF303A48,0xFF48576C,0xFFF2F4F8,0xFFACB8CB,0xFF26313E},
            {0xFFE9EBF0,0xFFF9FAFC,0xFFF0F1F5,0xFFCDD2DB,0xFFB2BBC9,0xFF151922,0xFF515C6B,0xFFE0E4EB},
            {0xFF131B2A,0xFF202D43,0xFF172236,0xFF354259,0xFF386BC5,0xFFE6EEFF,0xFF98A9C6,0xFF222E42},
            {0xFF111A15,0xFF1C3024,0xFF17271C,0xFF355442,0xFF4C8D66,0xFFE9F8EA,0xFFA6C8AE,0xFF23392A},
            {0xFF20141D,0xFF362230,0xFF2A1C28,0xFF624358,0xFFA45D86,0xFFFFECF6,0xFFD4A8C2,0xFF3E2938}
        };
        return palettes[theme][part];
    }
    static String keyName(int code) {
        if (code < 0) return "None";
        if (code == InputConstants.KEY_RSHIFT) return "Right Shift";
        if (code == InputConstants.KEY_F10) return "F10";
        return "Key " + code;
    }
    static void remap(KeyMapping mapping, int code) {
        mapping.setKey(code < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYBOARD.getOrCreate(code));
        KeyMapping.resetMapping();
    }
    static void applyScale() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null || mc.getWindow() == null) return;
        if (oldScale < 0) oldScale = mc.options.guiScale().get();
        mc.options.guiScale().set(scale == 0 ? oldScale : scale);
        mc.resizeGui();
    }
    static void restoreScale() {
        if (oldScale < 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && mc.getWindow() != null) {
            mc.options.guiScale().set(oldScale);
            oldScale = -1;
            mc.resizeGui();
        }
    }
}
