package dev.hugeblank.allium.loader.type;

import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaUserdata;

public class AlliumClassUserdata extends LuaUserdata {

    public AlliumClassUserdata(Object obj, LuaTable metatable) {
        super(obj, metatable);
    }
}
