if (mc.player == null) { api.local('Join a world first.'); return }

long startedAt = System.nanoTime()

api.onRender2D { canvas ->
    double seconds = (System.nanoTime() - startedAt) / 1_000_000_000d
    int centerX = canvas.width().intdiv(2)
    int centerY = Math.max(116, canvas.height().intdiv(3))
    double rotation = seconds * 1.9d
    int red = 128 + Math.round(127d * Math.sin(seconds * 1.3d)) as int
    int green = 128 + Math.round(127d * Math.sin(seconds * 1.3d + 2.1d)) as int
    int blue = 128 + Math.round(127d * Math.sin(seconds * 1.3d + 4.2d)) as int
    int movingColor = (0xFF000000 | (red << 16) | (green << 8) | blue) as int

    int movingX = centerX - 20 + Math.round(Math.sin(seconds * 1.6d) * 90d) as int
    int movingY = centerY - 11 + Math.round(Math.cos(seconds * 2.2d) * 18d) as int
    canvas.rect(movingX + 5, movingY + 5, 40, 24, 0x66000000 as int)
    canvas.rect(movingX, movingY, 40, 24, movingColor)
    canvas.graphics().fill(movingX + 5, movingY + 5, movingX + 7, movingY + 7, 0xFFFFFFFF as int)
    canvas.outline(movingX - 2, movingY - 2, 44, 28, 0xFFFFFFFF as int)

    int spinX = centerX + 105
    int spinY = centerY + 47
    for (int spoke = 0; spoke < 4; spoke++) {
        double angle = rotation + spoke * Math.PI / 2d
        int tipX = spinX + Math.round(Math.cos(angle) * 33d) as int
        int tipY = spinY + Math.round(Math.sin(angle) * 33d) as int
        canvas.line(spinX, spinY, tipX, tipY, movingColor)
        canvas.rect(tipX - 2, tipY - 2, 5, 5, 0xFFFFFFFF as int)
    }

    for (int waveIndex = 0; waveIndex < 23; waveIndex++) {
        int waveX = centerX - 116 + waveIndex * 10
        int waveY = centerY + 78 + Math.round(Math.sin(seconds * 3d + waveIndex * 0.43d) * 12d) as int
        canvas.rect(waveX, waveY, 5, 5, movingColor)
    }
}
