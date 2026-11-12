package dev.mcexec.client.mixin;

import dev.mcexec.client.CodeEditorMarker;
import dev.mcexec.client.GroovyColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxSyntaxMixin implements CodeEditorMarker {
    @Unique private boolean mcexec$syntax;

    @Override public void mcexec$setSyntax(boolean enabled) { mcexec$syntax = enabled; }

    @Redirect(method = "extractContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V"))
    private void mcexec$coloredText(GuiGraphicsExtractor graphics, Font font, String value,
                                    int x, int y, int color, boolean shadow) {
        if (mcexec$syntax) graphics.text(font, GroovyColors.line(value), x, y, 0xFFFFFFFF, shadow);
        else graphics.text(font, value, x, y, color, shadow);
    }
}
