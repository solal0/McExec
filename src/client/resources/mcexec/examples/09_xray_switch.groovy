if (mc.player == null || mc.level == null) { api.local('Join a world first.'); return }

boolean filterEnabled = false
api.local('X: toggle ore X-ray')

api.onKey('X') { key ->
    filterEnabled = !filterEnabled
    if (filterEnabled) {
        api.xray('diamond_ore', 'deepslate_diamond_ore', 'ancient_debris')
    } else {
        api.normalRendering()
    }
    api.local(filterEnabled ? 'X-RAY: ON' : 'X-RAY: OFF')
}
