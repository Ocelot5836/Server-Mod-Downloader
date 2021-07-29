package io.github.ocelot.serverdownloader.common.network.login;

import io.github.ocelot.serverdownloader.common.download.DownloadableModFile;
import io.github.ocelot.serverdownloader.common.download.ModFileManager;
import io.github.ocelot.serverdownloader.common.network.handler.IDownloaderLoginClientHandler;
import io.github.ocelot.serverdownloader.server.ServerConfig;
import io.github.ocelot.sonar.common.network.message.SimpleSonarLoginMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Ocelot
 */
public class NotifyFileStatusMessage extends SimpleSonarLoginMessage<IDownloaderLoginClientHandler>
{
    private final Set<DownloadableModFile> files;
    private int port;

    public NotifyFileStatusMessage()
    {
        this.files = new HashSet<>(ModFileManager.getFiles());
        this.port = ServerConfig.INSTANCE.httpServerPort.get();
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
                DownloadableModFile modFile = buf.readWithCodec(DownloadableModFile.CODEC);
                if (modFile.getModIds().length > 0)
                    this.files.add(modFile);
            }
            catch (Exception e)
            {
                throw new IllegalStateException("Failed to read server file from packet", e);
            }
        }
        this.port = buf.readVarInt();
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
            catch (Exception e)
            {
                throw new IllegalStateException("Failed to write server file to packet", e);
            }
        }
        buf.writeVarInt(this.port);
    }

    @Override
    public void processPacket(IDownloaderLoginClientHandler handler, NetworkEvent.Context ctx)
    {
        handler.handleNotifyFileStatusMessage(this, ctx);
    }

    /**
     * @return The files sent by the other side
     */
    @OnlyIn(Dist.CLIENT)
    public Set<DownloadableModFile> getFiles()
    {
        return files;
    }

    /**
     * @return The port to retrieve server files on
     */
    @OnlyIn(Dist.CLIENT)
    public int getPort()
    {
        return port;
    }
}