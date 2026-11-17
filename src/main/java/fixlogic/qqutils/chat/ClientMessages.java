package fixlogic.qqutils.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Shared local chat notifications. Call on the client thread. */
public final class ClientMessages {
    private ClientMessages() { }

    public static MutableComponent prefixed(String key, Object... arguments) {
        return Component.translatable("qqutils.chat.prefix", Component.translatable(key, arguments));
    }

    public static void error(String key, Object... arguments) {
        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(
            prefixed(key, arguments).withStyle(ChatFormatting.RED));
    }
}
