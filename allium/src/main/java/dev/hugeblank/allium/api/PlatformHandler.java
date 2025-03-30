package dev.hugeblank.allium.api;

import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.Registry;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

import java.io.IOException;

public abstract class PlatformHandler implements Identifiable {
    private static final Registry<TreeReference> REGISTRY = new Registry<>();

    public final VisitableMappingTree of() throws IOException {
        if (!REGISTRY.has(getID())) REGISTRY.register(new TreeReference(getID(), getHandler()));
        return REGISTRY.get(getID()).tree();
    }

    protected abstract VisitableMappingTree getHandler() throws IOException;

    public abstract String getSourceNS();

    public abstract String getDestNS();

    record TreeReference(String getID, VisitableMappingTree tree) implements Identifiable {}
}
