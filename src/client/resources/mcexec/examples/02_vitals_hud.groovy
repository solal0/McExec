if (mc.player == null) { api.local('Join a world first.'); return }

int leftPadding = 12
int bottomPadding = 14
int barWidth = 172
int barHeight = 17
int barSpacing = 8

api.onRender2D { canvas ->
    def localPlayer = mc.player
    if (localPlayer == null) return

    float health = Math.max(0f, localPlayer.health)
    float maxHealth = Math.max(1f, localPlayer.maxHealth)
    float hunger = localPlayer.foodData.foodLevel as float
    int healthFill = Math.round(barWidth * Math.min(1f, health / maxHealth))
    int hungerFill = Math.round(barWidth * Math.min(1f, hunger / 20f))

    int hungerY = canvas.height() - bottomPadding - barHeight
    int healthY = hungerY - barSpacing - barHeight
    int healthColor = health <= 6f ? 0xFFFF486A as int : 0xFF4CE8A0 as int

    canvas.rect(leftPadding, healthY, barWidth, barHeight, 0xA0222A31 as int)
    canvas.rect(leftPadding, healthY, healthFill, barHeight, healthColor)
    canvas.text(leftPadding + 6, healthY + 4, String.format('%.1f', health), 0xFFFFFFFF as int)

    canvas.rect(leftPadding, hungerY, barWidth, barHeight, 0xA0222A31 as int)
    canvas.rect(leftPadding, hungerY, hungerFill, barHeight, 0xFFFFB64E as int)
    canvas.text(leftPadding + 6, hungerY + 4, String.format('%.1f', hunger), 0xFFFFFFFF as int)
}
