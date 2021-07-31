package io.github.ocelot.serverdownloader.client.download;

import io.github.ocelot.serverdownloader.common.download.DownloadableFile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Ocelot
 */
public class DownloadableResourcePackFile implements DownloadableFile
{
    private final String url;

    public DownloadableResourcePackFile(String url)
    {
        this.url = url;
    }

    /**
     * @return The location of the file download
     */
    public String getUrl()
    {
        return url;
    }

    @Override
    public CompletableFuture<ClientDownload> createDownload(String httpServer, Consumer<ClientDownload> completeListener)
    {
        return ClientDownloadManager.downloadResourcePack(this, this.url, completeListener);
    }

    @Override
    public Component getDisplayName()
    {
        return new TextComponent("resources.zip").withStyle(this.url.startsWith("https") ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    @Override
    public boolean needsRestart()
    {
        return false;
    }

    @Override
    public boolean ignoreErrors()
    {
        return false;
    }
}
