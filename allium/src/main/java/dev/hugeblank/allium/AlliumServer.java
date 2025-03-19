package dev.hugeblank.allium;

import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;

public class AlliumServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        SetupHelpers.initializeEnvironment(EnvType.SERVER);
    }
}
