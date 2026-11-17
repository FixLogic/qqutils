package fixlogic.qqutils.mixin;

import fixlogic.qqutils.Qqutils;
import fixlogic.qqutils.gui.WidgetHitTest;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class ServerEntryMixin extends ServerSelectionList.Entry {
    @Shadow @Final private ServerData serverData;
    @Shadow protected abstract void refreshStatus();

    @Unique private Button qqutils$refreshButton;

    @Unique
    private Button qqutils$positionButton() {
        if (qqutils$refreshButton == null) {
            qqutils$refreshButton = Button.builder(Component.translatable("qqutils.server_refresh.button"), button -> {
                if (Qqutils.serverRefresh().refresh(serverData)) refreshStatus();
            }).size(14, 14)
                .tooltip(Tooltip.create(Component.translatable("qqutils.server_refresh.tooltip", serverData.name)))
                .createNarration(ignored -> Component.translatable("qqutils.server_refresh.tooltip", serverData.name))
                .build();
        }
        // Vanilla draws the 10px ping icon at contentRight - 15, contentY.
        qqutils$refreshButton.setX(getContentRight() - 17);
        qqutils$refreshButton.setY(getContentY() + 12);
        qqutils$refreshButton.active = Qqutils.serverRefresh().canRefresh(serverData);
        return qqutils$refreshButton;
    }

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void qqutils$startPendingRefresh(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                             boolean hovered, float delta, CallbackInfo ci) {
        if (Qqutils.serverRefresh().startPendingRefresh(serverData)) refreshStatus();
    }

    @Inject(method = "extractContent", at = @At("TAIL"))
    private void qqutils$drawRefresh(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                     boolean hovered, float delta, CallbackInfo ci) {
        if (Qqutils.serverRefresh().showsButton()) {
            qqutils$positionButton().extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    @ModifyArg(method = "extractContent", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), index = 1)
    private int qqutils$reserveButtonSpace(int width) {
        return Qqutils.serverRefresh().showsButton() ? Math.max(1, width - 18) : width;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void qqutils$clickRefresh(MouseButtonEvent event, boolean doubleClick,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (!Qqutils.serverRefresh().showsButton() || event.button() != 0) return;
        var button = qqutils$positionButton();
        if (WidgetHitTest.contains(button, event.x(), event.y())) {
            button.mouseClicked(event, doubleClick);
            // Consume even while disabled: repeated clicks must never join the server.
            cir.setReturnValue(true);
        }
    }
}
