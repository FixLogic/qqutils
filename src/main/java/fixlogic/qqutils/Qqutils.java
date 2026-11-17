package fixlogic.qqutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import fixlogic.qqutils.config.ConfigStore;
import fixlogic.qqutils.module.ModuleManager;
import fixlogic.qqutils.feature.locator.LocatorNamesModule;
import fixlogic.qqutils.feature.particles.ParticleGuardModule;
import fixlogic.qqutils.feature.servers.ServerRefreshModule;
import fixlogic.qqutils.command.ModuleCommands;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Qqutils implements ClientModInitializer {
	public static final String MOD_ID = "qqutils";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final ModuleManager MODULES = new ModuleManager();
	private static LocatorNamesModule locatorNames;
	private static ParticleGuardModule particleGuard;
	private static ServerRefreshModule serverRefresh;

	@Override
	public void onInitializeClient() {
		ConfigStore config = new ConfigStore(FabricLoader.getInstance().getConfigDir().resolve("qqutils.json"));
		config.load();
		locatorNames = MODULES.register(new LocatorNamesModule(config));
		particleGuard = MODULES.register(new ParticleGuardModule(config));
		serverRefresh = MODULES.register(new ServerRefreshModule(config));
		ModuleCommands.register(MODULES, config);
		LOGGER.info("Loaded {} client modules", MODULES.all().size());
	}

	public static LocatorNamesModule locatorNames() {
		return locatorNames;
	}

	public static ParticleGuardModule particleGuard() {
		return particleGuard;
	}

	public static ServerRefreshModule serverRefresh() {
		return serverRefresh;
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
