package dev.hugeblank.bouquet.api.lib;

import dev.hugeblank.allium.loader.type.coercion.TypeCoercions;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import dev.hugeblank.bouquet.util.TableHelpers;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;
import org.squiddev.cobalt.*;

import java.util.Set;

@LuaWrapped(name = "nbt")
public class NbtLib implements WrappedLuaLibrary {
    @LuaWrapped
    public static LuaValue fromNbt(NbtElement element) {
        return switch (element.getType()) {
            case NbtElement.BYTE_TYPE, NbtElement.SHORT_TYPE, NbtElement.INT_TYPE ->
                    ValueFactory.valueOf(((AbstractNbtNumber) element).intValue());
            case NbtElement.LONG_TYPE ->
                    ValueFactory.valueOf(((NbtLong) element).longValue());
            case NbtElement.FLOAT_TYPE, NbtElement.DOUBLE_TYPE ->
                    ValueFactory.valueOf(((AbstractNbtNumber) element).doubleValue());
            case NbtElement.BYTE_ARRAY_TYPE ->
                    ValueFactory.valueOf(((NbtByteArray) element).getByteArray());
            case NbtElement.STRING_TYPE -> {
                // asString() now returns Optional<String>
                String str = element.asString().orElse("");
                yield ValueFactory.valueOf(str);
            }
            case NbtElement.LIST_TYPE -> {
                NbtList list = (NbtList) element;
                LuaTable table = new LuaTable();
                for (int i = 0; i < list.size(); i++) {
                    table.rawset(i + 1, fromNbt(list.get(i)));
                }
                yield table;
            }
            case NbtElement.COMPOUND_TYPE -> {
                NbtCompound compound = (NbtCompound) element;
                LuaTable table = new LuaTable();

                for (String key : compound.getKeys()) {
                    NbtElement value = compound.get(key);
                    if (value != null) {
                        table.rawset(key, fromNbt(value));
                    } else {
                        table.rawset(key, Constants.NIL);
                    }
                }
                yield table;
            }
            case NbtElement.INT_ARRAY_TYPE ->
                    TypeCoercions.toLuaValue(((NbtIntArray) element).getIntArray());
            case NbtElement.LONG_ARRAY_TYPE ->
                    TypeCoercions.toLuaValue(((NbtLongArray) element).getLongArray());
            default -> Constants.NIL;
        };
    }

    @Nullable
    @LuaWrapped
    public static NbtElement toNbt(LuaValue value) throws LuaError {
        return toNbtInternal(value, new ReferenceOpenHashSet<>());
    }

    public static NbtElement toNbtSafe(LuaValue value) {
        try {
            return toNbt(value);
        } catch (LuaError e) {
            return null;
        }
    }

    private static NbtElement toNbtInternal(LuaValue value, Set<LuaValue> seenValues) throws LuaError {
        if (value instanceof LuaUserdata userdata) {
            Object val = userdata.toUserdata();
            if (val instanceof NbtElement nbtVal) return nbtVal;
        }

        if (seenValues.contains(value)) return null;

        if (value instanceof LuaInteger)
            return NbtInt.of(value.toInteger());
        else if (value instanceof LuaBoolean)
            return NbtByte.of(value.toBoolean());
        else if (value instanceof LuaNumber)
            return NbtDouble.of(value.toDouble());
        else if (value instanceof LuaString)
            return NbtString.of(value.toString());
        else if (value instanceof LuaTable table) {
            NbtCompound nbt = new NbtCompound();
            seenValues.add(table);
            TableHelpers.forEach(table, (k, v) -> {
                try {
                    NbtElement child = toNbtInternal(v, seenValues);
                    if (child != null) nbt.put(k.toString(), child);
                } catch (LuaError ignored) {
                }
            });
            seenValues.remove(table);
            return nbt;
        }
        return null;
    }
}
