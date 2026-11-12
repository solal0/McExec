package dev.mcexec.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GroovyColors {
    private static final Set<String> KEYWORDS = Set.of("def", "import", "as", "new", "if", "else", "for", "while", "return", "class", "static", "final", "try", "catch", "throw", "true", "false", "null", "in", "instanceof", "this", "int", "long", "boolean", "float", "double", "String", "void", "switch", "case", "break", "continue", "assert", "enum", "public", "private", "protected");
    private static final Set<String> BINDINGS = Set.of("api", "draw", "log", "mc", "player", "world", "session");
    private static final Pattern TOKENS = Pattern.compile("//.*|\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|'(?:\\\\.|[^'\\\\])*'|\\b[A-Za-z_][A-Za-z_0-9]*\\b|\\b\\d+(?:\\.\\d+)?\\b");
    private static final int[][] THEMES = {
            {0xFFDB85FF, 0xFF8FE6A8, 0xFF6F859A, 0xFFFFC56E, 0xFF67D4FF, 0xFFFF8EAE},
            {0xFFFF5FC8, 0xFF50FFB1, 0xFF6296B3, 0xFFFFEF63, 0xFF3DE8FF, 0xFFFF9F54},
            {0xFFB9A9E7, 0xFF98CDB9, 0xFF8693AD, 0xFFE6BF9C, 0xFF90C8E8, 0xFFE2A7C5},
            {0xFFEBEBEB, 0xFFB5B5B5, 0xFF7F7F7F, 0xFFDDDDDD, 0xFFFFFFFF, 0xFF999999}
    };
    private GroovyColors() {}
    public static Component line(String line) {
        MutableComponent out = Component.empty();
        Matcher m = TOKENS.matcher(line);
        int last = 0;
        int[] c = THEMES[UISettings.syntax];
        while (m.find()) {
            if (m.start() > last) out.append(Component.literal(line.substring(last, m.start())).withColor(UISettings.color(5)));
            String token = m.group();
            int color = token.startsWith("//") ? c[2]
                    : token.startsWith("'") || token.startsWith("\"") ? c[1]
                    : Character.isDigit(token.charAt(0)) ? c[3]
                    : KEYWORDS.contains(token) ? c[0]
                    : BINDINGS.contains(token) ? c[4]
                    : m.start() > 0 && line.charAt(m.start() - 1) == '.' ? c[5]
                    : m.end() < line.length() && line.charAt(m.end()) == '(' ? c[5]
                    : UISettings.color(5);
            out.append(Component.literal(token).withColor(color));
            last = m.end();
        }
        if (last < line.length()) out.append(Component.literal(line.substring(last)).withColor(UISettings.color(5)));
        return out;
    }
}
