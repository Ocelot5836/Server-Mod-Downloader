package io.github.ocelot.common.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class ModFileManager
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ModFile> MOD_FILES = new HashMap<>();

    public static void load()
    {
        MOD_FILES.clear();

        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> ModList.get().getMods().forEach(modInfo -> MOD_FILES.put(modInfo.getModId(), new ModFile(modInfo))));
        DistExecutor.runWhenOn(Dist.DEDICATED_SERVER, () -> () ->
        {
            try
            {
                Path path = Paths.get("client-mods.json");
                if (!Files.exists(path))
                {
                    LOGGER.info(path + " does not exist so it will not be loaded.");
                    return;
                }

                try (FileInputStream inputStream = new FileInputStream(path.toString()))
                {
                    JsonArray json = new JsonParser().parse(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).getAsJsonArray();
                    Set<String> failedMods = new HashSet<>();
                    for (int i = 0; i < json.size(); i++)
                    {
                        String modId = null;
                        try
                        {
                            JsonObject modJson = json.get(i).getAsJsonObject();
                            modId = modJson.get("modId").getAsString();
                            if (MOD_FILES.containsKey(modId))
                                throw new JsonParseException("Duplicate mod '" + modId + "'. Skipping!");
                            if (!(modJson.has("clientOnly") && modJson.get("clientOnly").getAsBoolean()) && !ModList.get().isLoaded(modId))
                                throw new JsonParseException(modId + " does not appear to be a valid or loaded mod. Skipping!");

                            MOD_FILES.put(modId, new ModFile(modId, modJson.get("version").getAsString(), modJson.get("url").getAsString()));
                        }
                        catch (Exception e)
                        {
                            LOGGER.error(modId == null ? ("Failed to load mod at '" + i + "'") : "Failed to load mod '" + modId + "'", e);
                            failedMods.add(modId == null ? ("Unknown " + i) : modId);
                        }
                    }
                    LOGGER.debug("Loaded " + MOD_FILES.size() + " client mod(s)." + (failedMods.isEmpty() ? "" : failedMods.size() + " mod(s) failed to load."));
                }
                catch (Exception e)
                {
                    LOGGER.error("Failed to load '" + path + "'", e);
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Could not initialize mod file manager.", e);
            }
        });
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public static Set<ModFile> getClientMissingFiles(Set<ModFile> clientFiles)
    {
        return MOD_FILES.values().stream().filter(serverFile -> !clientFiles.contains(serverFile) || !MOD_FILES.get(serverFile.getModId()).getVersion().equals(serverFile.getVersion())).collect(Collectors.toSet());
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<ModFile> getMissingFiles(Set<ModFile> serverFiles)
    {
        return serverFiles.stream().filter(serverFile -> !MOD_FILES.containsValue(serverFile) || !MOD_FILES.get(serverFile.getModId()).getVersion().equals(serverFile.getVersion())).collect(Collectors.toSet());
    }

    @Nullable
    public static ModFile getModFile(String modId)
    {
        return MOD_FILES.get(modId);
    }

    public static Collection<ModFile> getFiles()
    {
        return MOD_FILES.values();
    }
}
