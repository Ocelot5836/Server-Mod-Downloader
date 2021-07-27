package io.github.ocelot.common.download;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Ocelot
 */
public class DownloadableModFile
{
    public static final Codec<DownloadableModFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("mod_ids").forGetter(file -> Arrays.asList(file.getModIds())),
            Codec.STRING.fieldOf("hash").forGetter(DownloadableModFile::getHash)
    ).apply(instance, (modIds, hash) -> new DownloadableModFile(modIds.toArray(new String[0]), hash)));

    private final String[] modIds;
    private final String hash;

    public DownloadableModFile(String[] modIds, String hash)
    {
        this.modIds = modIds;
        this.hash = hash;
    }

    public String[] getModIds()
    {
        return modIds;
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
        DownloadableModFile that = (DownloadableModFile) o;
        return Arrays.equals(this.modIds, that.modIds) && this.hash.equals(that.hash);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(this.hash);
        result = 31 * result + Arrays.hashCode(this.modIds);
        return result;
    }

    @Override
    public String toString()
    {
        return "ModFile{Mods: " + Arrays.toString(this.modIds) + ", Hash: " + this.hash + "}";
    }
}
