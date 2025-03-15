package dev.hugeblank.allium.util.asm;

import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public record VisitedMethod(VisitedClass owner, int access, String name, String descriptor, String signature, String[] exceptions) implements VisitedElement {
    public List<Type> getMixinParams() {

        List<Type> params = Arrays.asList(Type.getArgumentTypes(descriptor));
        params.add(Type.getType(CallbackInfo.class));
        return params;
    }
}
