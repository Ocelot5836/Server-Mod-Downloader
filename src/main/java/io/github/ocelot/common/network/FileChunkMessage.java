package io.github.ocelot.common.network;

import io.github.ocelot.ServerDownloader;
import io.github.ocelot.common.init.ServerDownloaderMessages;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author Ocelot
 */
public class FileChunkMessage extends LoginMessage
{
    private final String fileName;
    private final byte[] data;

    public FileChunkMessage(String fileName, byte[] data)
    {
        this.fileName = fileName;
        this.data = data;
    }

    public static void sendTo(int chunkSize, Path file, NetworkEvent.Context ctx)
    {
        ServerDownloader.THREAD_EXECUTOR.execute(() ->
        {
            try (FileInputStream is = new FileInputStream(file.toString()))
            {
                byte[] buffer = new byte[chunkSize];
                int chunkLen;
                while ((chunkLen = is.read(buffer)) != -1)
                {
                    ServerDownloaderMessages.LOGIN.reply(new FileChunkMessage(file.getFileName().toString(), Arrays.copyOf(buffer, chunkLen)), ctx);
                }
                ServerDownloaderMessages.LOGIN.reply(new FileCompletionMessage(file.getFileName().toString()), ctx);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    public static void encode(FileChunkMessage msg, PacketBuffer buf)
    {
        buf.writeString(msg.fileName);
        buf.writeByteArray(msg.data);
    }

    public static FileChunkMessage decode(PacketBuffer buf)
    {
        return new FileChunkMessage(buf.readString(), buf.readByteArray());
    }

    @OnlyIn(Dist.CLIENT)
    public String getFileName()
    {
        return fileName;
    }

    @OnlyIn(Dist.CLIENT)
    public byte[] getData()
    {
        return data;
    }
}