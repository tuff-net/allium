package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
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
        return ScriptRegistry.scriptFromState(state).getMappings().getMapped(
                Mappings.asMethod(owner().name(), name())
        ).split("#")[1];
    }
}
