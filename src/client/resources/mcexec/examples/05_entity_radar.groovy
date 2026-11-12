import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.item.ItemEntity

if (mc.player == null || mc.level == null) { api.local('Join a world first.'); return }

// Adjust these two values to change radar range and size.
double radarRadius = 48d
int radarSize = 124
int radarLeft = 12
int radarTop = 12

api.onRender2D { canvas ->
    def localPlayer = mc.player
    if (localPlayer == null || mc.level == null) return

    int radarCenterX = radarLeft + radarSize.intdiv(2)
    int radarCenterY = radarTop + radarSize.intdiv(2)
    double pixelsPerBlock = (radarSize / 2d - 6d) / radarRadius
    double playerYawRadians = Math.toRadians(localPlayer.getYRot())
    double yawCosine = Math.cos(playerYawRadians)
    double yawSine = Math.sin(playerYawRadians)

    canvas.outline(radarLeft, radarTop, radarSize, radarSize, 0xFF65A6BB as int)
    canvas.line(radarCenterX, radarTop + 2, radarCenterX, radarTop + radarSize - 3,
            0x4C84B7CA as int)
    canvas.line(radarLeft + 2, radarCenterY, radarLeft + radarSize - 3, radarCenterY,
            0x4C84B7CA as int)
    canvas.rect(radarCenterX - 2, radarCenterY - 2, 5, 5, 0xFFFFFFFF as int)
    canvas.line(radarCenterX, radarCenterY - 7, radarCenterX, radarCenterY - 3, 0xFFFFFFFF as int)

    mc.level.entitiesForRendering().each { entity ->
        if (entity == localPlayer) return

        double deltaX = entity.x - localPlayer.x
        double deltaZ = entity.z - localPlayer.z
        if (deltaX * deltaX + deltaZ * deltaZ > radarRadius * radarRadius) return

        double rightwardDistance = deltaX * yawCosine - deltaZ * yawSine
        double forwardDistance = -deltaX * yawSine + deltaZ * yawCosine
        int dotX = radarCenterX + Math.round(rightwardDistance * pixelsPerBlock) as int
        int dotY = radarCenterY - Math.round(forwardDistance * pixelsPerBlock) as int
        int dotColor = entity instanceof Monster ? 0xFFFF526B as int
                     : entity instanceof Player ? 0xFF58D8FF as int
                     : entity instanceof Animal ? 0xFF73F5A8 as int
                     : entity instanceof ItemEntity ? 0xFFFFD567 as int
                     : 0xFFBFA8DD as int
        canvas.rect(dotX - 1, dotY - 1, 3, 3, dotColor)
    }
}
