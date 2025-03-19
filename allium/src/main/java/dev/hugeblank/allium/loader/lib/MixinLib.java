package dev.hugeblank.allium.loader.lib;

import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.api.event.MixinEventType;
import dev.hugeblank.allium.loader.Script;
import dev.hugeblank.allium.loader.mixin.MixinClassBuilder;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.allium.loader.type.annotation.OptionalArg;
import net.fabricmc.api.EnvType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.LuaError;

@LuaWrapped(name = "mixin")
public class MixinLib implements WrappedLuaLibrary {
    private final Script script;

    public MixinLib(Script script, EnvType envType) {
        this.script = script;
    }

    @LuaWrapped
    public static MixinEventType get(Identifier eventId) {
        return MixinEventType.EVENT_MAP.get(eventId);
    }

    @LuaWrapped
    public MixinClassBuilder to(String targetClass, @OptionalArg @Nullable String targetEnvironment, @OptionalArg @Nullable Boolean duck) throws LuaError {
        EnvType targetEnv;
        if (targetEnvironment == null) {
            targetEnv = null;
        } else if (targetEnvironment.equals("client")) {
            targetEnv = EnvType.CLIENT;
        } else if (targetEnvironment.equals("server")) {
            targetEnv = EnvType.SERVER;
        } else {
            throw new LuaError("Mixin for " + targetClass + " expects target environment of nil, 'client' or 'server'.");
        }
        return new MixinClassBuilder(targetClass, targetEnv, duck != null && duck, script);
    }
}
