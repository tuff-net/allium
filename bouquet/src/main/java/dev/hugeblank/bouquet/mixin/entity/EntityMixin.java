package dev.hugeblank.bouquet.mixin.entity;

import dev.hugeblank.bouquet.api.lib.NbtLib;
import dev.hugeblank.bouquet.util.EntityDataHolder;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaValue;

import java.util.HashMap;
import java.util.Map;

@Mixin(Entity.class)
public class EntityMixin implements EntityDataHolder {
    @Unique
    private final Map<String, LuaValue> allium_tempData = new HashMap<>();
    @Unique
    private final Map<String, LuaValue> allium_data = new HashMap<>();

    @Override
    public LuaValue allium$getTemporaryData(String key) {
        return this.allium_tempData.getOrDefault(key, Constants.NIL);
    }

    @Override
    public void allium$setTemporaryData(String key, LuaValue value) {
        this.allium_tempData.put(key, value);
    }

    @Override
    public LuaValue allium$getData(String key) {
        return this.allium_data.getOrDefault(key, Constants.NIL);
    }

    @Override
    public void allium$setData(String key, LuaValue value) {
        this.allium_data.put(key, value);
    }

    @Override
    public void allium_private$copyFromData(Entity source) {
        var mixined = (EntityMixin) (Object) source;
        this.allium_tempData.clear();
        this.allium_data.clear();
        if (mixined != null) {
            this.allium_tempData.putAll(mixined.allium_tempData);
            this.allium_data.putAll(mixined.allium_data);
        }
    }

@Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
private void allium_storeData(NbtCompound nbt, CallbackInfo ci) {
    if (!this.allium_data.isEmpty()) {
        NbtCompound data = new NbtCompound();
        for (var entry : this.allium_data.entrySet()) {
            var val = NbtLib.toNbtSafe(entry.getValue());
            if (val != null) data.put(entry.getKey(), val);
        }
        nbt.put("AlliumData", data);
    }
}

@Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
private void allium_readData(NbtCompound nbt, CallbackInfo ci) {
    NbtCompound data = nbt.getCompoundOrEmpty("AlliumData");
    for (String key : data.getKeys()) {
        var val = data.get(key);
        if (val != null) {
            var luaVal = NbtLib.fromNbt(val);
            if (luaVal != null) this.allium_data.put(key, luaVal);
        }
    }
}


}
