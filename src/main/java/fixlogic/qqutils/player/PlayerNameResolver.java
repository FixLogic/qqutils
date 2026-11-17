package fixlogic.qqutils.player;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;

/** Uses the player list, so names also resolve outside entity render distance. */
public final class PlayerNameResolver {
    private PlayerNameResolver() { }

    public static Optional<String> resolve(Minecraft client, UUID uuid) {
        var connection = client.getConnection();
        if (connection == null) return Optional.empty();
        var player = connection.getPlayerInfo(uuid);
        return player == null ? Optional.empty() : Optional.of(player.getProfile().name());
    }
}
