package dev.hugeblank.allium.mappings;

import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.util.Identifiable;
import dev.hugeblank.allium.util.Registry;
import me.basiqueevangelist.enhancedreflection.api.EClass;
import me.basiqueevangelist.enhancedreflection.api.EField;
import me.basiqueevangelist.enhancedreflection.api.EMethod;
import me.basiqueevangelist.enhancedreflection.api.EParameter;
import net.fabricmc.mappingio.tree.VisitableMappingTree;
import org.jetbrains.annotations.Debug;

import java.util.*;

@Debug.Renderer(text = "\"Mappings { ... }\"", hasChildren = "false")
public record Mappings(VisitableMappingTree tree) {


    public void mappedMethod(EMethod method) {
        return tree.getMethod(method.declaringClass().name(), method.name(), );
    }

    public List<String> getUnmapped(String value) {
        var val = this.mapped2unmapped.get(value);
        return val != null && !Allium.DEVELOPMENT ? val : List.of(value);
    }

    public String getMapped(String value) {
        return this.unmapped2mapped.getOrDefault(value, value);
    }

    public static String asMethod(String className, String method) {
        return (className + "#" + method).replace('/', '.');
    }

    private static String asDescriptor(List<EParameter> parameters) {
        String vals = parameters.stream().map(EParameter::name).reduce("", (a, b) -> {

        }, ());
    }

    public static String asMethod(EClass<?> clazz, EMethod method) {
        return asMethod(clazz.name(), method.name());
    }

    public static String asMethod(EClass<?> clazz, EField field) {
        return asMethod(clazz.name(), field.name());
    }

    public static String asClass(String className) {
        return className.replace('/', '.');
    }

    public static String asClass(EClass<?> clazz) {
        return asClass(clazz.name());
    }
}
