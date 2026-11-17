package fixlogic.qqutils.module;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleManager {
    private final Map<String, ClientModule> modules = new LinkedHashMap<>();

    public <T extends ClientModule> T register(T module) {
        if (modules.putIfAbsent(module.id(), module) != null) {
            throw new IllegalArgumentException("Duplicate module: " + module.id());
        }
        return module;
    }

    public ClientModule find(String id) {
        return modules.get(id);
    }

    public Collection<ClientModule> all() {
        return Collections.unmodifiableCollection(modules.values());
    }
}
