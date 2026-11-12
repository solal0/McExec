package dev.mcexec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable, safely published render filter read by terrain compilation workers. */
public final class XrayRender {
    public enum Mode { OFF, ONLY, HIDE }
    private record Filter(Mode mode, Set<String> ids) {}
    private static volatile Filter filter = new Filter(Mode.OFF, Set.of());

    private XrayRender() {}
    public static Mode mode() { return filter.mode(); }
    public static boolean enabled() { return mode() != Mode.OFF; }

    public static void showOnly(String... ids) {
        set(Mode.ONLY, ids);
    }
    public static void hide(String... ids) {
        set(Mode.HIDE, ids);
    }
    public static void off() {
        filter = new Filter(Mode.OFF, Set.of());
        refresh();
    }
    private static void set(Mode mode, String... ids) {
        Set<String> blockIds = Arrays.stream(ids)
                .map(XrayRender::normalize)
                .collect(Collectors.toUnmodifiableSet());
        filter = new Filter(mode, blockIds);
        refresh();
    }
    private static String normalize(String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.contains(":") ? normalized : "minecraft:" + normalized;
    }
    public static boolean shouldHide(BlockState state) {
        Filter snapshot = filter;
        if (snapshot.mode() == Mode.OFF || state.isAir()) return false;
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;
        boolean inList = snapshot.ids().contains(id.toString());
        return snapshot.mode() == Mode.ONLY ? !inList : inList;
    }
    public static void refresh() {
        // Vanilla 26.3 updates render meshes from LevelExtractor, not LevelRenderer.
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level != null) mc.levelExtractor.allChanged();
        });
    }
}
