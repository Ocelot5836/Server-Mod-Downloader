package io.github.ocelot.serverdownloader.common.network.handler;

import io.github.ocelot.serverdownloader.common.network.login.ServerboundNotifyFileStatusResponseMessage;
import net.minecraftforge.fml.network.NetworkEvent;

/**
 * @author Ocelot
 */
public interface IDownloaderLoginServerHandler
{
    /**
     * Notifies both the server of the client mods.
     *
     * @param msg The message instance
     * @param ctx The context of the message
     */
    void handleNotifyFileStatusResponseMessage(ServerboundNotifyFileStatusResponseMessage msg, NetworkEvent.Context ctx);
}
