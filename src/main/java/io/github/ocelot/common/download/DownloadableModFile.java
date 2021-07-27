package io.github.ocelot.common.download;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Arrays;
import java.util.Objects;

/**
 * <p>A file that can be downloaded from the server.</p>
 *
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

    /**
     * @return The mods in the file
     */
    public String[] getModIds()
    {
        return modIds;
    }

    /**
     * @return A visual representation of the mods in the file
     */
    public String getVisualMods()
    {
        return String.join(", ", this.modIds);
    }

    /**
     * @return The SHA1 hash of the file on the server
     */
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
        return "ModFile{Mods: " + this.getVisualMods() + ", Hash: " + this.hash + "}";
    }
}
