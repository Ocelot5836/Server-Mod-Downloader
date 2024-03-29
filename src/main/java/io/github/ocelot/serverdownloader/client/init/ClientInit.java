package io.github.ocelot.serverdownloader.client.init;

import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.client.ClientConfig;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ocelot
 */
public class ClientInit
{
    @OnlyIn(Dist.CLIENT)
    public static void init(IEventBus bus)
    {
        bus.addListener(EventPriority.NORMAL, false, ColorHandlerEvent.Block.class, event ->
        {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            if (resourceManager instanceof ReloadableResourceManager)
            {
                ((ReloadableResourceManager) resourceManager).registerReloadListener(ModFileManager.getReloader());
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            try
            {
                Path modsFolder = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), "mods");
                if (!Files.exists(modsFolder))
                    Files.createDirectory(modsFolder);

                Path newModsFolder = Paths.get(Minecraft.getInstance().gameDirectory.getAbsolutePath(), ServerDownloader.MOD_ID + "-mod-downloads");
                if (!Files.exists(newModsFolder))
                    return;

                Files.list(newModsFolder).forEach(child ->
                {
                    try
                    {
                        Files.deleteIfExists(modsFolder.resolve(child.getFileName()));
                        Files.move(child, modsFolder.resolve(child.getFileName()));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }));
    }

    @OnlyIn(Dist.CLIENT)
    public static void initClient(FMLClientSetupEvent event)
    {
    }
}
