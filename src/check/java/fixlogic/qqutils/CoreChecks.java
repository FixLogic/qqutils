package fixlogic.qqutils;

import fixlogic.qqutils.config.ConfigStore;
import fixlogic.qqutils.feature.particles.ParticleGuardModule;
import fixlogic.qqutils.feature.servers.ServerRefreshModule;
import fixlogic.qqutils.gui.WidgetHitTest;
import net.minecraft.client.gui.components.Button;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import fixlogic.qqutils.hud.LabelLayout;
import fixlogic.qqutils.module.ClientModule;
import fixlogic.qqutils.module.ModuleManager;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

/** Run by Gradle check, without requiring another test library download. */
public final class CoreChecks {
    public static void main(String[] args) throws Exception {
        checkLayout();
        checkConfigAndModules();
        checkParticleGuard();
        checkServerRefresh();
        checkDisabledButtonHitTest();
        System.out.println("Core checks passed: layout, config persistence, validation, module registry, particle guard, server refresh");
    }

    private static void checkLayout() {
        var layout = new LabelLayout();
        require(layout.place(0, 30, 100, 2).x() == 0, "Left edge must stay on screen");
        require(layout.place(100, 30, 100, 2).x() == 70, "Right edge must stay on screen");
        require(layout.place(0, 30, 100, 2).row() == 1, "Overlapping names need a new row");
        require(layout.place(0, 30, 100, 2) == null, "Full layout must not overlap");
        require(new LabelLayout().place(50, 30, 100, 0) == null, "No vertical room");
        var random = new Random(42);
        for (int screen : new int[] {160, 320, 854}) {
            var crowded = new LabelLayout();
            var placed = new ArrayList<int[]>();
            for (int i = 0; i < 200; i++) {
                int width = random.nextInt(10, 100);
                var position = crowded.place(random.nextInt(screen), width, screen, 10);
                if (position == null) continue;
                require(position.x() >= 0 && position.x() + width <= screen, "Label outside viewport");
                for (var previous : placed) {
                    if (position.row() == previous[0]) {
                        require(position.x() >= previous[2] + 3 || position.x() + width + 3 <= previous[1],
                            "Labels in the same row overlap");
                    }
                }
                placed.add(new int[] {position.row(), position.x(), position.x() + width});
            }
        }
    }

    private static void checkConfigAndModules() throws Exception {
        var directory = Files.createTempDirectory("qqutils-check-");
        var path = directory.resolve("qqutils.json");
        try {
            var config = new ConfigStore(path);
            require(config.load(), "First launch must create settings");
            var modules = new ModuleManager();
            var module = modules.register(new ClientModule("locator_names", config) { });
            require(module.isEnabled(), "Feature enabled by default");
            module.setEnabled(false);
            require(config.save(), "Save module state");
            var restored = new ConfigStore(path);
            require(restored.load() && !restored.isEnabled("locator_names"), "State survives restart");
            require(config.lockServerRefreshWhilePinging(), "Refresh lock enabled for existing configs");
            config.setLockServerRefreshWhilePinging(false);
            require(config.save() && restored.load() && !restored.lockServerRefreshWhilePinging(),
                "Refresh lock preference survives restart");
            Files.writeString(path, "{\"modules\":null,\"locatorNames\":{\"scale\":99,\"gap\":-10}}");
            require(restored.load(), "Missing sections are accepted");
            require(restored.lockServerRefreshWhilePinging(), "Old configs use the default refresh lock");
            require(restored.locatorNames().scale == 1.5f && restored.locatorNames().gap == 0,
                "Unsafe config values are clamped");
            Files.writeString(path, "{\"locatorNames\":null}");
            require(restored.load() && restored.locatorNames().scale == 0.75f, "Null section uses defaults");
            module.setEnabled(false);
            Files.writeString(path, "{broken");
            require(!config.load() && !module.isEnabled(), "Invalid reload retains current state");
            require(Files.readString(path).equals("{broken"), "Invalid file is preserved");
            try {
                modules.register(module);
                throw new AssertionError("Duplicate module accepted");
            } catch (IllegalArgumentException expected) { }
            require(modules.find("locator_names") == module && modules.find("missing") == null, "Registry lookup");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (var entry : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(entry);
            }
        }
    }

    private static void checkParticleGuard() throws Exception {
        var directory = Files.createTempDirectory("qqutils-particles-check-");
        var path = directory.resolve("qqutils.json");
        try {
            var config = new ConfigStore(path);
            var guard = new ParticleGuardModule(config);
            require(config.load(), "Create particle settings");
            require(guard.isEnabled() && config.maxParticlesPerPacket() == 1000, "Protection defaults");
            require(!guard.shouldBlock(0), "Zero count is vanilla's single-particle mode");
            require(!guard.shouldBlock(-1), "Negative count does not spawn a vanilla loop");
            require(!guard.shouldBlock(1000), "Count equal to limit must pass");
            require(guard.shouldBlock(1001) && guard.shouldBlock(Integer.MAX_VALUE), "Excess counts must be blocked");
            config.setMaxParticlesPerPacket(25);
            require(!guard.shouldBlock(25) && guard.shouldBlock(26), "Changed limit applies immediately");
            guard.setEnabled(false);
            require(!guard.shouldBlock(Integer.MAX_VALUE), "Disabled protection allows packets");
            require(config.save(), "Save particle configuration");
            var reloaded = new ConfigStore(path);
            require(reloaded.load() && reloaded.maxParticlesPerPacket() == 25
                && !reloaded.isEnabled("particle_guard"), "Particle settings survive restart");
            Files.writeString(path, "{\"modules\":{\"locator_names\":false},\"locatorNames\":{\"scale\":1}}");
            require(config.load() && guard.isEnabled() && config.maxParticlesPerPacket() == 1000,
                "Old config gains protection without resetting old module settings");
            require(!config.isEnabled("locator_names"), "Existing feature toggle preserved");
            Files.writeString(path, "{\"particleGuard\":{\"maxParticlesPerPacket\":-5}}");
            require(config.load() && config.maxParticlesPerPacket() == 1, "Invalid limit normalized");
            require(!guard.shouldBlock(1) && guard.shouldBlock(2), "Minimum limit boundary");
            Files.writeString(path, "{\"particleGuard\":null}");
            require(config.load() && config.maxParticlesPerPacket() == 1000, "Null settings fall back to default");
        } finally {
            Files.deleteIfExists(path);
            Files.delete(directory);
        }
    }

    private static void checkServerRefresh() {
        SharedConstants.tryDetectVersion();
        var config = new ConfigStore(java.nio.file.Path.of("unused-core-check-config.json"));
        var module = new ServerRefreshModule(config);
        var selected = new ServerData("First", "localhost:25565", ServerData.Type.OTHER);
        var neighbor = new ServerData("Second", "localhost:25566", ServerData.Type.OTHER);
        neighbor.setState(ServerData.State.SUCCESSFUL);
        neighbor.ping = 42;
        neighbor.motd = Component.literal("Keep this status");

        require(module.isEnabled(), "Server refresh enabled by default");
        require(!module.showsButton() && !module.canRefresh(neighbor), "Per-server button hidden by default");
        config.setShowServerRefreshButton(true);
        require(module.showsButton(), "Per-server button can be enabled separately");
        require(!module.refresh(selected), "No duplicate request before initial ping");
        selected.setState(ServerData.State.PINGING);
        require(!module.refresh(selected), "No duplicate request while pinging");
        for (var completed : new ServerData.State[] {ServerData.State.SUCCESSFUL,
                ServerData.State.INCOMPATIBLE, ServerData.State.UNREACHABLE}) {
            selected.setState(completed);
            selected.ping = 100;
            selected.status = Component.literal("Old status");
            selected.motd = Component.literal("Old description");
            selected.playerList = java.util.List.of(Component.literal("Old player"));
            require(module.refresh(selected), "Finished/failed server can be refreshed");
            require(selected.state() == ServerData.State.INITIAL && selected.ping == -1
                && selected.status.getString().isEmpty() && selected.motd.getString().isEmpty()
                && selected.playerList.isEmpty(), "Clear stale results and schedule vanilla ping");
            require(!module.refresh(selected), "Repeated click does not queue another ping");
        }
        require(neighbor.state() == ServerData.State.SUCCESSFUL && neighbor.ping == 42
            && neighbor.motd.getString().equals("Keep this status"), "Other servers remain untouched");
        require(selected.name.equals("First") && selected.ip.equals("localhost:25565"),
            "Saved server identity preserved");
        config.setLockServerRefreshWhilePinging(false);
        require(module.canRefresh(selected) && module.refresh(selected), "Unlocked button accepts clicks before ping");
        selected.setState(ServerData.State.PINGING);
        require(module.canRefresh(selected) && module.refresh(selected) && module.refresh(selected),
            "Unlocked button accepts repeated clicks during ping");
        require(selected.state() == ServerData.State.PINGING && !module.startPendingRefresh(selected),
            "Repeated clicks do not race an in-flight ping");
        selected.setState(ServerData.State.UNREACHABLE);
        require(module.startPendingRefresh(selected) && selected.state() == ServerData.State.INITIAL,
            "Queued refresh starts when the previous request finishes");
        selected.setState(ServerData.State.SUCCESSFUL);
        require(!module.startPendingRefresh(selected), "Rapid clicks coalesce into one follow-up");
        selected.setState(ServerData.State.PINGING);
        require(module.refresh(selected), "Queue another refresh");
        module.setEnabled(false);
        selected.setState(ServerData.State.SUCCESSFUL);
        require(!module.startPendingRefresh(selected), "Disable clears pending refreshes");
        require(!module.refresh(selected) && selected.state() == ServerData.State.SUCCESSFUL,
            "Disabled module cannot refresh");
        module.setEnabled(true);
        require(!module.startPendingRefresh(selected), "No stale pending refresh after re-enabling");
    }

    private static void checkDisabledButtonHitTest() {
        var button = Button.builder(Component.literal("Refresh"), ignored -> { })
            .bounds(100, 50, 14, 14).build();
        require(WidgetHitTest.contains(button, 107, 57), "First click belongs to refresh");
        button.active = false;
        require(!button.isMouseOver(107, 57), "Vanilla hit test excludes disabled buttons");
        require(WidgetHitTest.contains(button, 107, 57), "Second click still belongs to disabled refresh");
        require(WidgetHitTest.contains(button, 100, 50), "Top-left edge is included");
        require(!WidgetHitTest.contains(button, 114, 57)
            && !WidgetHitTest.contains(button, 107, 64)
            && !WidgetHitTest.contains(button, 99, 57), "Clicks outside keep normal row behavior");
        button.visible = false;
        require(!WidgetHitTest.contains(button, 107, 57), "Hidden controls do not consume row clicks");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
