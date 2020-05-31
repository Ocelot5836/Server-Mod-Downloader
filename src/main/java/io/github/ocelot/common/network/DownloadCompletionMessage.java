package io.github.ocelot.common.network;

import net.minecraft.network.PacketBuffer;

/**
 * @author Ocelot
 */
public class DownloadCompletionMessage
{
    public DownloadCompletionMessage()
    {
    }

    public static void encode(DownloadCompletionMessage msg, PacketBuffer buf)
    {
    }

    public static DownloadCompletionMessage decode(PacketBuffer buf)
    {
        return new DownloadCompletionMessage();
    }
}