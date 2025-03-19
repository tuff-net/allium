package dev.hugeblank.allium;

import dev.hugeblank.allium.util.SetupHelpers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;

public class AlliumClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SetupHelpers.initializeEnvironment(EnvType.CLIENT);
    }
}
