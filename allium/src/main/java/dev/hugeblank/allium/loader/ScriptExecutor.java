package dev.hugeblank.allium.loader;

import net.fabricmc.api.EnvType;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptExecutor extends EnvironmentManager {
    protected final Script script;
    protected final Path path;
    protected final Entrypoint entrypoint;

    public ScriptExecutor(Script script, Path path, Entrypoint entrypoint) {
        super();
        this.script = script;
        this.path = path;
        this.entrypoint = entrypoint;
    }

    public LuaState getState() {
        return state;
    }

    public Varargs initialize(EnvType envType) throws Throwable {
        createEnvironment(script, envType);
        if (entrypoint.has(Entrypoint.Type.STATIC) && entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            Varargs out = execute(Entrypoint.Type.STATIC, script.getID() + ":static");
            execute(Entrypoint.Type.DYNAMIC, script.getID() + ":dynamic");
            return out;
        } else if (entrypoint.has(Entrypoint.Type.STATIC)) {
            return execute(Entrypoint.Type.STATIC, script.getID());
        } else if (entrypoint.has(Entrypoint.Type.DYNAMIC)) {
            return execute(Entrypoint.Type.DYNAMIC, script.getID());
        }
        // This should be caught sooner, but who knows maybe a dev (hugeblank) will come along and mess something up
        throw new Exception("Expected either static or dynamic entrypoint, got none");
    }

    public void preInitialize() {

    }


    public Varargs reload() throws LuaError, CompileException, IOException {
        if (entrypoint.has(Entrypoint.Type.DYNAMIC)) return execute(Entrypoint.Type.DYNAMIC, script.getID());
        return null;
    }

    private Varargs execute(Entrypoint.Type type, String name) throws IOException, CompileException, LuaError {
        return LuaThread.runMain(state, load(getInputStream(type), name));
    }

    private InputStream getInputStream(Entrypoint.Type entrypointType) throws IOException {
        return entrypoint.has(entrypointType) ?
                Files.newInputStream(path.resolve(entrypoint.get(entrypointType))) :
                null;
    }

    public LuaFunction load(InputStream stream, String name) throws CompileException, IOException, LuaError {
        return LoadState.load(
                state,
                stream,
                name,
                state.globals()
        );
    }

}
