package io.github.ocelot.common.download;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author Ocelot
 */
public class ModFile
{
    private final String modId;
    private final String version;
    private final String url;

    public ModFile(String modId, String version, String url)
    {
        this.modId = modId;
        this.version = version;
        this.url = url;
    }

    public ModFile(ModInfo modInfo)
    {
        this.modId = modInfo.getModId();
        this.version = modInfo.getVersion().toString();
        this.url = null;
    }

    public void write(PacketBuffer buf)
    {
        buf.writeString(this.modId);
        buf.writeString(this.version);
        buf.writeBoolean(this.url != null);
        if (this.url != null)
            buf.writeString(this.url);
    }

    public String getModId()
    {
        return modId;
    }

    public String getVersion()
    {
        return version;
    }

    @Nullable
    public String getUrl()
    {
        return url;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModFile modFile = (ModFile) o;
        return modId.equals(modFile.modId) &&
                version.equals(modFile.version);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(modId, version);
    }

    @Override
    public String toString()
    {
        return "ModFile{" + this.modId + " v" + this.version + ", URL: " + this.url + "}";
    }

    public static ModFile deserialize(PacketBuffer buf)
    {
        return new ModFile(buf.readString(32767), buf.readString(32767), buf.readBoolean() ? buf.readString(32767) : null);
    }
}
