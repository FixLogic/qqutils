package fixlogic.qqutils.feature.locator;

import fixlogic.qqutils.config.ConfigStore;
import fixlogic.qqutils.hud.HudLabelRenderer;
import fixlogic.qqutils.hud.LabelLayout;
import fixlogic.qqutils.module.ClientModule;
import fixlogic.qqutils.player.PlayerNameResolver;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.waypoints.TrackedWaypoint;
import net.minecraft.util.ARGB;

public final class LocatorNamesModule extends ClientModule {
    private final ConfigStore config;
    private final List<Label> labels = new ArrayList<>();

    public LocatorNamesModule(ConfigStore config) {
        super("locator_names", config);
        this.config = config;
    }

    public void beginFrame() {
        labels.clear();
    }

    public void collect(TrackedWaypoint waypoint, int markerX, int markerY, int markerWidth, int markerColor) {
        if (!isEnabled()) return;
        waypoint.id().left().flatMap(uuid -> PlayerNameResolver.resolve(Minecraft.getInstance(), uuid))
            .ifPresent(name -> labels.add(new Label(name, markerX + markerWidth / 2, markerY, markerColor)));
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (!isEnabled() || labels.isEmpty()) return;
        var font = Minecraft.getInstance().font;
        var settings = config.locatorNames();
        int height = HudLabelRenderer.height(font, settings.scale);
        var layout = new LabelLayout();
        // Stable ordering prevents labels swapping rows as waypoint distance ordering changes.
        labels.sort(Comparator.comparing(Label::name));
        for (var label : labels) {
            String text = font.plainSubstrByWidth(label.name(),
                Math.max(0, (int) (graphics.guiWidth() / settings.scale) - 4));
            int width = HudLabelRenderer.width(font, text, settings.scale);
            // Reserve room for vanilla's arrow above the marker.
            int bottom = label.markerY() - 5 - settings.gap;
            var placement = layout.place(label.centerX(), width, graphics.guiWidth(), bottom / (height + 2));
            if (placement != null) {
                HudLabelRenderer.draw(graphics, font, text, placement.x(),
                    bottom - height - placement.row() * (height + 2), settings.scale, settings.background,
                    ARGB.srgbLerp(0.35f, 0xFFFFFFFF, ARGB.opaque(label.color())));
            }
        }
        labels.clear();
    }

    private record Label(String name, int centerX, int markerY, int color) { }
}
