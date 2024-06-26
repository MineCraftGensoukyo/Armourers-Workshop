package moe.plushie.armourers_workshop.init.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import moe.plushie.armourers_workshop.api.config.IConfigSpec;
import moe.plushie.armourers_workshop.init.environment.EnvironmentType;
import net.minecraft.server.MinecraftServer;

import java.io.File;

public class EnvironmentManager {

    @ExpectPlatform
    public static String getVersion() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static EnvironmentType getEnvironmentType() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static File getRootDirectory() {
        throw new AssertionError();
    }

    public static File getSkinLibraryDirectory() {
        return new File(getRootDirectory(), "skin-library");
    }

    public static File getSkinCacheDirectory() {
        return new File(getRootDirectory(), "skin-cache");
    }

    @ExpectPlatform
    public static MinecraftServer getServer() {
        throw new AssertionError();
    }

    public static boolean isDedicatedServer() {
        return getEnvironmentType() == EnvironmentType.SERVER;
    }

    @ExpectPlatform
    public static boolean isDevelopmentEnvironment() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static IConfigSpec getClientConfigSpec() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static IConfigSpec getCommonConfigSpec() {
        throw new AssertionError();
    }
}


