package io.github.ocelot.serverdownloader.server;

import io.github.ocelot.serverdownloader.ServerDownloader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>Mod configuration for server.</p>
 *
 * @author Ocelot
 */
public class ServerConfig
{
    public static final ServerConfig INSTANCE;
    private static final ForgeConfigSpec SPEC;

    static
    {
        Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    public ForgeConfigSpec.IntValue httpServerPort;

    private ServerConfig(ForgeConfigSpec.Builder builder)
    {
        builder.comment("Server Mod Downloader Server Config");
        this.httpServerPort = builder
                .worldRestart()
                .comment("The port to open the HTTP server on. This is required to be an open port to send mods to clients!")
                .translation("config." + ServerDownloader.MOD_ID + ".httpServerPort")
                .defineInRange("httpServerPort", 25566, 1, 65535);
    }

    public static void init(ModLoadingContext context)
    {
        context.registerConfig(ModConfig.Type.SERVER, SPEC);
    }
}
