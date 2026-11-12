import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.item.ItemEntity

if (mc.player == null || mc.level == null) { api.local('Join a world first.'); return }

api.coloredGlow([players: 'AQUA', hostile: 'RED', animals: 'GREEN',
                 items: 'YELLOW', other: 'LIGHT_PURPLE'])

def entityCounts = [players: 0, hostile: 0, animals: 0, items: 0, other: 0]
int refreshTicks = 0
api.onTick { tick ->
    if (++refreshTicks % 12 != 0 || mc.level == null) return
    entityCounts = [players: 0, hostile: 0, animals: 0, items: 0, other: 0]
    mc.level.entitiesForRendering().each { entity ->
        if (entity == mc.player) return
        if (entity instanceof Player) entityCounts.players++
        else if (entity instanceof Monster) entityCounts.hostile++
        else if (entity instanceof Animal) entityCounts.animals++
        else if (entity instanceof ItemEntity) entityCounts.items++
        else entityCounts.other++
    }
}

api.onRender2D { canvas ->
    int textX = 12
    int textY = 11
    def fragments = [
        ['Players: ' + entityCounts.players, 0xFF55FFFF as int],
        ['Monsters: ' + entityCounts.hostile, 0xFFFF6A6A as int],
        ['Animals: ' + entityCounts.animals, 0xFF65FF9D as int],
        ['Items: ' + entityCounts.items, 0xFFFFE877 as int],
        ['Other: ' + entityCounts.other, 0xFFDBA5FF as int]
    ]
    fragments.eachWithIndex { item, index ->
        String label = item[0]
        canvas.text(textX, textY, label, item[1] as int)
        textX += mc.font.width(label) + 12
    }
}
