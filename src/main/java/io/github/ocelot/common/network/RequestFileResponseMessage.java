package io.github.ocelot.common.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * @author Ocelot
 */
public class RequestFileResponseMessage
{
    private final String fileName;
    private final boolean success;

    public RequestFileResponseMessage(String fileName, boolean success)
    {
        this.fileName = fileName;
        this.success = success;
    }

    public static void encode(RequestFileResponseMessage msg, PacketBuffer buf)
    {
        buf.writeString(msg.fileName);
        buf.writeBoolean(msg.success);
    }

    public static RequestFileResponseMessage decode(PacketBuffer buf)
    {
        return new RequestFileResponseMessage(buf.readString(), buf.readBoolean());
    }

    @OnlyIn(Dist.CLIENT)
    public String getFileName()
    {
        return fileName;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isSuccess()
    {
        return success;
    }
}