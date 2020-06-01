package io.github.ocelot.common.network.handler;

import io.github.ocelot.client.screen.DownloadModFilesScreen;
import io.github.ocelot.common.download.ModFile;
import io.github.ocelot.common.download.ModFileManager;
import io.github.ocelot.common.init.ServerDownloaderMessages;
import io.github.ocelot.common.network.NotifyFileStatusMessage;
import io.github.ocelot.common.network.NotifyFileStatusResponseMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Ocelot
 */
public class ClientMessageHandler implements MessageHandler
{
    public static final MessageHandler INSTANCE = new ClientMessageHandler();

    private ClientMessageHandler()
    {
    }

    @Override
    public void handleNotifyFileStatusMessage(NotifyFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        Set<ModFile> missingFiles = ModFileManager.getMissingFiles(msg.getServerFiles());
        ServerDownloaderMessages.LOGIN.reply(new NotifyFileStatusResponseMessage(), ctx.get());
        if (!missingFiles.isEmpty())
        {
            ctx.get().getNetworkManager().closeChannel(new StringTextComponent("Missing mod files: " + missingFiles)); // TODO lang
            Minecraft.getInstance().displayGuiScreen(new DownloadModFilesScreen(Minecraft.getInstance().currentScreen));
        }
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void handleNotifyFileStatusResponseMessage(NotifyFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        throw new UnsupportedOperationException("Server should not acknowledge client.");
    }
}
