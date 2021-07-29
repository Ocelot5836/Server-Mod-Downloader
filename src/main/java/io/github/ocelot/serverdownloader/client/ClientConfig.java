package io.github.ocelot.serverdownloader.client;

import io.github.ocelot.serverdownloader.ServerDownloader;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>Mod configuration for client.</p>
 *
 * @author Ocelot
 */
public class ClientConfig
{
    public static final ClientConfig INSTANCE;
    private static final ForgeConfigSpec SPEC;

    static
    {
        Pair<ClientConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    public ForgeConfigSpec.IntValue maxDownloadSize;
    public ForgeConfigSpec.IntValue downloadBufferSize;

    private ClientConfig(ForgeConfigSpec.Builder builder)
    {
        builder.comment("Server Mod Downloader Client Config");
        this.maxDownloadSize = builder
                .comment("The maximum allowed size of a file download.")
                .translation("config." + ServerDownloader.MOD_ID + ".maxDownloadSize")
                .defineInRange("maxDownloadSize", 100, 10, Integer.MAX_VALUE);
        this.downloadBufferSize = builder
                .comment("The memory buffer size to use when downloading files.")
                .translation("config." + ServerDownloader.MOD_ID + ".downloadBufferSize")
                .defineInRange("downloadBufferSize", 4096, 1024, Integer.MAX_VALUE);
    }

    public static void init(ModLoadingContext context)
    {
        context.registerConfig(ModConfig.Type.CLIENT, SPEC);
    }
}
