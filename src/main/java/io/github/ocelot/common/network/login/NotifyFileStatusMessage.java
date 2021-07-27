package io.github.ocelot.common.network.login;

import io.github.ocelot.common.download.DownloadableModFile;
import io.github.ocelot.common.download.ModFileManager;
import io.github.ocelot.common.network.handler.IDownloaderLoginHandler;
import io.github.ocelot.sonar.common.network.message.SimpleSonarLoginMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ocelot
 */
public class NotifyFileStatusMessage extends SimpleSonarLoginMessage<IDownloaderLoginHandler>
{
    private final Set<DownloadableModFile> files;

    public NotifyFileStatusMessage()
    {
        this.files = new HashSet<>(ModFileManager.getFiles());
    }

    @Override
    public void readPacketData(FriendlyByteBuf buf)
    {
        this.files.clear();

        int size = buf.readVarInt();
        for (int i = 0; i < size; i++)
        {
            try
            {
                this.files.add(buf.readWithCodec(DownloadableModFile.CODEC));
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Failed to read server file from packet", e);
            }
        }
    }

    @Override
    public void writePacketData(FriendlyByteBuf buf)
    {
        buf.writeVarInt(this.files.size());
        for (DownloadableModFile file : this.files)
        {
            try
            {
                buf.writeWithCodec(DownloadableModFile.CODEC, file);
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Failed to write server file to packet", e);
            }
        }
    }

    @Override
    public void processPacket(IDownloaderLoginHandler handler, NetworkEvent.Context ctx)
    {
        handler.handleNotifyFileStatusMessage(this, ctx);
    }

    public Set<DownloadableModFile> getFiles()
    {
        return files;
    }
}