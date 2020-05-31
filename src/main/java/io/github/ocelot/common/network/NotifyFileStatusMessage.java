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
public class NotifyFileStatusMessage
{
    private final Map<String, String> files;

    @OnlyIn(Dist.DEDICATED_SERVER)
    public NotifyFileStatusMessage()
    {
        this(new HashMap<>());
        ModFileManager.getFiles().forEach(entry -> this.files.put(entry.getKey(), entry.getValue()));
    }

    private NotifyFileStatusMessage(Map<String, String> files)
    {
        this.files = files;
    }

    public static void encode(NotifyFileStatusMessage msg, PacketBuffer buf)
    {
        buf.writeVarInt(msg.files.size());
        msg.files.forEach((key, value) ->
        {
            buf.writeString(key);
            buf.writeString(value);
        });
    }

    public static NotifyFileStatusMessage decode(PacketBuffer buf)
    {
        Map<String, String> files = new HashMap<>();
        int size = buf.readVarInt();
        for (int i = 0; i < size; i++)
            files.put(buf.readString(), buf.readString());

        return new NotifyFileStatusMessage(files);
    }

    @OnlyIn(Dist.CLIENT)
    public Set<Map.Entry<String, String>> getFiles()
    {
        return files.entrySet();
    }

}