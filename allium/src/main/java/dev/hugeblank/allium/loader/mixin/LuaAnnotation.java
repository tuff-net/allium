package dev.hugeblank.allium.loader.mixin;

import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.loader.type.InvalidArgumentException;
import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.util.asm.VisitedClass;
import me.basiqueevangelist.enhancedreflection.api.ClassType;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorRemappable;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.squiddev.cobalt.*;

import java.util.*;

public class LuaAnnotation implements Annotating {
    private static final Map<Class<?>, String> REMAP_KEYS = new HashMap<>();

    private final LuaState state;
    private final EClass<?> clazz;
    private final String name;
    private final List<Annotating> prototype = new ArrayList<>();

    public LuaAnnotation(LuaState state, @Nullable String name, LuaValue input, VisitedClass visitedClass, EClass<?> annotationClass) throws InvalidArgumentException, LuaError {
        this.state = state;
        this.clazz = annotationClass;
        this.name = name;
        if (input.type() == Constants.TTABLE) {
            LuaTable table = input.checkTable();
            for (EMethod method : annotationClass.declaredMethods()) {
                String key = method.name();
                LuaValue value = table.rawget(key);
                EClass<?> returnType = method.rawReturnType();
                EClass<?> arrayComponent = returnType.arrayComponent();
                if (arrayComponent != null && value != Constants.NIL) {
                    if (value.type() != Constants.TTABLE) {
                        LuaTable t = new LuaTable();
                        t.rawset(0, value);
                        value = t;
                    }
                    LuaTable checkedTable = value.checkTable();
                    List<Annotating> elements = new ArrayList<>();
                    for (int i = 0; i < checkedTable.size(); i++) {
                        String id = key+"["+ i + "]";
                        LuaValue tableElement = checkedTable.rawget(i);
                        if (arrayComponent.type() == ClassType.ANNOTATION) {
                            elements.add(new LuaAnnotation(state, id, tableElement, visitedClass, arrayComponent));
                        } else if (!value.isNil()) {
                            if (arrayComponent.raw().isEnum()) {
                                elements.add(new EnumElement(arrayComponent, id, tableElement.checkString()));
                            } else {
                                Object object = TypeCoercions.toJava(state, tableElement, arrayComponent);
                                elements.add(new Element(arrayComponent, id, object));
                            }
                        }
                    }
                    prototype.add(new ArrayElement(arrayComponent, key, elements));
                } else if (returnType.type() == ClassType.ANNOTATION) {
                    prototype.add(new LuaAnnotation(state, key, value, visitedClass, returnType));
                } else if (!value.isNil()) {
                    if (returnType.raw().isEnum()) {
                        prototype.add(new EnumElement(returnType, key, value.checkString()));
                    } else {
                        Object object = TypeCoercions.toJava(state, value, returnType);
                        prototype.add(new Element(returnType, key, object));
                    }
                } else if (method.raw().getDefaultValue() == null) {
                    throw new LuaError("Missing required element '" + key + "' while parsing " + annotationClass.name());
                }
            }
        } else {
            // TODO this is probably not working as intended
            Optional<EMethod> maybeTarget = annotationClass.methods().stream()
                    .filter(method -> method.name().equals("value"))
                    .findFirst();
            if (maybeTarget.isPresent()) {
                boolean remap = maybeTarget.get().name().equals(REMAP_KEYS.get(annotationClass.raw()));
                if (remap && input instanceof LuaString) {
                    ITargetSelectorRemappable mapped = MemberInfo.parse(input.checkString(), null);
                    Mappings mappings = ScriptRegistry.scriptFromState(state).getMappings();
                    boolean hasOwner  = mapped.getOwner() == null;
                    String mappedOwner = hasOwner ? visitedClass.mappedClassName() : mapped.getOwner();
                    String unmappedOwner = hasOwner ? null : mappings.getUnmapped(Mappings.asClass(mappedOwner)).get(0).replace(".", "/");
                    String unmappedName = mappings.getUnmapped(Mappings.asMethod(mappedOwner, mapped.getName())).get(0);
                    if (unmappedName != null) {
                        String[] split = unmappedName.split("#");
                        unmappedName = split.length == 2 ? split[1] : null;
                    }
                    final String mappedDesc = mapped.getDesc(), unmappedDesc;
                    if (mappedDesc == null) {
                        unmappedDesc = null;
                    } else if (mappedDesc.startsWith("(")) {
                        unmappedDesc = unmapMethodDescriptor(mappedDesc);
                    } else {
                        unmappedDesc = unmapFieldDescriptor(mappedDesc);
                    }
                    input = ValueFactory.valueOf(mapped
                            .remapUsing(new MappingMethod(unmappedOwner, unmappedName, unmappedDesc), true)
                            .toString()
                    );
                }
                // If the user supplies a value here that can't be coerced
                // into what the annotation is expecting, an explosion occurs.
                Object value = TypeCoercions.toJava(state, input, maybeTarget.get().rawReturnType());
                prototype.add(new Element(maybeTarget.get().rawReturnType(), "value", value));
            }
        }
    }

    public LuaAnnotation(LuaState state, LuaValue input, VisitedClass visitedClass, EClass<?> annotationClass) throws InvalidArgumentException, LuaError {
        this(state, null, input, visitedClass, annotationClass);
    }

    public String name() {
        return name;
    }

    @Override
    public EClass<?> type() {
        return clazz;
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

    @Override
    public void apply(AnnotationVisitor visitor) {
        prototype.forEach((value) -> {
            if (value instanceof LuaAnnotation annotation) {
                AnnotationVisitor nextVisitor = visitor.visitAnnotation(annotation.name(), value.type().raw().descriptorString());
                annotation.apply(nextVisitor);
                nextVisitor.visitEnd();
            } else {
                value.apply(visitor);
            }
        });
    }

    public LuaAnnotation findAnnotation(String key) {
        for (Annotating annotating : prototype) {
            if (annotating instanceof LuaAnnotation luaAnnotation && luaAnnotation.name().equals(key)) {
                return luaAnnotation;
            }
        }
        return null;
    }

    public <T> T findElement(String name, EClass<T> eClass) {
        for (Annotating annotating : prototype) {
            if (annotating.name().equals(name) && annotating.type().equals(eClass)) {
                if (annotating instanceof Element element) {
                    return eClass.cast(element.object());
                } else if (annotating instanceof ArrayElement arrayElement) {
                    if (arrayElement.objects().get(0) instanceof Element element) {
                        return eClass.cast(element.object());
                    }
                }
            }
        }
        return null;
    }

    public <T> T findElement(String name, Class<T> clazz) {
        return findElement(name, EClass.fromJava(clazz));
    }

    private record Element(EClass<?> type, String name, Object object) implements Annotating {

        @Override
        public void apply(AnnotationVisitor annotationVisitor) {
            annotationVisitor.visit(name, object);
        }
    }

    private record EnumElement(EClass<?> type, String name, String value) implements Annotating {

        @Override
        public void apply(AnnotationVisitor annotationVisitor) {
            annotationVisitor.visitEnum(name, type.raw().descriptorString(), value);
        }
    }

    private record ArrayElement(EClass<?> type, String name, List<Annotating> objects) implements Annotating {

        @Override
        public void apply(AnnotationVisitor annotationVisitor) {
            AnnotationVisitor visitor = annotationVisitor.visitArray(name);
            for (Annotating object : objects) {
                if (object instanceof LuaAnnotation annotation) {
                    AnnotationVisitor nextVisitor = visitor.visitAnnotation(annotation.name(), object.type().raw().descriptorString());
                    annotation.apply(nextVisitor);
                    nextVisitor.visitEnd();
                } else {
                    object.apply(visitor);
                }
            }
            visitor.visitEnd();
        }
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
