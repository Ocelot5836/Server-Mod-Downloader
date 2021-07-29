package io.github.ocelot.server;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.github.ocelot.ServerDownloader;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ocelot
 */
public class BlacklistedServerModLoader extends SimplePreparableReloadListener<Set<String>>
{
    public static final BlacklistedServerModLoader INSTANCE = new BlacklistedServerModLoader();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Path LOCATION = Paths.get("server-mods.json");
    private static final Set<String> INVALID_MODS;

    static
    {
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<>();
        builder.add("minecraft", "forge", ServerDownloader.MOD_ID);
        ModList.get().forEachModContainer((modId, modContainer) ->
        {
            if (modContainer.getCustomExtension(ExtensionPoint.DISPLAYTEST).map(pair -> FMLNetworkConstants.IGNORESERVERONLY.equals(pair.getLeft().get())).orElse(false))
                builder.add(modId);
        });
        INVALID_MODS = builder.build();
    }

    private final Set<String> blacklistedMods = new HashSet<>();

    @Override
    protected Set<String> prepare(ResourceManager resourceManager, ProfilerFiller profiler)
    {
        if (!Files.exists(LOCATION))
            return Collections.emptySet();

        Set<String> mods = new HashSet<>();

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(LOCATION.toFile())))
        {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array)
            {
                mods.add(element.getAsString());
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to read server only mods from: " + LOCATION, e);
        }

        return mods;
    }

    @Override
    protected void apply(Set<String> object, ResourceManager arg, ProfilerFiller arg2)
    {
        this.blacklistedMods.clear();
        this.blacklistedMods.addAll(INVALID_MODS);
        this.blacklistedMods.addAll(object);
    }

    /**
     * Checks to see if the specified mod is valid to be sent to the client.
     *
     * @param modId The mod id to check
     * @return Whether or not that mod can be sent
     */
    public static boolean isValid(String modId)
    {
        return !INSTANCE.blacklistedMods.contains(modId);
    }
}
