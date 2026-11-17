package fixlogic.qqutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fixlogic.qqutils.Qqutils;
import fixlogic.qqutils.gui.ServerListScrollAccess;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MultiplayerScreenMixin implements ServerListScrollAccess {
    @Shadow protected ServerSelectionList serverSelectionList;

    @WrapOperation(method = "refreshServerList", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void qqutils$keepScroll(Gui gui, Screen refreshedScreen, Operation<Void> original) {
        boolean preserve = Qqutils.serverRefresh().isEnabled() && serverSelectionList != null;
        double scroll = preserve ? serverSelectionList.scrollAmount() : 0;
        original.call(gui, refreshedScreen);
        // setScreen initializes and lays out the new list before scroll clamping is applied.
        if (preserve && refreshedScreen instanceof ServerListScrollAccess access) {
            access.qqutils$restoreScroll(scroll);
        }
    }

    @Override
    public void qqutils$restoreScroll(double scrollAmount) {
        if (serverSelectionList != null) serverSelectionList.setScrollAmount(scrollAmount);
    }
}
