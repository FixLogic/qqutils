package fixlogic.qqutils.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import fixlogic.qqutils.chat.ClientMessages;
import fixlogic.qqutils.config.ConfigStore;
import fixlogic.qqutils.module.ClientModule;
import fixlogic.qqutils.module.ModuleManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class ModuleCommands {
    private ModuleCommands() { }

    public static void register(ModuleManager modules, ConfigStore config) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("qqutils")
                .executes(context -> {
                    for (var module : modules.all()) {
                        context.getSource().sendFeedback(moduleStatus(module));
                    }
                    return 1;
                })
                .then(literal("toggle").then(argument("module", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        modules.all().stream().map(module -> module.id())
                            .filter(id -> id.startsWith(builder.getRemaining())).forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(context -> {
                        var id = StringArgumentType.getString(context, "module");
                        var module = modules.find(id);
                        if (module == null) {
                            context.getSource().sendError(ClientMessages.prefixed("qqutils.command.unknown_module", id));
                            return 0;
                        }
                        module.setEnabled(!module.isEnabled());
                        context.getSource().sendFeedback(moduleStatus(module));
                        if (!config.save()) context.getSource().sendError(ClientMessages.prefixed("qqutils.config.save_failed"));
                        return 1;
                    })))
                .then(literal("config").then(literal("particle_limit")
                    .executes(context -> {
                        context.getSource().sendFeedback(ClientMessages.prefixed(
                            "qqutils.config.particle_limit", config.maxParticlesPerPacket()));
                        return 1;
                    })
                    .then(argument("maximum", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            config.setMaxParticlesPerPacket(IntegerArgumentType.getInteger(context, "maximum"));
                            context.getSource().sendFeedback(ClientMessages.prefixed(
                                "qqutils.config.particle_limit", config.maxParticlesPerPacket()));
                            if (!config.save()) context.getSource().sendError(ClientMessages.prefixed("qqutils.config.save_failed"));
                            return 1;
                        })))
                    .then(literal("server_refresh_lock")
                        .executes(context -> {
                            context.getSource().sendFeedback(ClientMessages.prefixed(
                                "qqutils.config.server_refresh_lock", state(config.lockServerRefreshWhilePinging())));
                            return 1;
                        })
                        .then(argument("enabled", BoolArgumentType.bool()).executes(context -> {
                            config.setLockServerRefreshWhilePinging(BoolArgumentType.getBool(context, "enabled"));
                            context.getSource().sendFeedback(ClientMessages.prefixed(
                                "qqutils.config.server_refresh_lock", state(config.lockServerRefreshWhilePinging())));
                            if (!config.save()) context.getSource().sendError(ClientMessages.prefixed("qqutils.config.save_failed"));
                            return 1;
                        })))
                    .then(literal("server_refresh_button")
                        .executes(context -> {
                            context.getSource().sendFeedback(ClientMessages.prefixed(
                                "qqutils.config.server_refresh_button", state(config.showServerRefreshButton())));
                            return 1;
                        })
                        .then(argument("visible", BoolArgumentType.bool()).executes(context -> {
                            config.setShowServerRefreshButton(BoolArgumentType.getBool(context, "visible"));
                            context.getSource().sendFeedback(ClientMessages.prefixed(
                                "qqutils.config.server_refresh_button", state(config.showServerRefreshButton())));
                            if (!config.save()) context.getSource().sendError(ClientMessages.prefixed("qqutils.config.save_failed"));
                            return 1;
                        }))))
                .then(literal("reload").executes(context -> {
                    if (!config.load()) {
                        context.getSource().sendError(ClientMessages.prefixed("qqutils.config.load_failed"));
                        return 0;
                    }
                    context.getSource().sendFeedback(ClientMessages.prefixed("qqutils.config.reloaded"));
                    return 1;
                }))));
    }

    private static Component moduleStatus(ClientModule module) {
        var name = Component.translatableWithFallback("qqutils.module." + module.id(), module.id());
        return ClientMessages.prefixed("qqutils.command.module_status", name, module.id(), state(module.isEnabled()));
    }

    private static Component state(boolean enabled) {
        return Component.translatable(enabled ? "qqutils.state.enabled" : "qqutils.state.disabled");
    }
}
