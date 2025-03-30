package dev.hugeblank.allium.api;

import dev.hugeblank.allium.util.Identifiable;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public interface MappingsLoader extends Identifiable {
    void load(MappingVisitor visitor);
}
