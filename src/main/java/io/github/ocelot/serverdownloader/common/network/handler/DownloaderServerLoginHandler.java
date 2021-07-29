package io.github.ocelot.serverdownloader.common.network.handler;

import io.github.ocelot.serverdownloader.common.network.login.NotifyFileStatusMessage;
import io.github.ocelot.serverdownloader.common.download.DownloadableModFile;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * @author Ocelot
 */
public class DownloaderServerLoginHandler implements IDownloaderLoginHandler
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handleNotifyFileStatusMessage(NotifyFileStatusMessage msg, NetworkEvent.Context ctx)
    {
        Set<DownloadableModFile> missingFiles = ModFileManager.getClientMissingFiles(msg.getFiles());
        LOGGER.debug("Client returned mods '" + msg.getFiles() + "'." + (missingFiles.isEmpty() ? "" : " " + missingFiles.size() + " were missing and require download."));
        if (!missingFiles.isEmpty())
            ctx.getNetworkManager().disconnect(new TextComponent("Client missing mods: " + missingFiles));
        ctx.setPacketHandled(true);
    }
}
