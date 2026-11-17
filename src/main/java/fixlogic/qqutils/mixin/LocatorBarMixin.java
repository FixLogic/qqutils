package fixlogic.qqutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import fixlogic.qqutils.Qqutils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import net.minecraft.resources.Identifier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adapter only: vanilla owns visibility, positions, icons and interpolation. */
@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void qqutils$beginFrame(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        Qqutils.locatorNames().beginFrame();
    }

    @WrapOperation(method = "lambda$extractRenderState$1", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
    private void qqutils$collectMarker(GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite,
                                      int x, int y, int width, int height, int color, Operation<Void> original,
                                      @Local(argsOnly = true) TrackedWaypoint waypoint) {
        original.call(graphics, pipeline, sprite, x, y, width, height, color);
        Qqutils.locatorNames().collect(waypoint, x, y, width, color);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void qqutils$drawNames(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        Qqutils.locatorNames().render(graphics);
    }
}
