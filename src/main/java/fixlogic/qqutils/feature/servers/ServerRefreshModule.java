package fixlogic.qqutils.feature.servers;

import fixlogic.qqutils.config.ConfigStore;
import fixlogic.qqutils.module.ClientModule;
import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;

/** Reuses vanilla's per-entry ping lifecycle without rebuilding the server list. */
public final class ServerRefreshModule extends ClientModule {
    private final ConfigStore config;
    private final Set<ServerData> pending = Collections.newSetFromMap(new WeakHashMap<>());

    public ServerRefreshModule(ConfigStore config) {
        super("server_refresh", config);
        this.config = config;
    }

    public boolean showsButton() {
        return isEnabled() && config.showServerRefreshButton();
    }

    public boolean canRefresh(ServerData server) {
        return showsButton() && (!config.lockServerRefreshWhilePinging() || !isPinging(server));
    }

    /** Client thread only. The entry starts its normal asynchronous ping on its next render. */
    public boolean refresh(ServerData server) {
        if (!canRefresh(server)) return false;
        if (isPinging(server)) {
            // Keep the button usable without racing two pings writing to the same server data.
            // Multiple clicks during one request coalesce into one follow-up refresh.
            pending.add(server);
            return true;
        }
        pending.remove(server);
        reset(server);
        return true;
    }

    public boolean startPendingRefresh(ServerData server) {
        if (!showsButton()) {
            pending.remove(server);
            return false;
        }
        if (isPinging(server) || !pending.remove(server)) return false;
        reset(server);
        return true;
    }

    private static boolean isPinging(ServerData server) {
        return server.state() == ServerData.State.INITIAL || server.state() == ServerData.State.PINGING;
    }

    private static void reset(ServerData server) {
        server.ping = -1;
        server.players = null;
        server.playerList = List.of();
        server.motd = CommonComponents.EMPTY;
        server.status = CommonComponents.EMPTY;
        server.setState(ServerData.State.INITIAL);
    }
}
