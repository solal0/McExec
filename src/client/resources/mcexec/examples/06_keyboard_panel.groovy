if (mc.player == null) { api.local('Join a world first.'); return }

int leftPadding = 12
int bottomPadding = 12
int keySize = 25
int keyGap = 4

api.onRender2D { canvas ->
    if (mc.player == null) return

    int bottomRow = canvas.height() - bottomPadding - keySize
    int middleRow = bottomRow - keySize - keyGap
    int topRow = middleRow - keySize - keyGap

    def drawKey = { String keyName, int x, int y, int width, int height, boolean pressed ->
        int background = pressed ? 0xFFEEF6FF as int : 0x88303B4E as int
        int foreground = pressed ? 0xFF182433 as int : 0xFFD6E4FF as int
        canvas.rect(x, y, width, height, background)
        canvas.outline(x, y, width, height, 0xFF7497BD as int)
        canvas.centeredText(x + width.intdiv(2), y + (height - 8).intdiv(2), keyName, foreground)
    }

    // Mouse clicks use Minecraft's input mappings so the display follows rebinding.
    boolean leftClickPressed = mc.options.keyAttack.isDown()
    boolean rightClickPressed = mc.options.keyUse.isDown()

    drawKey('W', leftPadding + keySize + keyGap, topRow, keySize, keySize, api.keyDown('W'))
    drawKey('A', leftPadding, middleRow, keySize, keySize, api.keyDown('A'))
    drawKey('S', leftPadding + keySize + keyGap, middleRow, keySize, keySize, api.keyDown('S'))
    drawKey('D', leftPadding + 2 * (keySize + keyGap), middleRow, keySize, keySize, api.keyDown('D'))

    drawKey('SPACE', leftPadding, bottomRow, 3 * keySize + 2 * keyGap, keySize, api.keyDown('SPACE'))
    int extraKeysX = leftPadding + 3 * (keySize + keyGap) + 8
    drawKey('SHIFT', extraKeysX, middleRow, 46, keySize, api.keyDown('LSHIFT'))
    drawKey('CTRL', extraKeysX, bottomRow, 46, keySize, api.keyDown('LCTRL'))
    drawKey('LMB', extraKeysX + 51, middleRow, 32, keySize, leftClickPressed)
    drawKey('RMB', extraKeysX + 51, bottomRow, 32, keySize, rightClickPressed)
}
