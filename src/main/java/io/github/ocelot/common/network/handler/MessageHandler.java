package io.github.ocelot.common.network.handler;

import io.github.ocelot.common.network.*;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * @author Ocelot
 */
public interface MessageHandler
{
    void handleNotifyFileStatusMessage(NotifyFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx);

    void handleNotifyFileStatusResponseMessage(NotifyFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx);

//    @Deprecated
//    void handleRequestFileMessage(RequestFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx);
//
//    @Deprecated
//    void handleRequestFileStatusResponseMessage(RequestFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx);
//
//    @Deprecated
//    void handleSendFileStatusMessage(SendFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx);
//
//    @Deprecated
//    void handleSendFileMessage(SendFileMessage msg, Supplier<NetworkEvent.Context> ctx);
}