package io.github.ocelot.common.network.handler;

import io.github.ocelot.common.download.ModFileManager;
import io.github.ocelot.common.init.ServerDownloaderMessages;
import io.github.ocelot.common.network.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author Ocelot
 */
public class ServerMessageHandler implements MessageHandler
{
    public static final MessageHandler INSTANCE = new ServerMessageHandler();

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
        Set<String> missingFiles = ModFileManager.getMissingFiles(msg.getFiles());
        if (!missingFiles.isEmpty())
        {
            if (msg.isRequestingFiles())
            {
                Path path = Paths.get(ModFileManager.FOLDER_NAME);
                missingFiles.forEach(fileName ->
                {
                    Path file = path.resolve(fileName);
                    if (!Files.exists(file))
                    {
                        ServerDownloaderMessages.LOGIN.reply(new RequestFileResponseMessage(fileName, false), ctx.get());
                        return;
                    }
                    ServerDownloaderMessages.LOGIN.reply(new RequestFileResponseMessage(fileName, true), ctx.get());
                    FileChunkMessage.sendTo(1024, Paths.get(ModFileManager.FOLDER_NAME, fileName), ctx.get());
                    ctx.get().setPacketHandled(true);
                });
                ServerDownloaderMessages.LOGIN.reply(new DownloadCompletionMessage(), ctx.get());
                ctx.get().getNetworkManager().closeChannel(new StringTextComponent("Client needs to restart to apply files."));
            }
            else
            {
                ctx.get().getNetworkManager().closeChannel(new StringTextComponent("Client refused to download files."));
            }
        }
        ctx.get().setPacketHandled(true);
    }

    @Override
    public void handleRequestFileResponseMessage(RequestFileResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        throw new UnsupportedOperationException("Server should not respond to file request.");
    }

    @Override
    public void handleFileChunkMessage(FileChunkMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        throw new UnsupportedOperationException("Server should not receive file chunks.");
    }

    @Override
    public void handleFileCompletionMessage(FileCompletionMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        throw new UnsupportedOperationException("Server should not receive file completion.");
    }

    @Override
    public void handleDownloadCompletionMessage(DownloadCompletionMessage msg, Supplier<NetworkEvent.Context> ctx)
    {
        throw new UnsupportedOperationException("Server should not receive file completion.");
    }

//    @Override
//    public void handleRequestFileMessage(RequestFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        ctx.get().enqueueWork(() ->
//        {
//            String serverHash = ModFileManager.getHash(msg.getName());
//            if (serverHash != null && serverHash.equals(msg.getHash()))
//            {
//                ServerDownloaderMessages.INSTANCE.reply(new RequestFileStatusResponseMessage(true), ctx.get());
//                return;
//            }
//            ServerDownloaderMessages.INSTANCE.reply(new RequestFileStatusResponseMessage(false), ctx.get());
//        });
//        ctx.get().setPacketHandled(true);
//    }
//
//    @Override
//    public void handleRequestFileStatusResponseMessage(RequestFileStatusResponseMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        throw new IllegalArgumentException("Client should not send file request response to server.");
//    }
//
//    @Override
//    public void handleSendFileStatusMessage(SendFileStatusMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        throw new IllegalArgumentException("Client should not send file status to server.");
//    }
//
//    @Override
//    public void handleSendFileMessage(SendFileMessage msg, Supplier<NetworkEvent.Context> ctx)
//    {
//        throw new IllegalArgumentException("Client should not send files to server.");
//    }
}
