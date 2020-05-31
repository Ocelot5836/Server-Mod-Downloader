package io.github.ocelot.common.network;

import io.github.ocelot.common.download.ModFileManager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Ocelot
 */
public class NotifyFileStatusResponseMessage extends LoginMessage
{
    private final Map<String, String> files;
    private final boolean requestFiles;

    @OnlyIn(Dist.CLIENT)
    public NotifyFileStatusResponseMessage(boolean requestFiles)
    {
        this(new HashMap<>(), requestFiles);
        ModFileManager.getFiles().forEach(entry -> this.files.put(entry.getKey(), entry.getValue()));
    }

    private NotifyFileStatusResponseMessage(Map<String, String> files, boolean requestFiles)
    {
        this.files = files;
        this.requestFiles = requestFiles;
    }

    public static void encode(NotifyFileStatusResponseMessage msg, PacketBuffer buf)
    {
        buf.writeVarInt(msg.files.size());
        msg.files.forEach((key, value) ->
        {
            buf.writeString(key);
            buf.writeString(value);
        });
        buf.writeBoolean(msg.requestFiles);
    }

    public static NotifyFileStatusResponseMessage decode(PacketBuffer buf)
    {
        Map<String, String> files = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++)
            files.put(buf.readString(32767), buf.readString(32767));

        return new NotifyFileStatusResponseMessage(files, buf.readBoolean());
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public Set<Map.Entry<String, String>> getFiles()
    {
        return files.entrySet();
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public boolean isRequestingFiles()
    {
        return requestFiles;
    }
}