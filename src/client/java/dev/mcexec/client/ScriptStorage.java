package dev.mcexec.client;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Script storage and a versioned, bundled, read-only collection of teaching examples. */
public final class ScriptStorage {
    public record Example(String category, String id, String title, String description) {}

    private static final String DEFAULT_SCRIPT = """
            // Open Examples for interactive scripts and live HUD demos.
            if (player != null) api.local('McExec ready. Open Examples to start.')
            """;

    private static final List<Example> EXAMPLES = List.of(
            new Example("Start Here", "01_world_snapshot", "World Snapshot", "One run: player, blocks, inventory, entities"),
            new Example("Start Here", "02_vitals_hud", "Vitals Bars", "Minimal health and hunger bars at bottom left"),
            new Example("Live HUD", "03_navigation_overlay", "Navigation Overlay", "Live compass, coordinates and movement speed"),
            new Example("Live HUD", "04_inventory_overlay", "Hotbar Counts", "Stack counts aligned with Minecraft's hotbar"),
            new Example("Live HUD", "05_entity_radar", "Entity Radar", "North-up minimap with adjustable radius and colored dots"),
            new Example("Interaction", "06_keyboard_panel", "Keyboard Inputs", "Visual keycaps: press, hold and release"),
            new Example("Interaction", "07_movement_trainer", "Movement Trainer", "J walks for one second; K cancels"),
            new Example("Interaction", "08_using_draw", "Using Draw", "Moving shapes, rotating lines and animated colors"),
            new Example("Advanced", "09_xray_switch", "X-Ray Switch", "Chat notification and selective ore rendering"),
            new Example("Advanced", "10_colored_glow_esp", "Colored Glow ESP", "Single-line legend and entity outlines"),
            new Example("Advanced", "11_entity_field_probe", "Live Field Watch", "Inspect and compare an entity's internal Java fields")
    );

    private ScriptStorage() {}
    public static List<Example> examples() { return EXAMPLES; }
    public static List<String> categories() {
        return List.of("All", "Start Here", "Live HUD", "Interaction", "Advanced");
    }
    public static Path scriptsDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcexec").resolve("scripts");
    }
    public static Path templateDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("mcexec").resolve("examples");
    }
    public static String safeName(String name) throws IOException {
        String normalized = name == null ? "" : name.trim();
        if (normalized.endsWith(".groovy")) normalized = normalized.substring(0, normalized.length() - 7);
        if (!normalized.matches("[a-zA-Z0-9_-]{1,64}"))
            throw new IOException("Filename may contain only letters, numbers, underscores and hyphens");
        return normalized;
    }
    public static String loadNamed(String name) throws IOException {
        Path file = scriptsDir().resolve(safeName(name) + ".groovy");
        return Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : DEFAULT_SCRIPT;
    }
    public static void saveNamed(String name, String text) throws IOException {
        Files.createDirectories(scriptsDir());
        Files.writeString(scriptsDir().resolve(safeName(name) + ".groovy"), text, StandardCharsets.UTF_8);
    }
    public static String load() throws IOException { return loadNamed("editor"); }
    public static void save(String text) throws IOException { saveNamed("editor", text); }
    public static void seedExamples() throws IOException {
        Files.createDirectories(templateDir());
        for (Example example : EXAMPLES) {
            Path path = templateDir().resolve(example.id() + ".groovy");
            try (InputStream source = ScriptStorage.class.getResourceAsStream("/mcexec/examples/" + example.id() + ".groovy")) {
                if (source == null) throw new IOException("Bundled example missing: " + example.id());
                // Templates are refreshed on upgrade; user scripts remain separate in scripts/.
                Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    public static String example(String id) throws IOException {
        if (EXAMPLES.stream().noneMatch(e -> e.id().equals(id))) throw new IOException("Unknown example: " + id);
        try (InputStream source = ScriptStorage.class.getResourceAsStream("/mcexec/examples/" + id + ".groovy")) {
            if (source == null) throw new IOException("Bundled example missing: " + id);
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
