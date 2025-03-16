package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.util.asm.VisitedClass;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.squiddev.cobalt.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

// A masterpiece
public class MixinAnnotator {

    private static final Map<Class<?>, String> REMAP_KEYS = new HashMap<>();

    private final VisitedClass visitedClass;
    private final LuaState state;

    public MixinAnnotator(LuaState state, VisitedClass visitedClass) {
        this.state = state;
        this.visitedClass = visitedClass;
    }

    private void annotate(LuaValue value, AnnotationVisitor visitor, EClass<?> clazz) throws LuaError, InvalidArgumentException {
        LuaTable table = value.checkTable();
        for (EMethod method : clazz.methods()) {
            String name = method.name();
            LuaValue nextValue = table.rawget(name);
            EClass<?> returnType = method.rawReturnType();
            boolean remap = name.equals(REMAP_KEYS.get(clazz.raw()));
            if (!nextValue.isNil()) {
                if (returnType.raw().isArray()) {
                    annotateArray(visitor, remap, name, nextValue, returnType);
                } else {
                    visitValue(visitor, remap, name, nextValue, returnType);
                }
            } else if (name.equals("value") && !table.rawget(1).isNil()) {
                if (returnType.raw().isArray()) {
                    annotateArray(visitor, remap, name, table.rawget(1), returnType);
                } else {
                    visitValue(visitor, remap, "value", table.rawget(1), returnType);
                }
            }
        }
    }

    private void annotateArray(AnnotationVisitor visitor, boolean remap, String name, LuaValue nextValue, EClass<?> returnType) throws LuaError, InvalidArgumentException {
        AnnotationVisitor array = visitor.visitArray(name);
        if (nextValue instanceof LuaString || (nextValue instanceof LuaTable table && table.length() == 0)) {
            visitValue(array, remap, null, nextValue, returnType.arrayComponent());
        } else {
            LuaTable nextTable = nextValue.checkTable();
            for (int i = 0; i < nextTable.length(); i++) {
                visitValue(array, remap, null, nextTable.rawget(i + 1), returnType.arrayComponent());
            }
        }
        array.visitEnd();
    }

    private void visitValue(AnnotationVisitor visitor, boolean remap, String name, LuaValue value, EClass<?> returnType) throws LuaError, InvalidArgumentException {
        if (!value.isNil()) {
            if (remap && value instanceof LuaString) {
                ITargetSelectorRemappable mapped = MemberInfo.parse(value.checkString(), null);
                Mappings mappings = ScriptRegistry.scriptFromState(state).getMappings();
                String mappedOwner = mapped.getOwner() == null ? visitedClass.mappedClassName() : mapped.getOwner();
                String unmappedOwner = mapped.getOwner() == null ? null : mappings.getUnmapped(Mappings.asClass(mappedOwner)).get(0).replace(".", "/");
                String unmappedName = mappings.getUnmapped(Mappings.asMethod(mappedOwner, mapped.getName())).get(0);
                if (unmappedName != null) {
                    String[] split = unmappedName.split("#");
                    unmappedName = split.length == 2 ? split[1] : null;
                }
                final String unmappedDesc;
                if (mapped.getDesc() == null) {
                    unmappedDesc = null;
                } else if (mapped.getDesc().startsWith("(")) {
                    unmappedDesc = unmapMethodDescriptor(mapped.getDesc());
                } else {
                    unmappedDesc = unmapFieldDescriptor(mapped.getDesc());
                }
                mapped = mapped.remapUsing(new MappingMethod(unmappedOwner, unmappedName, unmappedDesc), true);
                value = ValueFactory.valueOf(mapped.toString());
            }
            if (returnType.raw().isAnnotation()) {
                AnnotationVisitor nextVisitor = visitor.visitAnnotation(name, returnType.raw().descriptorString());
                if (value instanceof LuaString) {
                    LuaTable nextTable = new LuaTable();
                    nextTable.rawset("value", value);
                    annotate(nextTable, nextVisitor, returnType);
                } else {
                    annotate(value, nextVisitor, returnType);
                }
                nextVisitor.visitEnd();
            } else if (returnType.raw().isEnum()) {
                visitor.visitEnum(name, returnType.raw().descriptorString(), value.checkString());
            } else {
                visitor.visit(name, TypeCoercions.toJava(state, value, returnType));
            }
        }
    }

    // I'm a hater of how ClassVisitor, MethodVisitor, FieldVisitor, etc. aren't all under a common interface.
    public AnnotationVisitor attachAnnotation(MethodVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    public AnnotationVisitor attachAnnotation(FieldVisitor visitor, Class<?> annotation) {
        EClass<?> eAnnotation = EClass.fromJava(annotation);
        return visitor.visitAnnotation(
                annotation.descriptorString(),
                !eAnnotation.hasAnnotation(Retention.class) ||
                        eAnnotation.annotation(Retention.class).value().equals(RetentionPolicy.RUNTIME)
        );
    }

    public LuaAnnotation annotateMethod(LuaTable annotationTable, MethodVisitor methodVisitor, EClass<?> annotation) throws InvalidArgumentException, LuaError {
        if (annotation.raw().isAnnotation()) {
            AnnotationVisitor visitor = attachAnnotation(methodVisitor, annotation.raw());
            LuaAnnotation luaAnnotation = new LuaAnnotation(state, annotationTable, visitedClass, annotation);
            luaAnnotation.apply(visitor);
            visitor.visitEnd();
            return luaAnnotation;
        }
        throw new InvalidArgumentException("Class must be an annotation");
    }

    private void unmapTypeArg(StringBuilder unmappedDescriptor, Type arg) throws LuaError {
        if (arg.getSort() == Type.OBJECT) {
            Mappings mappings = ScriptRegistry.scriptFromState(state).getMappings();
            unmappedDescriptor
                    .append("L")
                    .append(mappings.getUnmapped(Mappings.asClass(arg.getInternalName())).get(0).replace(".", "/"))
                    .append(";");
        } else {
            unmappedDescriptor.append(arg.getInternalName());
        }
    }

    private String unmapFieldDescriptor(String descriptor) throws LuaError {
        StringBuilder builder = new StringBuilder();
        unmapTypeArg(builder, Type.getType(descriptor));
        return builder.toString();
    }

    private String unmapMethodDescriptor(String descriptor) throws LuaError {
        StringBuilder unmappedDescriptor = new StringBuilder("(");
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            unmapTypeArg(unmappedDescriptor, arg);
        }
        unmappedDescriptor.append(")");
        unmapTypeArg(unmappedDescriptor, Type.getReturnType(descriptor));
        return unmappedDescriptor.toString();
    }

    static {
        REMAP_KEYS.put(At.class, "target");

        REMAP_KEYS.put(Inject.class, "method");
        REMAP_KEYS.put(Redirect.class, "method");
        REMAP_KEYS.put(Overwrite.class, "method");
        REMAP_KEYS.put(ModifyArg.class, "method");
        REMAP_KEYS.put(ModifyArgs.class, "method");
        REMAP_KEYS.put(ModifyVariable.class, "method");


        REMAP_KEYS.put(Accessor.class, "value");
        REMAP_KEYS.put(Invoker.class, "value");
    }

}
