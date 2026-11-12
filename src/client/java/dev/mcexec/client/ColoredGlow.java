package dev.mcexec.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Client-only colored glowing outlines using scoreboard teams + 0x40 entity data. */
final class ColoredGlow {
    private final Minecraft mc = Minecraft.getInstance();
    private final ClientLevel level;
    private final Scoreboard board;
    private final Map<String, PlayerTeam> teams = new LinkedHashMap<>();
    private final Map<String, PlayerTeam> originalTeams = new LinkedHashMap<>();
    private final Map<Entity, Boolean> originalFlags = new IdentityHashMap<>();
    private final EntityDataAccessor<Byte> glowAccessor;
    private boolean active = true;

    @SuppressWarnings("unchecked")
    ColoredGlow(ScriptSession owner, Map<String, String> selected) throws ReflectiveOperationException {
        if (mc.level == null) throw new IllegalStateException("Join a world first");
        level = mc.level;
        board = level.getScoreboard();
        Field field = Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
        field.setAccessible(true);
        glowAccessor = (EntityDataAccessor<Byte>) field.get(null);
        Map<String, String> colors = new LinkedHashMap<>(Map.of(
                "players", "AQUA", "hostile", "RED", "animals", "GREEN",
                "items", "YELLOW", "other", "LIGHT_PURPLE"));
        colors.putAll(selected);
        // Validate all arguments and name collisions before mutating the scoreboard.
        Map<String, TeamColor> palette = new LinkedHashMap<>();
        for (String category : new String[]{"players", "hostile", "animals", "items", "other"}) {
            String name = "mce" + owner.id() + category.substring(0, 2);
            if (board.getPlayerTeam(name) != null)
                throw new IllegalStateException("Scoreboard team name collision: " + name);
            palette.put(category, TeamColor.valueOf(colors.get(category).toUpperCase(Locale.ROOT)));
        }
        try {
            for (var entry : palette.entrySet()) {
                String name = "mce" + owner.id() + entry.getKey().substring(0, 2);
                PlayerTeam team = board.addPlayerTeam(name);
                teams.put(entry.getKey(), team);
                team.setColor(Optional.of(entry.getValue()));
            }
        } catch (RuntimeException error) {
            for (PlayerTeam team : teams.values()) {
                if (board.getPlayerTeam(team.getName()) == team) board.removePlayerTeam(team);
            }
            teams.clear();
            throw error;
        }
    }
    private String kind(Entity e) {
        if (e instanceof Player) return "players";
        if (e instanceof Monster) return "hostile";
        if (e instanceof Animal) return "animals";
        if (e instanceof ItemEntity) return "items";
        return "other";
    }
    void update() {
        if (!active || mc.level != level) return;
        for (Entity e : level.entitiesForRendering()) {
            if (e == mc.player) continue;
            String name = e.getScoreboardName();
            PlayerTeam team = teams.get(kind(e));
            if (!originalTeams.containsKey(name)) originalTeams.put(name, board.getPlayersTeam(name));
            if (board.getPlayersTeam(name) != team) board.addPlayerToTeam(name, team);
            int current = e.getEntityData().get(glowAccessor) & 0xFF;
            originalFlags.putIfAbsent(e, (current & 0x40) != 0);
            if ((current & 0x40) == 0) e.getEntityData().set(glowAccessor, (byte)(current | 0x40));
        }
    }
    void stop() {
        if (!active) return;
        active = false;
        for (Map.Entry<Entity, Boolean> entry : originalFlags.entrySet()) {
            Entity entity = entry.getKey();
            if (entity.isRemoved()) continue;
            int current = entity.getEntityData().get(glowAccessor) & 0xFF;
            int restored = entry.getValue() ? current | 0x40 : current & ~0x40;
            entity.getEntityData().set(glowAccessor, (byte)restored);
        }
        for (Map.Entry<String, PlayerTeam> entry : originalTeams.entrySet()) {
            String name = entry.getKey();
            if (teams.containsValue(board.getPlayersTeam(name))) {
                board.removePlayerFromTeam(name);
                PlayerTeam oldTeam = entry.getValue();
                if (oldTeam != null && board.getPlayerTeam(oldTeam.getName()) == oldTeam)
                    board.addPlayerToTeam(name, oldTeam);
            }
        }
        for (PlayerTeam team : teams.values()) {
            if (board.getPlayerTeam(team.getName()) == team) board.removePlayerTeam(team);
        }
        originalFlags.clear();
        originalTeams.clear();
        teams.clear();
    }
}
