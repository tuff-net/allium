package dev.hugeblank.allium;

import dev.hugeblank.allium.mappings.Mappings;
import dev.hugeblank.allium.mappings.YarnLoader;
import dev.hugeblank.allium.util.MixinConfigUtil;
import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class AlliumPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {

        SetupHelpers.initializeDirectories();

        Mappings.LOADERS.register(new YarnLoader());

        SetupHelpers.initializeExtensions();
        SetupHelpers.collectScripts();

        SetupHelpers.initializeEnvironment(Allium.EnvType.MIXIN);

        MixinConfigUtil.applyConfiguration();
    }

}
