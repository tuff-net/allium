package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.mappings.NoSuchMappingException;
import net.fabricmc.mappingio.tree.MappingTree;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public interface VisitedMember {
    VisitedClass owner();
    int access();
    String name();
    String descriptor();
    String signature();

    String unmappedMixinDescriptor();

    default boolean needsInstance() {
        return (access() & ACC_STATIC) == 0;
    }

    default String mappedName(LuaState state) throws LuaError {
        Mappings mappings = ScriptRegistry.scriptFromState(state).getMappings();
        if (!Allium.DEVELOPMENT) {
            try {
                MappingTree.ClassMapping classMapping = mappings.toMappedClass(owner().name());
                return mappings.toMappedMemberName(classMapping, name(), descriptor());
            } catch (NoSuchMappingException ignored) {}
        }
        return name();
    }
}
