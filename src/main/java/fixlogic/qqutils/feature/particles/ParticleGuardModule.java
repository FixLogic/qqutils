package fixlogic.qqutils.feature.particles;

import fixlogic.qqutils.chat.ClientMessages;
import fixlogic.qqutils.config.ConfigStore;
import fixlogic.qqutils.module.ClientModule;


public final class ParticleGuardModule extends ClientModule {
    private final ConfigStore config;

    public ParticleGuardModule(ConfigStore config) {
        super("particle_guard", config);
        this.config = config;
    }

    public boolean shouldBlock(int count) {
        return isEnabled() && count > config.maxParticlesPerPacket();
    }

    public void notifyBlocked(int count) {
        ClientMessages.error("qqutils.particle_guard.blocked", count, config.maxParticlesPerPacket());
    }
}
