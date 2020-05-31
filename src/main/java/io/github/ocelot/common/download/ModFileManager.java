package io.github.ocelot.common.download;

import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class ModFileManager
{
    public static final String FOLDER_NAME = DistExecutor.runForDist(() -> () -> "mods", () -> () -> "client-mods");

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, String> HASHES = new HashMap<>();

    private static void addHash(Path path) throws IOException
    {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toString())))
        {
            HASHES.put(path.getFileName().toString(), DigestUtils.sha1Hex(bis));
        }
    }

    public static void start()
    {
        try
        {
            Path path = Paths.get(FOLDER_NAME);
            Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);

            Files.list(path).forEach(childPath ->
            {
                if (Files.isDirectory(childPath))
                    return;

                try
                {
                    addHash(childPath);
                    LOGGER.debug("Loaded '" + childPath + "' client mod file.");
                }
                catch (Exception e)
                {
                    LOGGER.error("Failed to add hash for '" + childPath + "'", e);
                }
            });
        }
        catch (Exception e)
        {
            LOGGER.error("Could not initialize mod file manager.", e);
        }
    }

    public static Set<String> getMissingFiles(Set<Map.Entry<String, String>> others)
    {
        return HASHES.entrySet().stream().filter(entry -> !others.contains(entry)).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    public static Set<String> getAdditionalFiles(Set<Map.Entry<String, String>> others)
    {
        return others.stream().filter(entry ->
        {
            String hash = getHash(entry.getKey());
            return hash == null || !hash.equals(entry.getValue());
        }).map(Map.Entry::getKey).collect(Collectors.toSet());
    }

    @Nullable
    public static String getHash(String fileName)
    {
        return HASHES.get(fileName);
    }

    public static Set<Map.Entry<String, String>> getFiles()
    {
        return HASHES.entrySet();
    }

    public static void stop()
    {
        HASHES.clear();
    }

    public static void reload()
    {
        stop();
        start();
    }
}
