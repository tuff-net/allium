package dev.hugeblank.bouquet.api.lib;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.hugeblank.allium.api.WrappedLuaLibrary;
import dev.hugeblank.allium.loader.type.annotation.LuaWrapped;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.TagParser;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

@LuaWrapped(name = "text")
public class TextLib implements WrappedLuaLibrary {

    @LuaWrapped(name = "empty")
    public static final Text EMPTY = Text.empty();

    // See https://placeholders.pb4.eu/dev/parsing-placeholders/#placeholder-context
    @LuaWrapped
    public Text format(String input) {
        return TagParser.DEFAULT.parseText(input, ParserContext.of());
    }

    @LuaWrapped
    public Text formatSafe(String input) {
        return TagParser.DEFAULT_SAFE.parseText(input, ParserContext.of());
    }

    // ✅ Updated for 1.21.6+: use TextCodecs instead of Text.Serialization
    @LuaWrapped
    public Text fromJson(String input) {
        try {
            var json = JsonParser.parseString(input);
            return TextCodecs.CODEC.parse(JsonOps.INSTANCE, json)
                    .result()
                    .orElse(Text.literal(input)); // fallback if invalid JSON
        } catch (Exception e) {
            return Text.literal(input);
        }
    }

    // ✅ Updated for 1.21.6+: use TextCodecs instead of Text.Serialization
    @LuaWrapped
    public String toJson(Text text) {
        try {
            var json = TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text)
                    .result()
                    .map(Object::toString)
                    .orElse("{}");
            return json;
        } catch (Exception e) {
            return "{}";
        }
    }
}
