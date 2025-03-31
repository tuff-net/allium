package dev.hugeblank.allium.mappings;

import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.api.PlatformHandler;
import dev.hugeblank.allium.util.Registry;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;

public class PlatformMappings {
    private static final Registry<PlatformHandler> HANDLERS = new Registry<>();
    private static final Registry<MappingsLoader> LOADERS = new Registry<>();

    public static Mappings of(String mapping) {
        try {
            return HANDLERS.get(mapping).getOrCreate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasLoader(String id) {
        return LOADERS.has(id);
    }

    public static MappingsLoader getLoader(String id) {
        return LOADERS.get(id);
    }

    public static void registerHandler(PlatformHandler platformMapping) {
        HANDLERS.register(platformMapping);
    }

    public static void registerLoader(MappingsLoader mappingsLoader) {
        LOADERS.register(mappingsLoader);
    }
}

