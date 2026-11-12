if (mc.player == null) { api.local('Join a world first.'); return }

// Hotbar is 182 GUI pixels wide, regardless of the selected GUI scale.
int hotbarWidth = 182
int slotStep = 20
int firstSlotCenter = 11

api.onRender2D { canvas ->
    def localPlayer = mc.player
    if (localPlayer == null) return

    def inventory = localPlayer.inventory
    int hotbarLeft = (canvas.width() - hotbarWidth).intdiv(2)
    int labelY = canvas.height() - 32

    for (int slotIndex = 0; slotIndex < 9; slotIndex++) {
        def itemStack = inventory.getItem(slotIndex)

        int slotCenterX = hotbarLeft + firstSlotCenter + slotIndex * slotStep
        String countLabel = itemStack.empty ? '0' : itemStack.count.toString()
        int textColor = slotIndex == inventory.getSelectedSlot()
                ? 0xFFFFFF7A as int : 0xFFBFE8FF as int
        canvas.centeredText(slotCenterX, labelY, countLabel, textColor)
    }
}
