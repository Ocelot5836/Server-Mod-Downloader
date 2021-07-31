package io.github.ocelot.serverdownloader.common.download;

import io.github.ocelot.serverdownloader.client.download.ClientDownload;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface DownloadableFile
{
    CompletableFuture<ClientDownload> createDownload(String httpServer, Consumer<ClientDownload> completeListener);

    Component getDisplayName();

    boolean needsRestart();
}
