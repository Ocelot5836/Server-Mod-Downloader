package io.github.ocelot;

import io.github.ocelot.client.init.ClientInit;
import io.github.ocelot.common.network.ServerDownloaderMessages;
import io.github.ocelot.server.BlacklistedServerModLoader;
import io.github.ocelot.server.ModFileHttpServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
        modBus.addListener(this::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
        {
            ClientInit.init(modBus);
            modBus.addListener(ClientInit::setup);
        });
    }

    private void init(FMLCommonSetupEvent event)
    {
        ServerDownloaderMessages.init();
    }

    @SubscribeEvent
    public static void onEvent(AddReloadListenerEvent event)
    {
        event.addListener(BlacklistedServerModLoader.INSTANCE);
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
