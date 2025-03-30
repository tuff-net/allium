package dev.hugeblank.allium.mappings;

import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.api.PlatformHandler;
import dev.hugeblank.allium.util.Registry;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;
import java.util.MissingResourceException;

public class PlatformMappings {
    private static final Registry<PlatformHandler> HANDLERS = new Registry<>();
    private static final Registry<MappingsLoader> LOADERS = new Registry<>();

    public static VisitableMappingTree of(String mapping) {
        try {
            return HANDLERS.get(mapping).of();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MappingsLoader getLoader(String id) {
        if (!LOADERS.has(id)) throw new MissingResourceException("No such mappings loader", MappingsLoader.class.getSimpleName(), id);
        return LOADERS.get(id);
    }

    public static void registerHandler(PlatformHandler platformMapping) {
        HANDLERS.register(platformMapping);
    }

    public static void registerLoader(MappingsLoader mappingsLoader) {
        LOADERS.register(mappingsLoader);
    }
}

