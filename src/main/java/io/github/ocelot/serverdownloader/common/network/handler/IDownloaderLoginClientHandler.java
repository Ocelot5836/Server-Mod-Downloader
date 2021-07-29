package io.github.ocelot.serverdownloader.common.network.handler;

import io.github.ocelot.serverdownloader.common.network.login.NotifyFileStatusMessage;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * @author Ocelot
 */
public interface IDownloaderLoginClientHandler
{
    /**
     * Notifies both the client and server of each other's mods and their mod file hashes.
     *
     * @param msg The message instance
     * @param ctx The context of the message
     */
    void handleNotifyFileStatusMessage(NotifyFileStatusMessage msg, NetworkEvent.Context ctx);
}
