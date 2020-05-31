package io.github.ocelot.common.network.handler;

import io.github.ocelot.client.ClientModDownloader;
import io.github.ocelot.client.screen.DownloadModFilesScreen;
import io.github.ocelot.common.download.ModFileManager;
import io.github.ocelot.common.init.ServerDownloaderMessages;
import io.github.ocelot.common.network.*;
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
        Set<String> missingFiles = ModFileManager.getAdditionalFiles(msg.getFiles());
        if (!missingFiles.isEmpty())
        {
            Minecraft.getInstance().displayGuiScreen(new DownloadModFilesScreen(Minecraft.getInstance().currentScreen, success ->
            {
                ServerDownloaderMessages.LOGIN.reply(new NotifyFileStatusResponseMessage(success), ctx.get());
                if (!success)
                {
                    ctx.get().getNetworkManager().closeChannel(new StringTextComponent("Refused to download server files."));
                }
            }));
        }
        else
        {
            ServerDownloaderMessages.LOGIN.reply(new NotifyFileStatusResponseMessage(false), ctx.get());
        }
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void handleNotifyFileStatusResponseMessage(NotifyFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        throw new UnsupportedOperationException("Server should not acknowledge client.");
    }

    @Override
    public void handleRequestFileResponseMessage(RequestFileResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        if (msg.isSuccess())
            ClientModDownloader.prepare(msg.getFileName());
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void handleFileChunkMessage(FileChunkMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        ClientModDownloader.receive(msg.getFileName(), msg.getData());
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void handleFileCompletionMessage(FileCompletionMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        // TODO notify screen of successful or unsuccessful file download
        ClientModDownloader.complete(msg.getFileName());
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void handleDownloadCompletionMessage(DownloadCompletionMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().getNetworkManager().closeChannel(new StringTextComponent("A restart is needed to apply files."));
        ctx.get().setPacketHandled(true);
        // TODO notify screen that a restart is required
        Minecraft.getInstance().shutdown();
    }

//    @Override
//    public void handleRequestFileMessage(RequestFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        throw new UnsupportedOperationException("Server should not request client for files.");
//    }
//
//    @Override
//    public void handleRequestFileStatusResponseMessage(RequestFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        ctx.get().enqueueWork(() ->
//        {
//
//        });
//        ctx.get().setPacketHandled(true);
//    }
//
//    @Override
//    public void handleSendFileStatusMessage(SendFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        ctx.get().enqueueWork(() ->
//        {
//        });
//        ctx.get().setPacketHandled(true);
//    }
//
//    @Override
//    public void handleSendFileMessage(SendFileMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        ctx.get().enqueueWork(() ->
//        {
//            System.out.println("Received '" + new String(msg.getBytes()) + "' from server.");
//            // TODO collect bytes received in some sort of queue until the end is reached
//        });
//        ctx.get().setPacketHandled(true);
//    }
}
