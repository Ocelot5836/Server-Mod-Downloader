package io.github.ocelot.common.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author Ocelot
 */
public class FileCompletionMessage
{
    private final String fileName;

    public FileCompletionMessage(String fileName)
    {
        this.fileName = fileName;
    }

    public static void encode(FileCompletionMessage msg, PacketBuffer buf)
    {
        buf.writeString(msg.fileName);
    }

    public static FileCompletionMessage decode(PacketBuffer buf)
    {
        return new FileCompletionMessage(buf.readString());
    }

    @OnlyIn(Dist.CLIENT)
    public String getFileName()
    {
        return fileName;
    }

}