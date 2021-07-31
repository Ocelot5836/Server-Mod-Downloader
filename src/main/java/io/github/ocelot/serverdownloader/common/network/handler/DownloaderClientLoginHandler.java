package io.github.ocelot.serverdownloader.common.network.handler;

import io.github.ocelot.serverdownloader.client.download.ClientDownloadManager;
import io.github.ocelot.serverdownloader.client.download.DownloadableResourcePackFile;
import io.github.ocelot.serverdownloader.client.screen.DownloadModFilesConfirmationScreen;
import io.github.ocelot.serverdownloader.client.screen.DownloadModFilesScreen;
import io.github.ocelot.serverdownloader.common.download.DownloadableFile;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import io.github.ocelot.serverdownloader.common.network.ServerDownloaderMessages;
import io.github.ocelot.serverdownloader.common.network.login.ClientboundNotifyFileStatusMessage;
import io.github.ocelot.serverdownloader.common.network.login.ServerboundNotifyFileStatusResponseMessage;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Ocelot
 */
public class DownloaderClientLoginHandler implements IDownloaderLoginClientHandler
{
    @Override
    public void handleNotifyFileStatusMessage(ClientboundNotifyFileStatusMessage msg, NetworkEvent.Context ctx)
    {
        List<DownloadableFile> missingFiles = new ArrayList<>(ModFileManager.getMissingFiles(msg.getFiles()));
        String httpServer = getUrl(ctx.getNetworkManager()) + ":" + msg.getPort();
        ServerData server = Minecraft.getInstance().getCurrentServer();
        String resourcePack = msg.getResourcePack().startsWith("level://") ? "http://" + ((InetSocketAddress) ctx.getNetworkManager().getRemoteAddress()).getAddress().getHostAddress() + ":" + msg.getPort() + "/resources.zip" : msg.getResourcePack();

        if (!StringUtil.isNullOrEmpty(msg.getResourcePack()) && validateResourcePackUrl(resourcePack) && !ClientDownloadManager.isResourcePackDownloaded(resourcePack, msg.getResourcePackHash()))
        {
            if (server != null && server.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED)
            {
                ctx.getNetworkManager().disconnect(new TranslatableComponent("connect.aborted"));
                missingFiles.add(new DownloadableResourcePackFile(resourcePack));
                Minecraft.getInstance().setScreen(missingFiles.size() == 1 ? new DownloadModFilesScreen(server, httpServer, missingFiles) : new DownloadModFilesConfirmationScreen(server, httpServer, missingFiles));
            }
            else if (server != null && server.getResourcePackStatus() != ServerData.ServerPackStatus.PROMPT)
            {
                if (!missingFiles.isEmpty())
                {
                    ctx.getNetworkManager().disconnect(new TranslatableComponent("connect.aborted"));
                    Minecraft.getInstance().setScreen(new DownloadModFilesConfirmationScreen(server, httpServer, missingFiles));
                }
            }
            else
            {
                ctx.getNetworkManager().disconnect(new TranslatableComponent("connect.aborted"));
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(new ConfirmScreen(accepted ->
                {
                    if (accepted)
                    {
                        if (server != null)
                            server.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);

                        missingFiles.add(new DownloadableResourcePackFile(resourcePack));
                        Minecraft.getInstance().setScreen(missingFiles.size() == 1 ? new DownloadModFilesScreen(server, httpServer, missingFiles) : new DownloadModFilesConfirmationScreen(server, httpServer, missingFiles));
                    }
                    else
                    {
                        if (server != null)
                            server.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);

                        if (!missingFiles.isEmpty())
                        {
                            Minecraft.getInstance().setScreen(new DownloadModFilesConfirmationScreen(server, httpServer, missingFiles));
                        }
                        else if (server != null)
                        {
                            Minecraft.getInstance().setScreen(new ConnectScreen(new JoinMultiplayerScreen(new TitleScreen()), Minecraft.getInstance(), server));
                        }
                        else
                        {
                            Minecraft.getInstance().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
                        }
                    }

                    ServerList.saveSingleServer(server);
                }, new TranslatableComponent("multiplayer.texturePrompt.line1"), new TranslatableComponent("multiplayer.texturePrompt.line2"))));
            }
        }
        else if (!missingFiles.isEmpty())
        {
            Minecraft.getInstance().setScreen(new DownloadModFilesConfirmationScreen(server, httpServer, missingFiles));
        }

        ServerDownloaderMessages.LOGIN.reply(new ServerboundNotifyFileStatusResponseMessage(), ctx);
        ctx.setPacketHandled(true);
    }

    private static boolean validateResourcePackUrl(String url)
    {
        try
        {
            URI uri = new URI(url);
            String s = uri.getScheme();
            if (!"http".equals(s) && !"https".equals(s))
                throw new URISyntaxException(url, "Wrong protocol");
            return true;
        }
        catch (URISyntaxException ignored)
        {
            return false;
        }
    }

    private static String getUrl(Connection networkManager)
    {
        if (!(networkManager.getRemoteAddress() instanceof InetSocketAddress))
            throw new IllegalStateException("Failed to create URL to server");
        InetSocketAddress address = (InetSocketAddress) networkManager.getRemoteAddress();
        return "http://" + address.getAddress().getHostAddress();
    }
}
