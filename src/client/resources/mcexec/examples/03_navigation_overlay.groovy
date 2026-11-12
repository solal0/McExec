if (mc.player == null) { api.local('Join a world first.'); return }

api.onRender2D { canvas ->
    def localPlayer = mc.player
    if (localPlayer == null) return

    double horizontalSpeed = Math.hypot(localPlayer.deltaMovement.x,
                                        localPlayer.deltaMovement.z) * 20d
    float yaw = ((localPlayer.getYRot() % 360f) + 360f) % 360f
    String cardinal = yaw < 45f || yaw >= 315f ? 'S'
                    : yaw < 135f ? 'W' : yaw < 225f ? 'N' : 'E'

    int right = canvas.width() - 14
    int centerX = right - 78
    int startY = 13
    canvas.line(centerX, startY + 12, centerX, startY + 17, 0xFF50E7FF as int)
    canvas.centeredText(centerX, startY + 20, cardinal + '  ' + Math.round(yaw) + '°', 0xFF4EE5FF as int)
    canvas.centeredText(centerX, startY + 34,
            String.format('XYZ %.0f  %.0f  %.0f', localPlayer.x, localPlayer.y, localPlayer.z),
            0xFFFFFFFF as int)
    canvas.centeredText(centerX, startY + 46,
            String.format('%.2f blocks/s', horizontalSpeed), 0xFFFFD179 as int)
}
