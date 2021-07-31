package io.github.ocelot.serverdownloader.client;

import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.client.download.DownloadableResourcePackFile;
import io.github.ocelot.serverdownloader.client.screen.DownloadModFilesConfirmationScreen;
import io.github.ocelot.serverdownloader.common.download.DownloadableFile;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = ServerDownloader.MOD_ID)
public class ClientEvents
{
    @SubscribeEvent
    public static void onEvent(InputEvent.KeyInputEvent event)
    {
        if (FMLLoader.isProduction())
            return;
        if (event.getKey() == GLFW.GLFW_KEY_M)
        {
            List<DownloadableFile> list = new ArrayList<>(ModFileManager.getFiles());
            list.add(new DownloadableResourcePackFile("http://127.0.0.1:25566/resources.zip"));
            list.add(new DownloadableResourcePackFile("https://www.google.com"));
            list.add(new DownloadableResourcePackFile("http://127.0.0.1:25566/resources.zip"));
            list.add(new DownloadableResourcePackFile("http://127.0.0.1:25566/resources.zip"));
            list.add(new DownloadableResourcePackFile("http://127.0.0.1:25566/resources.zip"));
            list.add(new DownloadableResourcePackFile(""));
            list.add(new DownloadableResourcePackFile(""));
            list.add(new DownloadableResourcePackFile("http://127.0.0.1:25566/resources.zip"));
            list.add(new DownloadableResourcePackFile(""));
            list.add(new DownloadableResourcePackFile("http://127.0.0.1:25566/resources.zip"));
            list.add(new DownloadableResourcePackFile(""));
            Minecraft.getInstance().setScreen(new DownloadModFilesConfirmationScreen(null, "", list));
        }
    }
}
