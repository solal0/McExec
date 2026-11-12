package dev.mcexec.client;

import com.mojang.blaze3d.platform.InputConstants;
import groovy.lang.Closure;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** High-level client API. All managed registrations belong to a named script session. */
public final class ScriptAPI {
    private final Minecraft mc = Minecraft.getInstance();
    private final List<TickHandler> tickHandlers = new CopyOnWriteArrayList<>();
    private final List<KeyHandler> keyHandlers = new CopyOnWriteArrayList<>();
    private final List<TickHandler> hudHandlers = new CopyOnWriteArrayList<>();
    private final List<TickHandler> worldHandlers = new CopyOnWriteArrayList<>();
    private final Map<ScriptSession, Set<String>> held = new LinkedHashMap<>();
    private final Set<String> applied = new HashSet<>();
    private final Map<ScriptSession, Map<Path, byte[]>> textures = new IdentityHashMap<>();
    private final Map<ScriptSession, ColoredGlow> glows = new IdentityHashMap<>();
    private final AdvancedAccess advanced = new AdvancedAccess();
    private DrawAPI draw;
    private int tickCounter;

    private record TickHandler(ScriptSession owner, Closure<?> closure) {}
    private static final class KeyHandler {
        final ScriptSession owner;
        final Closure<?> closure;
        final int code;
        final boolean release;
        final boolean repeat;
        boolean previous;
        KeyHandler(ScriptSession owner, Closure<?> closure, int code, boolean release, boolean repeat, boolean initial) {
            this.owner = owner; this.closure = closure;
            this.code = code; this.release = release; this.repeat = repeat; this.previous = initial;
        }
    }
    void initializeDraw(ScriptRuntime runtime) { draw = new DrawAPI(runtime); }
    public DrawAPI draw() { return draw; }
    public AdvancedAccess advanced() { return advanced; }
    public AdvancedAccess getAdvanced() { return advanced; }
    // JVM access through the same api object as the rest of McExec.
    public java.lang.reflect.Field findField(Class<?> type, String name) throws ReflectiveOperationException {
        return advanced.findField(type, name);
    }
    public Object getField(Object target, String name) throws ReflectiveOperationException {
        return advanced.get(target, name);
    }
    public void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        advanced.set(target, name, value);
    }
    public void setTrackedField(Object target, String name, Object value) throws ReflectiveOperationException {
        advanced.setTracked(target, name, value);
    }
    public java.util.List<String> fields(Class<?> type) { return advanced.fields(type); }
    public java.util.List<String> methods(Class<?> type) { return advanced.methods(type); }
    public java.lang.invoke.MethodHandles.Lookup privateLookup(Class<?> type) throws IllegalAccessException {
        return advanced.privateLookup(type);
    }
    public java.lang.foreign.Arena arena() { return advanced.arena(); }
    public DrawAPI getDraw() { return draw; }
    public Minecraft client() { return mc; }
    public ScriptSession session() { return MCExecClient.runtime().requireSession(); }
    public Path scriptFolder() { return ScriptStorage.scriptsDir(); }
    public void log(Object message) { MCExecClient.runtime().log(String.valueOf(message)); }
    public void local(String message) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(message));
        log("LOCAL: " + message);
    }
    public void chat(String message) {
        if (mc.player == null) throw new IllegalStateException("Join a world first");
        if (message.startsWith("/")) mc.player.connection.sendCommand(message.substring(1));
        else mc.player.connection.sendChat(message);
        log("SENT: " + message);
    }
    public void command(String command) { chat(command.startsWith("/") ? command : "/" + command); }
    private void playerRequired() {
        if (mc.player == null) throw new IllegalStateException("Join a world first");
    }
    public void defer(Closure<?> cleanup) { session().onStop(() -> cleanup.call()); }
    public void onStop(Closure<?> cleanup) { defer(cleanup); }
    public boolean cancelled() { return session().cancelled(); }
    public int activeScripts() { return MCExecClient.runtime().activeScripts(); }
    public void stopScript(String name) { MCExecClient.runtime().stopScript(name); }
    public void stopAll() { MCExecClient.runtime().stopAll(); }

    // ----- Ticks, keyboard, 2D HUD -----
    public AutoCloseable onTick(Closure<?> action) {
        ScriptSession owner = session();
        TickHandler handler = new TickHandler(owner, action);
        tickHandlers.add(handler);
        owner.onStop(() -> tickHandlers.remove(handler));
        log("Registered tick callback for " + owner.name());
        return () -> tickHandlers.remove(handler);
    }
    public void clearTicks() {
        ScriptSession owner = MCExecClient.runtime().currentSession();
        if (owner != null) tickHandlers.removeIf(handler -> handler.owner() == owner);
        else tickHandlers.clear();
        log("Cleared " + (owner == null ? "all" : owner.name()) + " tick callbacks");
    }
    public AutoCloseable onRender2D(Closure<?> action) {
        ScriptSession owner = session();
        TickHandler handler = new TickHandler(owner, action);
        hudHandlers.add(handler);
        owner.onStop(() -> hudHandlers.remove(handler));
        return () -> hudHandlers.remove(handler);
    }
    /** Raw Fabric level-render callback. */
    public AutoCloseable onRender3D(Closure<?> action) {
        ScriptSession owner = session();
        TickHandler handler = new TickHandler(owner, action);
        worldHandlers.add(handler);
        owner.onStop(() -> worldHandlers.remove(handler));
        return () -> worldHandlers.remove(handler);
    }
    void extractWorld(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        for (TickHandler handler : worldHandlers) {
            if (!handler.owner().active()) { worldHandlers.remove(handler); continue; }
            try { MCExecClient.runtime().invoke(handler.owner(), handler.closure(), ctx); }
            catch (Throwable ex) { worldHandlers.remove(handler); MCExecClient.runtime().error("3D callback removed after failure", ex); }
        }
    }
    void extractHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
        if (draw == null || mc.player == null) return;
        draw.extract(graphics);
        DrawAPI.Canvas canvas = new DrawAPI.Canvas(graphics);
        for (TickHandler callback : hudHandlers) {
            if (!callback.owner().active()) { hudHandlers.remove(callback); continue; }
            try { MCExecClient.runtime().invoke(callback.owner(), callback.closure(), canvas); }
            catch (Throwable ex) { hudHandlers.remove(callback); MCExecClient.runtime().error("HUD callback removed after failure", ex); }
        }
    }
    public int keyCode(String key) {
        String normalized = key.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.startsWith("KEY_")) normalized = normalized.substring(4);
        try { return InputConstants.class.getField("KEY_" + normalized).getInt(null); }
        catch (ReflectiveOperationException e) { throw new IllegalArgumentException("Unknown SDL key: " + key, e); }
    }
    public boolean keyDown(String key) { return keyDown(keyCode(key)); }
    private boolean keyDown(int code) {
        return mc.gui.screen() == null && mc.getWindow() != null
                && code >= 0 && InputConstants.isKeyDown(code);
    }
    public AutoCloseable onKey(String key, Closure<?> action) { return registerKey(key, action, false, false); }
    public AutoCloseable onKeyRelease(String key, Closure<?> action) { return registerKey(key, action, true, false); }
    public AutoCloseable onKeyHeld(String key, Closure<?> action) { return registerKey(key, action, false, true); }
    private AutoCloseable registerKey(String key, Closure<?> action, boolean release, boolean repeat) {
        ScriptSession owner = session();
        int code = keyCode(key);
        KeyHandler handler = new KeyHandler(owner, action, code, release, repeat, keyDown(code));
        keyHandlers.add(handler);
        owner.onStop(() -> keyHandlers.remove(handler));
        return () -> keyHandlers.remove(handler);
    }
    public void clearKeys() {
        ScriptSession owner = MCExecClient.runtime().currentSession();
        if (owner != null) keyHandlers.removeIf(handler -> handler.owner == owner);
        else keyHandlers.clear();
    }
    void tick() {
        tickCounter++;
        MCExecClient.runtime().stopForWorldChange();
        if (mc.player == null || mc.level == null) {
            if (!applied.isEmpty()) releaseAll();
            return;
        }
        if (mc.gui.screen() == null) {
            for (Set<String> movements : held.values()) for (String key : movements) apply(key);
            Set<String> intended = new HashSet<>();
            for (Set<String> keys : held.values()) intended.addAll(keys);
            for (String key : new HashSet<>(applied)) if (!intended.contains(key)) release(key);
        } else releaseAll();
        for (TickHandler handler : tickHandlers) {
            if (!handler.owner().active()) { tickHandlers.remove(handler); continue; }
            try { MCExecClient.runtime().invoke(handler.owner(), handler.closure(), tickCounter); }
            catch (Throwable ex) { tickHandlers.remove(handler); MCExecClient.runtime().error("Tick callback removed after failure", ex); }
        }
        for (KeyHandler handler : keyHandlers) {
            if (!handler.owner.active()) { keyHandlers.remove(handler); continue; }
            boolean now = keyDown(handler.code);
            boolean fire = handler.repeat ? now : handler.release ? handler.previous && !now : !handler.previous && now;
            handler.previous = now;
            if (fire) {
                try { MCExecClient.runtime().invoke(handler.owner, handler.closure, handler.code); }
                catch (Throwable ex) { keyHandlers.remove(handler); MCExecClient.runtime().error("Key callback removed after failure", ex); }
            }
        }
    }

    // ----- Player controls and effect undo -----
    public void look(float yaw, float pitch) {
        playerRequired();
        var player = mc.player;
        float beforeYaw = player.getYRot(), beforePitch = player.getXRot();
        session().onStop(() -> { if (mc.player == player) { player.setYRot(beforeYaw); player.setXRot(beforePitch); } });
        player.setYRot(yaw); player.setXRot(Math.max(-90, Math.min(90, pitch)));
    }
    public void velocity(double x, double y, double z) {
        playerRequired();
        var player = mc.player;
        Vec3 previous = player.getDeltaMovement();
        session().onStop(() -> { if (mc.player == player) player.setDeltaMovement(previous); });
        player.setDeltaMovement(new Vec3(x, y, z));
    }
    private static String normalizedMove(String direction) {
        String key = direction.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("forward", "back", "left", "right", "jump", "sneak", "sprint").contains(key))
            throw new IllegalArgumentException("Invalid movement: " + direction);
        return key;
    }
    public void move(String direction, boolean enabled) {
        ScriptSession owner = session();
        String key = normalizedMove(direction);
        if (enabled && !held.containsKey(owner)) {
            held.put(owner, new HashSet<>());
            owner.onStop(() -> { held.remove(owner); releaseNoLongerHeld(); });
        }
        Set<String> local = held.get(owner);
        if (enabled) local.add(key);
        else if (local != null) { local.remove(key); releaseNoLongerHeld(); }
    }
    public void stopMovement() {
        ScriptSession owner = MCExecClient.runtime().currentSession();
        if (owner == null) held.clear();
        else held.remove(owner);
        releaseNoLongerHeld();
    }
    private void releaseNoLongerHeld() {
        Set<String> requested = new HashSet<>();
        for (Set<String> group : held.values()) requested.addAll(group);
        for (String key : new HashSet<>(applied)) if (!requested.contains(key)) release(key);
    }
    private void releaseAll() { for (String key : new HashSet<>(applied)) release(key); }
    private void release(String key) {
        if (!applied.remove(key)) return;
        mapping(key).setDown(false);
    }
    private void apply(String key) { mapping(key).setDown(true); applied.add(key); }
    private net.minecraft.client.KeyMapping mapping(String key) {
        return switch (key) {
            case "forward" -> mc.options.keyUp;
            case "back" -> mc.options.keyDown;
            case "left" -> mc.options.keyLeft;
            case "right" -> mc.options.keyRight;
            case "jump" -> mc.options.keyJump;
            case "sneak" -> mc.options.keyShift;
            case "sprint" -> mc.options.keySprint;
            default -> throw new IllegalArgumentException(key);
        };
    }
    private net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectType(String id) {
        String full = id.contains(":") ? id : "minecraft:" + id;
        return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(full))
                .orElseThrow(() -> new IllegalArgumentException("Unknown potion effect: " + id));
    }
    public void effect(String id, int seconds, int amplifier) {
        playerRequired();
        var player = mc.player;
        var effect = effectType(id);
        MobEffectInstance prior = player.getEffect(effect);
        session().onStop(() -> {
            if (mc.player != player) return;
            player.removeEffect(effect);
            if (prior != null) player.addEffect(prior);
        });
        player.addEffect(new MobEffectInstance(effect, Math.max(1, seconds * 20), Math.max(0, amplifier)));
    }
    public void nightVision(int seconds) { effect("night_vision", seconds, 0); }
    public void speed(int seconds, int amplifier) { effect("speed", seconds, amplifier); }
    public void clearEffect(String id) {
        playerRequired();
        var player = mc.player;
        var effect = effectType(id);
        MobEffectInstance prior = player.getEffect(effect);
        session().onStop(() -> { if (mc.player == player && prior != null) player.addEffect(prior); });
        player.removeEffect(effect);
    }

    // ----- Terrain rendering -----
    public void xray(String... blocksToKeep) {
        if (blocksToKeep.length == 0) blocksToKeep = new String[] {
            "diamond_ore", "deepslate_diamond_ore", "ancient_debris", "emerald_ore",
            "deepslate_emerald_ore", "gold_ore", "deepslate_gold_ore", "iron_ore",
            "deepslate_iron_ore", "redstone_ore", "deepslate_redstone_ore",
            "lapis_ore", "deepslate_lapis_ore", "coal_ore", "deepslate_coal_ore",
            "copper_ore", "deepslate_copper_ore"
        };
        session().onStop(XrayRender::off);
        XrayRender.showOnly(blocksToKeep);
        log("X-ray enabled; selected " + blocksToKeep.length + " block types");
    }
    public void hideBlocks(String... ids) {
        session().onStop(XrayRender::off);
        XrayRender.hide(ids);
    }
    public void normalRendering() { XrayRender.off(); }
    public String xrayMode() { return XrayRender.mode().name(); }

    // ----- Textures, with saved previous files for emergency rollback -----
    private Path resourcePack() {
        return FabricLoader.getInstance().getGameDir().resolve("resourcepacks/MCExecOverrides").toAbsolutePath().normalize();
    }
    private Path texturePath(String textureId) throws IOException {
        Identifier id = Identifier.parse(textureId.contains(":") ? textureId : "minecraft:" + textureId);
        String path = id.getPath();
        if (!path.matches("[a-z0-9_./-]+") || path.startsWith("/") || path.contains(".."))
            throw new IOException("Invalid texture id: " + textureId);
        Path destination = resourcePack().resolve("assets").resolve(id.getNamespace())
                .resolve("textures").resolve(path + ".png").normalize();
        if (!destination.startsWith(resourcePack())) throw new IOException("Texture path escapes pack");
        return destination;
    }
    private void rememberFile(Path path) throws IOException {
        ScriptSession owner = session();
        if (!textures.containsKey(owner)) {
            textures.put(owner, new LinkedHashMap<>());
            owner.onStop(() -> restoreTextures(owner));
        }
        Map<Path, byte[]> snapshots = textures.get(owner);
        if (!snapshots.containsKey(path)) snapshots.put(path, Files.exists(path) ? Files.readAllBytes(path) : null);
    }
    private void ensurePack() throws IOException {
        Path root = resourcePack();
        Files.createDirectories(root);
        Path metadata = root.resolve("pack.mcmeta");
        if (!Files.exists(metadata)) {
            rememberFile(metadata);
            Files.writeString(metadata, """
                    {"pack":{"description":"McExec generated textures (26.3)",
                    "min_format":[97,1],"max_format":[97,1]}}
                    """);
        }
    }
    public Path overrideTexture(String textureId, String sourceFile) throws IOException {
        Path source = Path.of(sourceFile).toAbsolutePath();
        if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
            throw new IOException("A PNG is required");
        Path path = texturePath(textureId);
        ensurePack();
        rememberFile(path);
        Files.createDirectories(path.getParent());
        Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
        log("Texture exported: " + path);
        return path;
    }
    public Path solidTexture(String textureId, int argb) throws IOException {
        Path tmp = Files.createTempFile("mcexec-texture-", ".png");
        try {
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) image.setRGB(x, y, argb);
            if (!ImageIO.write(image, "PNG", tmp.toFile())) throw new IOException("PNG encoder unavailable");
            return overrideTexture(textureId, tmp.toString());
        } finally { Files.deleteIfExists(tmp); }
    }
    public Path transparentTexture(String textureId) throws IOException { return solidTexture(textureId, 0); }
    public void removeTexture(String id) throws IOException {
        Path destination = texturePath(id);
        rememberFile(destination);
        Files.deleteIfExists(destination);
        reloadTextures();
    }
    public void clearTextures() throws IOException {
        Path assets = resourcePack().resolve("assets");
        if (!Files.isDirectory(assets)) return;
        try (var paths = Files.walk(assets)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".png")).toList()) {
                rememberFile(file);
                Files.deleteIfExists(file);
            }
        }
        reloadTextures();
    }
    private void restoreTextures(ScriptSession owner) {
        Map<Path, byte[]> originals = textures.remove(owner);
        if (originals == null) return;
        for (var entry : originals.entrySet()) {
            try {
                if (entry.getValue() == null) Files.deleteIfExists(entry.getKey());
                else {
                    Files.createDirectories(entry.getKey().getParent());
                    Files.write(entry.getKey(), entry.getValue());
                }
            } catch (IOException e) { MCExecClient.runtime().error("Failed restoring texture " + entry.getKey(), e); }
        }
        reloadTextures();
    }
    public void reloadTextures() {
        mc.reloadResourcePacks();
        log("Requested texture resource reload (enable MCExecOverrides pack in settings)");
    }

    // ----- Fully managed colored glow -----
    public void coloredGlow() throws ReflectiveOperationException { coloredGlow(Map.of()); }
    public void coloredGlow(Map<String, String> colors) throws ReflectiveOperationException {
        ScriptSession owner = session();
        if (glows.containsKey(owner)) { glows.get(owner).stop(); glows.remove(owner); }
        ColoredGlow glow = new ColoredGlow(owner, colors);
        glows.put(owner, glow);
        owner.onStop(() -> { glow.stop(); glows.remove(owner); });
        glow.update();
        onTick(new Closure<Object>(this) {
            public Object doCall(Object tick) { glow.update(); return null; }
        });
        log("Colored entity glow enabled for " + owner.name());
    }
    public void clearColoredGlow() {
        ScriptSession owner = MCExecClient.runtime().currentSession();
        if (owner == null) {
            for (ColoredGlow glow : new ArrayList<>(glows.values())) glow.stop();
            glows.clear();
        } else {
            ColoredGlow glow = glows.remove(owner);
            if (glow != null) glow.stop();
        }
    }

    /** Called by ScriptRuntime.stopAll() after per-session undo operations. */
    void resetManagedState() {
        tickHandlers.clear(); keyHandlers.clear(); hudHandlers.clear(); worldHandlers.clear();
        held.clear(); releaseAll();
        for (ColoredGlow glow : new ArrayList<>(glows.values())) glow.stop();
        glows.clear();
        for (ScriptSession session : new ArrayList<>(textures.keySet())) restoreTextures(session);
        if (draw != null) draw.clearAll();
        XrayRender.off();
    }
}
