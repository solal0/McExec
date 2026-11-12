package dev.mcexec.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import java.io.IOException;

/** McExec Fabric entrypoint, Java 25 and Minecraft 26.3 only. */
public final class MCExecClient implements ClientModInitializer {
    private static final ScriptAPI API = new ScriptAPI();
    private static final ScriptRuntime RUNTIME = new ScriptRuntime(API);
    private static KeyMapping toggle;
    private static KeyMapping emergency;

    public static ScriptAPI api() { return API; }
    public static ScriptRuntime runtime() { return RUNTIME; }

    static void refreshKeys() {
        if (toggle != null) UISettings.remap(toggle, UISettings.toggle);
        if (emergency != null) UISettings.remap(emergency, UISettings.stop);
    }

    @Override
    public void onInitializeClient() {
        UISettings.load();
        API.initializeDraw(RUNTIME);
        try { ScriptStorage.seedExamples(); }
        catch (IOException e) { RUNTIME.error("Could not copy example scripts", e); }
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("mcexec", "main"));
        toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mcexec.open", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT, category));
        emergency = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mcexec.panic", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_F10, category));
        refreshKeys();
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("mcexec", "hud"), (graphics, tick) ->
                API.extractHud(graphics));
        LevelRenderEvents.END_MAIN.register(API::extractWorld);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (emergency.consumeClick()) RUNTIME.stopAll();
            while (toggle.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new ConsoleScreen());
                    UISettings.applyScale();
                }
            }
            API.tick();
        });
    }
}
