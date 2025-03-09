package dev.hugeblank.allium.loader;

import java.util.HashMap;
import java.util.Map;

public class ScriptRegistry {
    public static final ScriptRegistry MAIN;
    public static final ScriptRegistry CLIENT;
    public static final ScriptRegistry SERVER;

    private final EnvType envType;
    private final Map<String, Script> scripts;
    private final Map<Key, Script> scriptKeys;

    private ScriptRegistry(EnvType envType) {
        this.envType = envType;
        this.scripts = new HashMap<>();
        this.scriptKeys = new HashMap<>();
    }

    public void registerScript(Script script) {
        String id = script.getId();
        if (!scripts.containsKey(id)) {
            scripts.put(id, script);
            Key key = new Key(id);
            scriptKeys.put(key, script);
        }
        // TODO: Throw an error or something?
    }

    public void unloadScript(Key key) {
        if (scriptKeys.containsKey(key)) {
            scriptKeys.get(key).unload();
        }
    }

    public EnvType getType() {
        return envType;
    }

    public boolean hasScript(String name) {
        return scripts.containsKey(name);
    }

    public Script getScript(String name) {
        return scripts.get(name);
    }

    public static class Key {
        private final String id;

        Key(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    static {
        MAIN = new ScriptRegistry(EnvType.MAIN);
        CLIENT = new ScriptRegistry(EnvType.CLIENT);
        SERVER = new ScriptRegistry(EnvType.SERVER);
    }

    public enum EnvType {
        MAIN("main"),
        CLIENT("client"),
        SERVER("server");

        private final String key;
        EnvType(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
