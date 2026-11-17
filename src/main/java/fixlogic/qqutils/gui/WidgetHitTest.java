package fixlogic.qqutils.gui;

import net.minecraft.client.gui.components.AbstractWidget;

/** Geometric hit test that also consumes clicks over disabled controls. */
public final class WidgetHitTest {
    private WidgetHitTest() { }

    public static boolean contains(AbstractWidget widget, double x, double y) {
        return widget.visible && x >= widget.getX() && y >= widget.getY()
            && x < widget.getX() + widget.getWidth() && y < widget.getY() + widget.getHeight();
    }
}
