package io.github.ocelot.common.download;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class ModFileManager
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, DownloadableModFile> MOD_FILES = new HashMap<>();

    public static void load()
    {
        MOD_FILES.clear();

        ModList.get().getModFiles().forEach(info ->
        {
            ModFile file = info.getFile();
            if (!Files.isRegularFile(file.getFilePath()))
                return;
            try (FileInputStream is = new FileInputStream(file.getFilePath().toFile()))
            {
                String hash = DigestUtils.sha1Hex(is);
                for (IModInfo modInfo : info.getMods())
                    MOD_FILES.put(modInfo.getModId(), new DownloadableModFile(modInfo.getModId(), hash));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to read mod file: " + file.getFileName(), e);
            }
        });
//        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ModList.get().getMods().forEach(modInfo -> MOD_FILES.put(modInfo.getModId(), new ModFile(modInfo))));
//        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> () ->
//        {
//            try
//            {
//                Path path = Paths.get("client-mods.json");
//                if (!Files.exists(path))
//                {
//                    LOGGER.info(path + " does not exist so it will not be loaded.");
//                    return;
//                }
//
//                try (FileInputStream inputStream = new FileInputStream(path.toString()))
//                {
//                    JsonArray json = new JsonParser().parse(IOUtils.toString(inputStream, StandardCharsets.UTF_8)).getAsJsonArray();
//                    Set<String> failedMods = new HashSet<>();
//                    for (int i = 0; i < json.size(); i++)
//                    {
//                        String modId = null;
//                        try
//                        {
//                            JsonObject modJson = json.get(i).getAsJsonObject();
//                            modId = modJson.get("modId").getAsString();
//                            if (MOD_FILES.containsKey(modId))
//                                throw new JsonParseException("Duplicate mod '" + modId + "'. Skipping!");
//                            if (!(modJson.has("clientOnly") && modJson.get("clientOnly").getAsBoolean()) && !ModList.get().isLoaded(modId))
//                                throw new JsonParseException(modId + " does not appear to be a valid or loaded mod. Skipping!");
//
//                            MOD_FILES.put(modId, new ModFile(modId, modJson.get("version").getAsString(), modJson.get("url").getAsString()));
//                        }
//                        catch (Exception e)
//                        {
//                            LOGGER.error(modId == null ? ("Failed to load mod at '" + i + "'") : "Failed to load mod '" + modId + "'", e);
//                            failedMods.add(modId == null ? ("Unknown " + i) : modId);
//                        }
//                    }
//                    LOGGER.debug("Loaded " + MOD_FILES.size() + " client mod(s)." + (failedMods.isEmpty() ? "" : failedMods.size() + " mod(s) failed to load."));
//                }
//                catch (Exception e)
//                {
//                    LOGGER.error("Failed to load '" + path + "'", e);
//                }
//            }
//            catch (Exception e)
//            {
//                LOGGER.error("Could not initialize mod file manager.", e);
//            }
//        });
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public static Set<DownloadableModFile> getClientMissingFiles(Set<DownloadableModFile> clientFiles)
    {
        return MOD_FILES.values().stream().filter(serverFile -> !clientFiles.contains(serverFile) || !MOD_FILES.get(serverFile.getModId()).getHash().equals(serverFile.getHash())).collect(Collectors.toSet());
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<DownloadableModFile> getMissingFiles(Set<DownloadableModFile> serverFiles)
    {
        return serverFiles.stream().filter(serverFile -> !MOD_FILES.containsValue(serverFile) || !MOD_FILES.get(serverFile.getModId()).getHash().equals(serverFile.getHash())).collect(Collectors.toSet());
    }

    @Nullable
    public static DownloadableModFile getModFile(String modId)
    {
        return MOD_FILES.get(modId);
    }

    public static Collection<DownloadableModFile> getFiles()
    {
        return MOD_FILES.values();
    }
}
