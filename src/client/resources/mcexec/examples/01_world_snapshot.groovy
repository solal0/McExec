import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.item.ItemEntity

if (mc.player == null || mc.level == null) {
    api.local('Join a world first.')
    return
}

def localPlayer = mc.player
def currentWorld = mc.level
def playerInventory = localPlayer.inventory

def playerPosition = localPlayer.blockPosition()
def blockBelow = playerPosition.below()
def blockState = currentWorld.getBlockState(blockBelow)

log("PLAYER: ${localPlayer.name.string}")
log(String.format('POSITION: %.1f, %.1f, %.1f', localPlayer.x, localPlayer.y, localPlayer.z))
log(String.format('ROTATION: yaw %.1f, pitch %.1f', localPlayer.getYRot(), localPlayer.getXRot()))
log("HEALTH: ${localPlayer.health}/${localPlayer.maxHealth}")
log("HUNGER: ${localPlayer.foodData.foodLevel}/20")
log("EXPERIENCE: level ${localPlayer.experienceLevel}")
log("SPRINTING: ${localPlayer.isSprinting()}")
log("DIMENSION: ${currentWorld.dimension()}")
log("WORLD TICKS: ${currentWorld.getGameTime()}")
log("BIOME: ${currentWorld.getBiome(playerPosition)}")
log("BLOCK UNDERFOOT: ${BuiltInRegistries.BLOCK.getKey(blockState.block)}")
log("MAIN HAND: ${localPlayer.mainHandItem}")
log("OFF HAND: ${localPlayer.offhandItem}")

int occupiedSlots = 0
int itemCount = 0
for (int slotIndex = 0; slotIndex < playerInventory.containerSize; slotIndex++) {
    def itemStack = playerInventory.getItem(slotIndex)
    if (!itemStack.empty) {
        occupiedSlots++
        itemCount += itemStack.count
    }
}
log("INVENTORY: ${occupiedSlots} occupied slots, ${itemCount} items")

def entityCounts = [players: 0, hostile: 0, animals: 0, drops: 0, other: 0]
currentWorld.entitiesForRendering().each { entity ->
    if (entity == localPlayer) return
    if (entity instanceof Player) entityCounts.players++
    else if (entity instanceof Monster) entityCounts.hostile++
    else if (entity instanceof Animal) entityCounts.animals++
    else if (entity instanceof ItemEntity) entityCounts.drops++
    else entityCounts.other++
}
log("LOADED ENTITIES: ${entityCounts}")
log("JAVA: ${Runtime.version().feature()} | SCRIPTS: ${api.activeScripts()}")
api.local('World snapshot saved to McExec Console.')
