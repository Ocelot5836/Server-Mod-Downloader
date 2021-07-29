package io.github.ocelot.serverdownloader.common.network.handler;

import io.github.ocelot.serverdownloader.client.screen.DownloadModFilesConfirmationScreen;
import io.github.ocelot.serverdownloader.common.download.DownloadableModFile;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import io.github.ocelot.serverdownloader.common.network.ServerDownloaderMessages;
import io.github.ocelot.serverdownloader.common.network.login.NotifyFileStatusMessage;
import io.github.ocelot.serverdownloader.common.network.login.NotifyFileStatusResponseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.net.InetSocketAddress;
import java.util.Set;

/**
 * @author Ocelot
 */
public class DownloaderClientLoginHandler implements IDownloaderLoginClientHandler
{
    @Override
    public void handleNotifyFileStatusMessage(NotifyFileStatusMessage msg, NetworkEvent.Context ctx)
    {
        Set<DownloadableModFile> missingFiles = ModFileManager.getMissingFiles(msg.getFiles());
        ServerDownloaderMessages.LOGIN.reply(new NotifyFileStatusResponseMessage(), ctx);
        if (!missingFiles.isEmpty())
            Minecraft.getInstance().setScreen(new DownloadModFilesConfirmationScreen(getUrl(ctx.getNetworkManager()) + ":" + msg.getPort(), missingFiles));
        ctx.setPacketHandled(true);
    }

    /**
     * Fetches the URL to the specified server. As this is an HT
     *
     * @param networkManager The network manager to get the ip from
     * @return A URL pointing to the server
     */
    private static String getUrl(Connection networkManager)
    {
        if (!(networkManager.getRemoteAddress() instanceof InetSocketAddress))
            throw new IllegalStateException("Failed to create URL to server");
        InetSocketAddress address = (InetSocketAddress) networkManager.getRemoteAddress();
        return "http://" + address.getAddress().getHostAddress() + ":" + (address.getPort() + 1);
    }
}
