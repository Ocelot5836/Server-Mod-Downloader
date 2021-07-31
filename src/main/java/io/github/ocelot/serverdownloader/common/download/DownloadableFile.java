package io.github.ocelot.serverdownloader.common.download;

import io.github.ocelot.serverdownloader.client.download.ClientDownload;
import io.github.ocelot.serverdownloader.client.screen.DownloadModFilesScreen;
import net.minecraft.network.chat.Component;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * <p>A file downloadable through {@link DownloadModFilesScreen}.</p>
 *
 * @author Ocelot
 */
public interface DownloadableFile
{
    /**
     * Creates a new download request for this file.
     *
     * @param httpServer       The HTTP server to use for the server host
     * @param completeListener The listener for completion results
     * @return A future for when this file exists
     */
    CompletableFuture<ClientDownload> createDownload(String httpServer, Consumer<ClientDownload> completeListener);

    /**
     * @return The name to display for this file in the GUI
     */
    Component getDisplayName();

    /**
     * @return Whether the game needs to be restarted before joining the server
     */
    boolean needsRestart();
}
