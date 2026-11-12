if (mc.player == null) { api.local('Join a world first.'); return }

int walkDurationTicks = 20
int ticksRemaining = 0
boolean walking = false

api.onKey('J') { key ->
    walking = true
    ticksRemaining = walkDurationTicks
    api.move('forward', true)
}
api.onKey('K') { key ->
    walking = false
    ticksRemaining = 0
    api.move('forward', false)
}
api.onTick { tick ->
    if (!walking) return
    ticksRemaining--
    if (ticksRemaining <= 0) {
        walking = false
        api.move('forward', false)
    }
}
api.onRender2D { canvas ->
    int centerX = canvas.width().intdiv(2)
    int topY = canvas.height() - 66
    int barWidth = 164
    int barLeft = centerX - barWidth.intdiv(2)

    canvas.centeredText(centerX, topY,
            walking ? 'AUTO WALK' : 'J: WALK  |  K: CANCEL',
            walking ? 0xFF52F3BA as int : 0xFFBFD0FF as int)
    canvas.rect(barLeft, topY + 14, barWidth, 5, 0xFF354053 as int)
    if (walking) {
        int progressWidth = Math.round(barWidth * ticksRemaining / (float) walkDurationTicks)
        canvas.rect(barLeft, topY + 14, progressWidth, 5, 0xFF52F3BA as int)
    }
}
