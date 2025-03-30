package dev.hugeblank.allium.mappings;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.minecraft.util.JsonHelper;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class MojangLoader implements MappingsLoader {
    public static final String VERSION_MANIFEST_ENDPOINT = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final Path CACHED_MAPPINGS = FileHelper.MAPPINGS_CFG_DIR.resolve("yarn-mappings-" +  SetupHelpers.getGameVersion() + ".tiny");

    @Override
    public void load(MappingVisitor visitor) {
        try {

            if (Files.exists(CACHED_MAPPINGS)) {
                try (BufferedReader br = Files.newBufferedReader(CACHED_MAPPINGS)) {
                    Tiny2FileReader.read(br, visitor);
                    return;
                }
            }

            MemoryMappingTree tree = new MemoryMappingTree(true);

            JsonArray versions = JsonHelper.getArray(SetupHelpers.fetch(VERSION_MANIFEST_ENDPOINT), "versions");
            String chosenUrl = null;

            for (int i = 0; i < versions.size(); i++) {
                JsonObject version = versions.get(i).getAsJsonObject();

                if (JsonHelper.getString(version, "id").equals(SetupHelpers.getGameVersion()))
                    chosenUrl = JsonHelper.getString(version, "url");
            }

            if (chosenUrl == null)
                throw new UnsupportedOperationException("Couldn't find version " + SetupHelpers.getGameVersion() + " on Mojang's servers!");

            JsonObject manifest = SetupHelpers.fetch(chosenUrl);
            JsonObject downloads = JsonHelper.getObject(manifest, "downloads");
            JsonObject clientMappings = JsonHelper.getObject(downloads, "client_mappings");
            JsonObject serverMappings = JsonHelper.getObject(downloads, "server_mappings");
            var sw = new MappingSourceNsSwitch(tree, "official");

            readProGuardInto(JsonHelper.getString(clientMappings, "url"), sw);
            readProGuardInto(JsonHelper.getString(serverMappings, "url"), sw);

            try (BufferedWriter bw = Files.newBufferedWriter(CACHED_MAPPINGS)) {
                tree.accept(new Tiny2FileWriter(bw, false));
            }

            tree.accept(visitor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void readProGuardInto(String url, MappingVisitor visitor) throws IOException {
            ProGuardFileReader.read(new InputStreamReader(new BufferedInputStream(new URL(url).openStream())), "mojnamed", "official", visitor);
    }

    @Override
    public String getID() {
        return "mojang";
    }
}