import net.minecraft.world.phys.EntityHitResult
import java.lang.reflect.Modifier

if (mc.player == null) { api.local('Join a world first.'); return }

// Press I while aiming at a mob to watch internal fields change live.
// This reads Java fields through api.getField(), including private members.
Object inspectedEntity = mc.player
String targetName = 'Player'
List<String> watchedFields = []
Map<String, String> values = [:]
Map<String, String> previousValues = [:]

// Favor fields whose values are likely to change while playing.
def chooseFields = { Object target ->
    List<String> found = []
    Class<?> type = target.class
    while (type != null && found.size() < 50) {
        type.declaredFields.each { field ->
            if (!Modifier.isStatic(field.modifiers) && !field.synthetic && field.type.isPrimitive()) {
                if (!found.contains(field.name)) found.add(field.name)
            }
        }
        type = type.superclass
    }
    def priority = { String name ->
        name.toLowerCase().matches('.*(tick|hurt|swing|walk|move|rot|age|time|speed).*') ? 0 : 1
    }
    watchedFields = found.sort { first, second ->
        int comparison = priority(first) <=> priority(second)
        comparison != 0 ? comparison : first <=> second
    }.take(5)
    values = [:]
    previousValues = [:]
}
chooseFields(inspectedEntity)
api.local('Aim at an entity and press I to inspect it.')

api.onKey('I') { key ->
    def targetHit = mc.hitResult
    inspectedEntity = targetHit instanceof EntityHitResult ? targetHit.entity : mc.player
    targetName = inspectedEntity.type.toString()
    chooseFields(inspectedEntity)
}

int refreshTicks = 0
api.onTick { tick ->
    if (++refreshTicks % 4 != 0 || inspectedEntity == null) return
    previousValues = new LinkedHashMap<>(values)
    Map<String, String> fresh = [:]
    watchedFields.each { fieldName ->
        try {
            Object currentValue = api.getField(inspectedEntity, fieldName)
            fresh[fieldName] = String.valueOf(currentValue)
        } catch (ReflectiveOperationException ignored) {
            fresh[fieldName] = '(unavailable)'
        }
    }
    values = fresh
}

api.onRender2D { canvas ->
    int rightX = Math.max(8, canvas.width() - 218)
    int textY = 100
    canvas.text(rightX, textY, targetName.take(22), 0xFFCF98FF as int)
    watchedFields.eachWithIndex { fieldName, index ->
        String currentValue = values.getOrDefault(fieldName, '...')
        boolean changed = previousValues.containsKey(fieldName)
                && previousValues[fieldName] != currentValue
        String line = fieldName + ': ' + currentValue
        canvas.text(rightX, textY + 15 + index * 13, line.take(35),
                changed ? 0xFF5BF7C6 as int : 0xFFC9D5E8 as int)
    }
}
