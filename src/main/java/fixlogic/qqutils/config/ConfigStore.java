package fixlogic.qqutils.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fixlogic.qqutils.Qqutils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared persistent settings; feature-specific settings live in their own sections. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private Settings settings = new Settings();

    public ConfigStore(Path path) {
        this.path = path;
    }

    public boolean load() {
        if (!Files.exists(path)) {
            return save();
        }
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Settings loaded = GSON.fromJson(reader, Settings.class);
            if (loaded == null) {
                throw new IllegalArgumentException("Empty config");
            }
            if (loaded.modules == null) loaded.modules = new LinkedHashMap<>();
            if (loaded.locatorNames == null) loaded.locatorNames = new LocatorNamesSettings();
            loaded.locatorNames.normalize();
            if (loaded.particleGuard == null) loaded.particleGuard = new ParticleGuardSettings();
            loaded.particleGuard.normalize();
            if (loaded.serverRefresh == null) loaded.serverRefresh = new ServerRefreshSettings();
            settings = loaded;
            return true;
        } catch (IOException | RuntimeException exception) {
            // Preserve the original file so a typo does not destroy the user's settings.
            Qqutils.LOGGER.error("Cannot read {}; retaining current settings", path, exception);
            return false;
        }
    }

    public boolean save() {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(settings) + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            Qqutils.LOGGER.error("Cannot save {}", path, exception);
            return false;
        }
    }

    public boolean isEnabled(String id) {
        return !Boolean.FALSE.equals(settings.modules.get(id));
    }

    public void setEnabled(String id, boolean enabled) {
        settings.modules.put(id, enabled);
    }

    public LocatorNamesSettings locatorNames() {
        return settings.locatorNames;
    }

    public int maxParticlesPerPacket() {
        return settings.particleGuard.maxParticlesPerPacket;
    }

    public void setMaxParticlesPerPacket(int maximum) {
        settings.particleGuard.maxParticlesPerPacket = Math.max(1, maximum);
    }

    public boolean showServerRefreshButton() {
        return settings.serverRefresh.showButton;
    }

    public void setShowServerRefreshButton(boolean show) {
        settings.serverRefresh.showButton = show;
    }

    public boolean lockServerRefreshWhilePinging() {
        return settings.serverRefresh.lockWhilePinging;
    }

    public void setLockServerRefreshWhilePinging(boolean lock) {
        settings.serverRefresh.lockWhilePinging = lock;
    }

    private static final class Settings {
        Map<String, Boolean> modules = new LinkedHashMap<>(Map.of(
            "locator_names", true, "particle_guard", true, "server_refresh", true));
        LocatorNamesSettings locatorNames = new LocatorNamesSettings();
        ParticleGuardSettings particleGuard = new ParticleGuardSettings();
        ServerRefreshSettings serverRefresh = new ServerRefreshSettings();
    }

    private static final class ServerRefreshSettings {
        boolean showButton = false;
        boolean lockWhilePinging = true;
    }

    private static final class ParticleGuardSettings {
        int maxParticlesPerPacket = 1000;

        void normalize() {
            maxParticlesPerPacket = Math.max(1, maxParticlesPerPacket);
        }
    }

    public static final class LocatorNamesSettings {
        public float scale = 0.75f;
        public int gap = 3;
        public boolean background = true;

        private void normalize() {
            scale = Float.isFinite(scale) ? Math.clamp(scale, 0.5f, 1.5f) : 0.75f;
            gap = Math.clamp(gap, 0, 20);
        }
    }
}
