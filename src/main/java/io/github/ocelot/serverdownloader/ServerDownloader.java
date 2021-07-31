package io.github.ocelot.serverdownloader;

import io.github.ocelot.serverdownloader.client.ClientConfig;
import io.github.ocelot.serverdownloader.client.init.ClientInit;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import io.github.ocelot.serverdownloader.common.network.ServerDownloaderMessages;
import io.github.ocelot.serverdownloader.server.ModFileHttpServer;
import io.github.ocelot.serverdownloader.server.ServerConfig;
import io.github.ocelot.sonar.Sonar;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * @author Ocelot
 */
@Mod.EventBusSubscriber(Dist.DEDICATED_SERVER)
@Mod(ServerDownloader.MOD_ID)
public class ServerDownloader
{
    public static final String MOD_ID = "serverdownloader";

    public ServerDownloader()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        Sonar.init(modBus);
        modBus.addListener(this::init);
        modBus.addListener(this::initDedicatedServer);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
        {
            ClientInit.init(modBus);
            modBus.addListener(ClientInit::initClient);
        });
        ClientConfig.init(ModLoadingContext.get());
        ServerConfig.init(ModLoadingContext.get());
    }

    private void init(FMLCommonSetupEvent event)
    {
        ServerDownloaderMessages.init();
    }

    private void initDedicatedServer(FMLDedicatedServerSetupEvent event)
    {
    }

    @SubscribeEvent
    public static void onEvent(AddReloadListenerEvent event)
    {
        event.addListener(ModFileManager.getReloader());
    }

    @SubscribeEvent
    public static void onEvent(FMLServerStartingEvent event)
    {
        ModFileHttpServer.open(event.getServer());
    }

    @SubscribeEvent
    public static void onEvent(FMLServerStoppingEvent event)
    {
        ModFileHttpServer.shutdown();
    }
}
