package io.github.ocelot.common.network;

import io.github.ocelot.common.download.ModFile;
import io.github.ocelot.common.download.ModFileManager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Ocelot
 */
public class NotifyFileStatusResponseMessage extends LoginMessage
{
    private final Set<ModFile> clientFiles;

    @OnlyIn(Dist.CLIENT)
    public NotifyFileStatusResponseMessage()
    {
        this(new HashSet<>(ModFileManager.getFiles()));
    }

    private NotifyFileStatusResponseMessage(Set<ModFile> clientFiles)
    {
        this.clientFiles = clientFiles;
    }

    public static void encode(NotifyFileStatusResponseMessage msg, PacketBuffer buf)
    {
        buf.writeVarInt(msg.clientFiles.size());
        msg.clientFiles.forEach(modFile -> modFile.write(buf));
    }

    public static NotifyFileStatusResponseMessage decode(PacketBuffer buf)
    {
        Set<ModFile> files = new HashSet<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++)
            files.add(ModFile.deserialize(buf));

        return new NotifyFileStatusResponseMessage(files);
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public Set<ModFile> getClientFiles()
    {
        return clientFiles;
    }
}