package io.github.ocelot.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ocelot
 */
@OnlyIn(Dist.CLIENT)
public class ClientModDownloader
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, FileOutputStream> DOWNLOADING = new HashMap<>();

    public static boolean prepare(String fileName)
    {
        try
        {
            Path path = Paths.get(Minecraft.getInstance().gameDirectory.getPath(), "client-mod-downloads");
            Files.createDirectories(Files.exists(path) ? path.toRealPath() : path);

            Path file = path.resolve(fileName);
            if (!Files.exists(file))
                Files.createFile(file);

            DOWNLOADING.put(fileName, new FileOutputStream(file.toString()));
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to initialize download for '" + fileName + "'.", e);
            return false;
        }
    }

    public static boolean receive(String fileName, byte[] data)
    {
        if (!DOWNLOADING.containsKey(fileName))
        {
            LOGGER.warn("Cannot receive file data for '" + fileName + "' as it has not yet begun to download.");
            return false;
        }
        try
        {
            DOWNLOADING.get(fileName).write(data);
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to write to '" + fileName + "'", e);
            return false;
        }
    }

    public static boolean complete(String fileName)
    {
        if (!DOWNLOADING.containsKey(fileName))
        {
            LOGGER.warn("Cannot complete file download of '" + fileName + "' as it has not yet begun to download.");
        }

        try
        {
            DOWNLOADING.get(fileName).close();
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to close stream for '" + fileName + "'.");
        }

        return DOWNLOADING.isEmpty();
    }
}
