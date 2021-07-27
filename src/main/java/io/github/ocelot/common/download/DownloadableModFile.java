package io.github.ocelot.common.download;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Ocelot
 */
public class DownloadableModFile
{
    public static final Codec<DownloadableModFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("mod_id").forGetter(DownloadableModFile::getModId),
            Codec.STRING.fieldOf("hash").forGetter(DownloadableModFile::getHash)
    ).apply(instance, DownloadableModFile::new));

    private final String modId;
    private final String hash;

    public DownloadableModFile(String modId, String hash)
    {
        this.modId = modId;
        this.hash = hash;
    }

    public String getModId()
    {
        return modId;
    }

    public String getHash()
    {
        return hash;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownloadableModFile modFile = (DownloadableModFile) o;
        return modId.equals(modFile.modId) &&
                hash.equals(modFile.hash);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(modId, hash);
    }

    @Override
    public String toString()
    {
        return "ModFile{" + this.modId + ", Hash: " + this.hash + "}";
    }
}
