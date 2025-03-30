package dev.hugeblank.allium.mappings;

import com.google.common.net.UrlEscapers;
import dev.hugeblank.allium.Allium;
import dev.hugeblank.allium.api.MappingsLoader;
import dev.hugeblank.allium.util.FileHelper;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;

public class IntermediaryLoader implements MappingsLoader {
    private static final Path CACHED_MAPPINGS = FileHelper.MAPPINGS_CFG_DIR.resolve("intermediary-mappings-" +  SetupHelpers.getGameVersion() + ".tiny");

    @Override
    public void load(MappingVisitor visitor) {
            try {
                if (!Files.exists(CACHED_MAPPINGS)) {

                    String encodedVersion = UrlEscapers.urlFragmentEscaper().escape(SetupHelpers.getGameVersion());
                    // Download V2 jar
                    String artifactUrl = "https://maven.fabricmc.net/net/fabricmc/intermediary/" + encodedVersion + "/intermediary-" + encodedVersion + "-v2.jar";

                    File jarFile = FileHelper.MAPPINGS_CFG_DIR.resolve("intermediary-mappings.jar").toFile();
                    jarFile.deleteOnExit();
                    try {
                        FileUtils.copyURLToFile(new URL(artifactUrl), jarFile);
                    } catch (IOException e) {
                        Allium.LOGGER.error("Failed to download mappings!");
                        throw e;
                    }

                    try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
                        Files.copy(jar.getPath("mappings/mappings.tiny"), CACHED_MAPPINGS, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        Allium.LOGGER.error("Failed to extract mappings!");
                        throw e;
                    }
                }
                Tiny2FileReader.read(Files.newBufferedReader(CACHED_MAPPINGS), visitor);
            } catch (IOException e) {
                Allium.LOGGER.error("Failed to load intermediary mappings", e);
            }
    }

    @Override
    public String getID() {
        return "intermediary";
    }
}
