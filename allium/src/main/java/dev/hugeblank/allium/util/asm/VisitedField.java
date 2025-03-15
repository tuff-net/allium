package dev.hugeblank.allium.util.asm;

import static org.objectweb.asm.Opcodes.ACC_STATIC;

public record VisitedField(VisitedClass owner, int access, String name, String descriptor, String signature, Object value) implements VisitedElement {
    public boolean isStatic() {
        return (access() & ACC_STATIC) != 0;
    }
}
