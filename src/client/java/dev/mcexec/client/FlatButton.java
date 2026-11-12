package dev.mcexec.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** Lightweight flat action button, independent of vanilla button textures. */
public final class FlatButton extends AbstractWidget {
    private final Runnable action;
    private final boolean accent;

    public FlatButton(String label, int x, int y, int w, int h, boolean accent, Runnable action) {
        super(x, y, w, h, Component.literal(label));
        this.action = action;
        this.accent = accent;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int color = accent ? (isHovered() ? UISettings.color(3) : UISettings.color(4))
                : (isHovered() ? UISettings.color(3) : UISettings.color(7));
        g.fill(getX(), getY(), getRight(), getBottom(), color);
        g.centeredText(net.minecraft.client.Minecraft.getInstance().font,
                getMessage(), getX() + width / 2, getY() + (height - 9) / 2,
                UISettings.color(5));
    }

    @Override
    public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        action.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        // The visual label is also exposed through getMessage().
    }
}
