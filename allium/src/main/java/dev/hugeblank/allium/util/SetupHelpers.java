package dev.hugeblank.allium.util;

import com.google.common.collect.ImmutableSet;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.AlliumExtension;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.ScriptRegistry;
import dev.hugeblank.allium.mappings.Mappings;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class SetupHelpers {
    public static void initializeEnvironment(Allium.EnvType containerEnvType) {
        ScriptRegistry registry = ScriptRegistry.getInstance(containerEnvType);
        ScriptRegistry.REFS.forEach((ref) -> registry.register(new Script(ref, containerEnvType)));
        registry.forEach(Script::initialize);
        Set<Script> set = ScriptRegistry.getInstance(containerEnvType).getAll();
        list(set, containerEnvType, "Initialized " + set.size() + " scripts:\n",
                (builder, script) -> {
                    if (script.isInitialized()) builder.append(script.getID());
                }
        );
    }

    public static void collectScripts() {
        ImmutableSet.Builder<Script.Reference> setBuilder = ImmutableSet.builder();
        setBuilder.addAll(FileHelper.getValidDirScripts(FileHelper.getScriptsDirectory()));
        setBuilder.addAll(FileHelper.getValidModScripts());
        Set<Script.Reference> refs = setBuilder.build();

        if (refs.isEmpty()) {
            return;
        }

        for (Script.Reference ref : refs) {
            String mappingsID = ref.manifest().mappings();
            if (!Mappings.REGISTRY.has(mappingsID) && Mappings.LOADERS.has(mappingsID)) {
                Mappings.REGISTRY.register(Mappings.of(mappingsID, Mappings.LOADERS.get(mappingsID).load()));
            } else if (!Mappings.LOADERS.has(mappingsID)){
                Allium.LOGGER.error("No mappings exist with ID {} for script {}", mappingsID, ref.manifest().id());
                refs.remove(ref);
                continue;
            }
            ScriptRegistry.REFS.register(ref);
        }

        list(refs, Allium.EnvType.COMMON, "Found " + refs.size() + " scripts:\n",
                (strBuilder, ref) -> strBuilder.append(ref.manifest().id())
        );
    }

    public static void initializeExtensions() {
        Set<ModContainer> mods = new HashSet<>();
        FabricLoader.getInstance().getEntrypointContainers(Allium.ID, AlliumExtension.class)
                .forEach((initializer) -> {
                    initializer.getEntrypoint().onInitialize();
                    mods.add(initializer.getProvider());
                });

        list(mods, Allium.EnvType.COMMON, "Initialized " + mods.size() + " extensions:\n",
                (builder, mod) -> builder.append(mod.getMetadata().getId())
        );
    }

    private static <T> void list(Collection<T> collection, Allium.EnvType envType, String initial, BiConsumer<StringBuilder, T> func) {
        if (envType != Allium.EnvType.COMMON) return;
        StringBuilder builder = new StringBuilder(initial);
        collection.forEach((script) -> {
            builder.append("\t- ");
            func.accept(builder, script);
            builder.append("\n");
        });
        Allium.LOGGER.info(builder.substring(0, builder.length()-1));
    }
}
