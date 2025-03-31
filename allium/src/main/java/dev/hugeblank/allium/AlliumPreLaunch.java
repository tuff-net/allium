package dev.hugeblank.allium;

import dev.hugeblank.allium.mappings.*;
import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.SetupHelpers;
import io.netty.util.Mapping;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class AlliumPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {

        SetupHelpers.initializeDirectories();

        PlatformMappings.registerLoader(new YarnLoader());
        PlatformMappings.registerLoader(new IntermediaryLoader());
        PlatformMappings.registerLoader(new MojangLoader());

        FabricHandlers.register();

        SetupHelpers.collectScripts();

        MixinConfigUtil.applyConfiguration();
    }

}
