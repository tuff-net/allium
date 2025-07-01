package dev.hugeblank.allium.util.asm;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.mappings.NoSuchMappingException;
import net.fabricmc.mappingio.tree.MappingTree;
import org.objectweb.asm.*;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;

import java.io.IOException;
import java.util.*;

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;

public class VisitedClass {
    private static final Map<String, VisitedClass> VISITED = new HashMap<>();

    private final Map<String, VisitedField> visitedFields = new HashMap<>();
    private final Map<String, VisitedMethod> visitedMethods = new HashMap<>();

    private final TargetClassVisitor.MappingsPair mappingsPair;
    private final int version;
    private final int access;
    private final String signature;
    private final String superName;
    private final String[] interfaces;
    private final List<VisitedClass> inheritance = new ArrayList<>();

    public VisitedClass(TargetClassVisitor.MappingsPair mappingsPair, int version, int access, String signature, String superName, String[] interfaces) {
        this.mappingsPair = mappingsPair;
        this.version = version;
        this.access = access;
        this.signature = signature;
        this.superName = superName;
        this.interfaces = interfaces;
        VISITED.put(mappingsPair.mapped(), this);
    }

    public void addInheritance(VisitedClass visitedClass) {
        inheritance.add(visitedClass);
    }

    public Type getType() {
        return Type.getType("L"+mappingsPair.unmappedPath()+";");
    }


    public Optional<VisitedMethod> getMethod(String name) {
        Optional<VisitedMember> member = get(name);
        if (member.isPresent() && member.get() instanceof VisitedMethod method) {
            return Optional.of(method);
        }
        return Optional.empty();
    }

    public Optional<VisitedField> getField(String name) {
        Optional<VisitedMember> member = get(name);
        if (member.isPresent() && member.get() instanceof VisitedField field) {
            return Optional.of(field);
        }
        return Optional.empty();
    }

    public Optional<VisitedMember> get(String name) {
        String info = parseDescriptor(this, name).toString();
        return get(new ArrayList<>(), info);
    }

    // Ldev/hugeblank/allium/util/asm/TargetClassVisitor;visitMethod(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;)Lorg/objectweb/asm/MethodVisitor;

    // TODO: respectfully, this class plus mappings suck.
    // So basically for finding the method name we have to dig from the source class down. But then creating the mapping is just taking everything that was already there.
    // This will probably not work in all mappings, it barely works with tiny. In my dream world all of this is in the mappings and we can route to a class/method just by that.

    private Optional<VisitedMember> get(List<VisitedClass> visited, String info) {
        visited.add(this);
        // If the descriptor lacks an owner then we know it must belong to the class we're currently in, no recursion necessary.
        // The only thing we WILL have to recurse for is the member name.
        Optional<VisitedMember> member = Optional.ofNullable(visitedMethods.get(info));
        if (member.isPresent()) return member;
        member = Optional.ofNullable(visitedFields.get(info));
        if (member.isPresent()) return member;
        for (VisitedClass visitedClass : inheritance) {
            if (visited.contains(visitedClass)) continue;
            member = visitedClass.get(visited, info);
            if (member.isPresent()) {
                return member;
            }
        }
        return Optional.empty();
    }

    void addVisitedField(LuaState state, int access, String name, String descriptor, String signature, Object value) throws LuaError {
        visitedFields.put(findMemberMapping(state, name, descriptor), new VisitedField(this, access, name, descriptor, signature, value));
    }

    void addVisitedMethod(LuaState state, int access, String name, String descriptor, String signature, String[] exceptions) throws LuaError {
        String key = "L" + mappingsPair.mappedPath() + ";" + findMemberMapping(state, name, descriptor);
        StringBuilder mappedDescriptor = new StringBuilder("(");
        for (Type arg : Type.getArgumentTypes(descriptor)) {
            mapTypeArg(state, mappedDescriptor, arg);
        }
        mappedDescriptor.append(")");
        mapTypeArg(state, mappedDescriptor, Type.getReturnType(descriptor));
        if (!name.equals("<init>") && !name.equals("<clinit>")) {
            key = key+mappedDescriptor;
        }
        visitedMethods.put(key, new VisitedMethod(this, access, name, descriptor, signature, exceptions));
    }

    private String findMemberMapping(LuaState state, String memberName, String descriptor) throws LuaError {
        Mappings mappings = ScriptRegistry.scriptFromState(state).getMappings();
        try {
            MappingTree.ClassMapping classMapping = mappings.toUnmappedClass(name());
            String target = null;
            try {
                target = mappings.toMappedMemberName(classMapping, memberName, descriptor);
            } catch (NoSuchMappingException ignored) {}
            if (target != null) {
                return target;
            }
            for (VisitedClass visitedClass : inheritance) {
                target = visitedClass.findMemberMapping(state, memberName, descriptor);
                if (target != null) {
                    return target;
                }
            }
        } catch (NoSuchMappingException e) {
            // TODO: Warn, and probably extract out to constructor
        }
        return memberName;
    }

    private void mapTypeArg(LuaState state, StringBuilder mappedDescriptor, Type arg) throws LuaError {
        if (arg.getSort() == Type.OBJECT) {
            String mappedType = arg.getInternalName();
            if (!Allium.DEVELOPMENT) {
                try {
                    mappedType = ScriptRegistry.scriptFromState(state).getMappings().toMappedClassName(arg.getInternalName());
                } catch (NoSuchMappingException ignored) {}
            }
            mappedDescriptor
                    .append("L")
                    .append(mappedType)
                    .append(";");
        } else {
            mappedDescriptor.append(arg.getInternalName());
        }
    }

    public boolean isInterface() {
        return (access & ACC_INTERFACE) != 0;
    }

    public int version() {
        return version;
    }

    public int access() {
        return access;
    }

    public String name() {
        return mappingsPair.unmappedPath();
    }

    public String signature() {
        return signature;
    }

    public String superName() {
        return superName;
    }

    public String[] interfaces() {
        return interfaces;
    }

    public static MemberInfo remap(LuaState state, MemberInfo info) throws LuaError {
        Mappings mappings = ScriptRegistry.scriptFromState(state).getMappings();
        MappingTree.MemberMapping unmappedSrc;
        try {
            unmappedSrc = mappings.toUnmappedMember(mappings.toUnmappedClass(info.getOwner()), info.getName(), info.getDesc());
        } catch (NoSuchMappingException e) {
            throw new RuntimeException(e);
        }
        return new MemberInfo(
                unmappedSrc.getName(mappings.getDstID()),
                unmappedSrc.getOwner().getDstName(mappings.getDstID()),
                unmappedSrc.getDesc(mappings.getDstID())
        );
    }

    public static MemberInfo parseDescriptor(VisitedClass visitedClass, String descriptor) {
        MemberInfo info = MemberInfo.parse(descriptor, null);
        if (!info.isFullyQualified()) {
            info = new MemberInfo(info.getName(), visitedClass.mappingsPair.mappedPath(), info.getDesc());
        }
        return info;
    }

    public static VisitedClass ofClass(LuaState state, String mappedClassName) throws LuaError {
        if (!VISITED.containsKey(mappedClassName)) {
            try {
                TargetClassVisitor.parseMappedTarget(state, mappedClassName);
            } catch (IOException e) {
                throw new LuaError(new RuntimeException("Could not read target class: " + mappedClassName, e));
            }
        }
        return VISITED.get(mappedClassName);
    }

    public static void clear() {
        VISITED.clear();
    }
}
