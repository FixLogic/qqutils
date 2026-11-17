package fixlogic.qqutils.mixin;

import fixlogic.qqutils.Qqutils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ParticlePacketMixin {
    // The first getCount is AFTER ensureRunningOnSameThread and BEFORE any particle is spawned.
    // This avoids accessing chat on the network thread or notifying twice for a scheduled packet.
    @Inject(method = "handleParticleEvent", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;getCount()I",
        ordinal = 0), cancellable = true)
    private void qqutils$filterParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        var guard = Qqutils.particleGuard();
        if (guard.shouldBlock(packet.getCount())) {
            ci.cancel();
            guard.notifyBlocked(packet.getCount());
        }
    }
}
