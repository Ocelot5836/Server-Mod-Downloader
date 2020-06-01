package io.github.ocelot;

import com.mojang.brigadier.CommandDispatcher;
import io.github.ocelot.client.init.ClientInit;
import io.github.ocelot.common.init.ServerDownloaderMessages;
import io.github.ocelot.server.command.ReloadClientModsCommand;
import io.github.ocelot.common.download.ModFileManager;
import net.minecraft.command.CommandSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Ocelot
 */
@SuppressWarnings("unused")
@Mod(ServerDownloader.MOD_ID)
public class ServerDownloader
{
    public static final String MOD_ID = "serverdownloader";
    public static final ScheduledExecutorService THREAD_EXECUTOR = Executors.newSingleThreadScheduledExecutor(task -> new Thread(task, MOD_ID + "-executor"));

    private static final Logger LOGGER = LogManager.getLogger();

    public ServerDownloader()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::init);
        DistExecutor.runWhenOn(Dist.CLIENT, () -> () ->
        {
            ClientInit.init(modBus);
            modBus.addListener(ClientInit::setup);
        });
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void init(FMLCommonSetupEvent event)
    {
        ServerDownloaderMessages.init();
    }

    @SubscribeEvent
    public void onEvent(FMLServerStartingEvent event)
    {
        CommandDispatcher<CommandSource> dispatcher = event.getCommandDispatcher();
        ReloadClientModsCommand.register(dispatcher);

        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> ModFileManager::load);
    }

    @SubscribeEvent
    public void onEvent(FMLServerStoppingEvent event)
    {
    }
}
