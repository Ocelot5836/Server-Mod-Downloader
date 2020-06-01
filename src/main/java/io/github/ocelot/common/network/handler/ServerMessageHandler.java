package io.github.ocelot.common.network.handler;

import io.github.ocelot.common.download.ModFile;
import io.github.ocelot.common.download.ModFileManager;
import io.github.ocelot.common.network.NotifyFileStatusMessage;
import io.github.ocelot.common.network.NotifyFileStatusResponseMessage;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Ocelot
 */
public class ServerMessageHandler implements MessageHandler
{
    public static final MessageHandler INSTANCE = new ServerMessageHandler();

    private static final Logger LOGGER = LogManager.getLogger();

    private ServerMessageHandler()
    {
    }

    @Override
    public void handleNotifyFileStatusMessage(NotifyFileStatusMessage msg, Supplier<NetworkEvent.Context> contextSupplier)
    {
        throw new IllegalArgumentException("Client should not send file status to server.");
    }

    @Override
    public void handleNotifyFileStatusResponseMessage(NotifyFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        Set<ModFile> missingFiles = ModFileManager.getClientMissingFiles(msg.getClientFiles());
        LOGGER.debug("Client returned mods '" + msg.getClientFiles() + "'." + (missingFiles.isEmpty() ? "" : " " + missingFiles.size() + " were missing and require download."));
        if (!missingFiles.isEmpty())
            ctx.get().getNetworkManager().closeChannel(new StringTextComponent("Client missing mods: " + missingFiles));
        ctx.get().setPacketHandled(true);
    }
}
