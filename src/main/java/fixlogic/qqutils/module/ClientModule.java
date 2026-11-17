package fixlogic.qqutils.module;

import fixlogic.qqutils.config.ConfigStore;

/** Base for independently switchable client features. */
public abstract class ClientModule {
    private final String id;
    private final ConfigStore config;

    protected ClientModule(String id, ConfigStore config) {
        this.id = id;
        this.config = config;
    }

    public final String id() {
        return id;
    }

    public final boolean isEnabled() {
        return config.isEnabled(id);
    }

    public final void setEnabled(boolean enabled) {
        config.setEnabled(id, enabled);
    }
}
