package dev.hugeblank.allium.util.asm;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record VisitedMethod(VisitedClass owner, int access, String name, String descriptor, String signature, String[] exceptions) implements VisitedMember {
    public List<Type> getParams() {
        return new ArrayList<>(Arrays.asList(Type.getArgumentTypes(descriptor)));
    }

    @Override
    public String unmappedMixinDescriptor() {
        return owner().getType().getDescriptor() + name + descriptor;
    }
}
