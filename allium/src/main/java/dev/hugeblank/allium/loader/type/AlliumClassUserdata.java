package dev.hugeblank.allium.loader.type;

import me.basiqueevangelist.enhancedreflection.api.EClass;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

// The only real purpose this class has is to assert that the object encapsulated by the userdata is an EClass.
public class AlliumClassUserdata extends LuaUserdata {

    public AlliumClassUserdata(EClass<?> obj, LuaTable metatable) {
        super(obj, metatable);
    }
}
