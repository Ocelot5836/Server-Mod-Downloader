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
public class NotifyFileStatusMessage
{
    private final Set<ModFile> serverFiles;

    @OnlyIn(Dist.DEDICATED_SERVER)
    public NotifyFileStatusMessage()
    {
        this(new HashSet<>(ModFileManager.getFiles()));
    }

    private NotifyFileStatusMessage(Set<ModFile> serverFiles)
    {
        this.serverFiles = serverFiles;
    }

    public static void encode(NotifyFileStatusMessage msg, PacketBuffer buf)
    {
        buf.writeVarInt(msg.serverFiles.size());
        msg.serverFiles.forEach(modFile -> modFile.write(buf));
    }

    public static NotifyFileStatusMessage decode(PacketBuffer buf)
    {
        Set<ModFile> files = new HashSet<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++)
            files.add(ModFile.deserialize(buf));

        return new NotifyFileStatusMessage(files);
    }

    @OnlyIn(Dist.CLIENT)
    public Set<ModFile> getServerFiles()
    {
        return serverFiles;
    }

}