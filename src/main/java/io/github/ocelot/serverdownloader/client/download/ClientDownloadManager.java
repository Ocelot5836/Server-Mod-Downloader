package io.github.ocelot.serverdownloader.client.download;

import com.google.common.collect.Iterables;
import com.mojang.datafixers.util.Pair;
import io.github.ocelot.serverdownloader.client.ClientConfig;
import io.github.ocelot.serverdownloader.common.UnitHelper;
import io.github.ocelot.serverdownloader.common.download.DownloadableModFile;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.conn.EofSensorWatcher;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author Ocelot
 */
public class ClientDownloadManager
{
    private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Path CACHE_FOLDER = Paths.get(Minecraft.getInstance().gameDirectory.toURI()).resolve("server-mods");
//    private static final long MAX_DOWNLOAD = 100 * 1024 * 1024; // 100 MB TODO make this a config
//    private static final int DOWNLOAD_BUFFER_SIZE = 4096; // 4 KB TODO make this a config

    private static final Map<String, Pair<Path, Path>> REPLACED_MODS = new ConcurrentHashMap<>();
    private static final Set<Path> REMOVED_MODS = ConcurrentHashMap.newKeySet();

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            int tries = 0;

            for (Path path : REMOVED_MODS)
            {
                try
                {
                    Files.delete(path);
                    REMOVED_MODS.remove(path);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    if (tries < 10)
                    {
                        try
                        {
                            Thread.sleep(1000L); // In case of concurrency issues, try again in a second up to 10 times
                        }
                        catch (InterruptedException e1)
                        {
                            e1.printStackTrace();
                        }
                        tries++;
                    }
                    else
                    {
                        tries = 0;
                        REMOVED_MODS.remove(path);
                    }
                }
            }

            while (!REPLACED_MODS.isEmpty())
            {
                Map.Entry<String, Pair<Path, Path>> entry = Iterables.getFirst(REPLACED_MODS.entrySet(), null);
                if (entry == null)
                    break;

                if (!Files.exists(entry.getValue().getFirst()))
                {
                    REPLACED_MODS.remove(entry.getKey());
                    break;
                }

                try
                {
                    Path dest = entry.getValue().getSecond();
                    if (dest.getParent() != null && !Files.exists(dest.getParent()))
                        Files.createDirectories(dest.getParent());
                    if (!Files.exists(dest))
                        Files.createFile(dest);

                    Files.move(entry.getValue().getFirst(), dest, StandardCopyOption.REPLACE_EXISTING);
                    REPLACED_MODS.remove(entry.getKey());
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    if (tries < 10)
                    {
                        try
                        {
                            Thread.sleep(1000L); // In case of concurrency issues, try again in a second up to 10 times
                        }
                        catch (InterruptedException e1)
                        {
                            e1.printStackTrace();
                        }
                        tries++;
                    }
                    else
                    {
                        tries = 0;
                        REPLACED_MODS.remove(entry.getKey());
                    }
                }
            }
        }));
    }

    public static CompletableFuture<ClientDownload> downloadMod(DownloadableModFile modFile, String url, Consumer<ClientDownload> completeListener)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            InputStream stream = null;
            try
            {
                Path location = CACHE_FOLDER.resolve(modFile.getFileName());

                if (location.getParent() != null && !Files.exists(location.getParent()))
                    Files.createDirectories(location.getParent());
                if (!Files.exists(location))
                {
                    Files.createFile(location);
                }
                else
                {
                    // Attempt to check if the file is already a full download
                    try (InputStream s = new FileInputStream(location.toFile()))
                    {
                        if (DigestUtils.sha1Hex(s).equals(modFile.getHash()))
                        {
                            // If it is, then skip downloading and move on
                            LOGGER.info("Skipped downloading file: " + location);
                            ClientDownload download = new ClientDownload(Files.size(location));
                            Minecraft.getInstance().execute(() ->
                            {
                                download.completeSuccessfully();
                                Minecraft.getInstance().execute(() -> completeListener.accept(download));
                            });
                            return download;
                        }
                    }
                    catch (Exception ignored)
                    {
                    }
                }

                Pair<HttpResponse, InputStream> pair = getStream(url);
                HttpResponse response = pair.getFirst();
                stream = pair.getSecond();

                long fileSize = response.getFirstHeader("Content-Length") != null ? Long.parseLong(response.getFirstHeader("Content-Length").getValue()) : -1;
                if (fileSize > ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024)
                    throw new IOException("Download for " + modFile.getVisualMods() + " is too large. Max Size: " + UnitHelper.abbreviateSize(ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024) + ", Download Size: " + UnitHelper.abbreviateSize(fileSize));

                ClientDownload download = new ClientDownload(fileSize);
                // Perform the download
                CompletableFuture.runAsync(() ->
                {
                    LOGGER.info("Started downloading file: " + location);
                    try (ReadableByteChannel in = Channels.newChannel(pair.getSecond()); FileChannel out = FileChannel.open(location, StandardOpenOption.WRITE))
                    {
                        int readAmount;
                        ByteBuffer buffer = ByteBuffer.allocate(ClientConfig.INSTANCE.downloadBufferSize.get());
                        while ((readAmount = in.read(buffer)) != -1)
                        {
                            if (!FMLLoader.isProduction()) // Debug only
                                Thread.sleep(Math.max(1, 50 - (fileSize / 1024 / 1024)));

                            if (download.isCancelled())
                                throw new CancellationException("Download cancelled");

                            download.addBytesDownloaded(readAmount);
                            if (download.getBytesDownloaded() > ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024)
                                throw new IOException("Download for " + modFile.getVisualMods() + " is too large. Max Size: " + UnitHelper.abbreviateSize(ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024));

                            buffer.flip();
                            out.write(buffer);
                            buffer.clear();
                        }

                        download.completeSuccessfully();
                        Minecraft.getInstance().execute(() -> completeListener.accept(download));

                        REPLACED_MODS.put(modFile.getVisualMods(), Pair.of(location, FMLPaths.MODSDIR.get().resolve(modFile.getFileName()).toAbsolutePath()));
                        // Delete all mod files associated with the files the new mods have
                        Arrays.stream(modFile.getModIds()).map(modId -> ModList.get().getModFileById(modId)).filter(Objects::nonNull).distinct().forEach(modFileInfo -> REMOVED_MODS.add(modFileInfo.getFile().getFilePath()));
                    }
                    catch (CancellationException e)
                    {
                        LOGGER.info("Cancelling download for mod file: " + modFile.getVisualMods());
                        deleteFile(location);
                        download.completeCancelled();
                        Minecraft.getInstance().execute(() -> completeListener.accept(download));
                    }
                    catch (Exception e)
                    {
                        deleteFile(location);
                        throw new CompletionException("Failed to download mod file: " + modFile.getVisualMods(), e);
                    }
                }, HttpUtil.DOWNLOAD_EXECUTOR).exceptionally(e ->
                {
                    if (e != null)
                    {
                        e.printStackTrace();
                        download.completeExceptionally(e, modFile.ignoreErrors());
                        Minecraft.getInstance().execute(() -> completeListener.accept(download));
                    }
                    return null;
                });
                return download;
            }
            catch (Exception e)
            {
                IOUtils.closeQuietly(stream);
                ClientDownload download = new ClientDownload(-1);
                Minecraft.getInstance().execute(() ->
                {
                    download.completeExceptionally(new CompletionException("Failed to request mod file: " + modFile.getVisualMods(), e), modFile.ignoreErrors());
                    Minecraft.getInstance().execute(() -> completeListener.accept(download));
                });
                return download;
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR);
    }

    public static CompletableFuture<ClientDownload> downloadResourcePack(DownloadableResourcePackFile resourcePack, String url, Consumer<ClientDownload> completeListener)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            String fileName = DigestUtils.sha1Hex(url);
            Path location = Minecraft.getInstance().gameDirectory.toPath().resolve("server-resource-packs").resolve(fileName);

            InputStream stream = null;
            try
            {
                if (location.getParent() != null && !Files.exists(location.getParent()))
                    Files.createDirectories(location.getParent());
                if (!Files.exists(location))
                {
                    Files.createFile(location);
                }
                else if (!Files.isRegularFile(location))
                {
                    deleteFile(location);
                    Files.createFile(location);
                }

                Pair<HttpResponse, InputStream> pair = getStream(url);
                HttpResponse response = pair.getFirst();
                stream = pair.getSecond();

                long fileSize = response.getFirstHeader("Content-Length") != null ? Long.parseLong(response.getFirstHeader("Content-Length").getValue()) : -1;
                if (fileSize > ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024)
                    throw new IOException("Download for resource pack is too large. Max Size: " + UnitHelper.abbreviateSize(ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024) + ", Download Size: " + UnitHelper.abbreviateSize(fileSize));

                ClientDownload download = new ClientDownload(fileSize);
                // Perform the download
                CompletableFuture.runAsync(() ->
                {
                    LOGGER.info("Started downloading server resources: " + location);
                    try (ReadableByteChannel in = Channels.newChannel(pair.getSecond()); FileChannel out = FileChannel.open(location, StandardOpenOption.WRITE))
                    {
                        int readAmount;
                        ByteBuffer buffer = ByteBuffer.allocate(ClientConfig.INSTANCE.downloadBufferSize.get());
                        while ((readAmount = in.read(buffer)) != -1)
                        {
                            if (!FMLLoader.isProduction()) // Debug only
                                Thread.sleep(Math.max(1, 50 - (fileSize / 1024 / 1024)));

                            if (download.isCancelled())
                                throw new CancellationException("Download cancelled");

                            download.addBytesDownloaded(readAmount);
                            if (download.getBytesDownloaded() > ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024)
                                throw new IOException("Download for " + url + " is too large. Max Size: " + UnitHelper.abbreviateSize(ClientConfig.INSTANCE.maxDownloadSize.get() * 1024 * 1024));

                            buffer.flip();
                            out.write(buffer);
                            buffer.clear();
                        }

                        download.completeSuccessfully();
                        Minecraft.getInstance().execute(() -> completeListener.accept(download));
                    }
                    catch (CancellationException e)
                    {
                        LOGGER.info("Cancelling download for server resources: " + url);
                        deleteFile(location);
                        download.completeCancelled();
                        Minecraft.getInstance().execute(() -> completeListener.accept(download));
                    }
                    catch (Exception e)
                    {
                        deleteFile(location);
                        throw new CompletionException("Failed to download server resources: " + url, e);
                    }
                }, HttpUtil.DOWNLOAD_EXECUTOR).exceptionally(e ->
                {
                    if (e != null)
                    {
                        e.printStackTrace();
                        download.completeExceptionally(e, resourcePack.ignoreErrors());
                        Minecraft.getInstance().execute(() -> completeListener.accept(download));
                    }
                    return null;
                });
                return download;
            }
            catch (Exception e)
            {
                IOUtils.closeQuietly(stream);
                ClientDownload download = new ClientDownload(-1);
                Minecraft.getInstance().execute(() ->
                {
                    download.completeExceptionally(new CompletionException("Failed to request server resources: " + url, e), resourcePack.ignoreErrors());
                    Minecraft.getInstance().execute(() -> completeListener.accept(download));
                });
                return download;
            }
        }, HttpUtil.DOWNLOAD_EXECUTOR);
    }

    public static boolean isResourcePackDownloaded(String url, String hash)
    {
        String fileName = DigestUtils.sha1Hex(url);
        String fileHash = SHA1.matcher(hash).matches() ? hash : "";
        Path location = Minecraft.getInstance().gameDirectory.toPath().resolve("server-resource-packs").resolve(fileName);

        if (Files.exists(location))
        {
            try (InputStream stream = new FileInputStream(location.toFile()))
            {
                return DigestUtils.sha1Hex(stream).equals(fileHash);
            }
            catch (Exception ignored)
            {
            }
        }

        return false;
    }

    private static void deleteFile(Path location)
    {
        try
        {
            Files.delete(location);
        }
        catch (IOException e1)
        {
            LOGGER.error("Failed to delete file: " + location, e1);
        }
    }

    private static Pair<HttpResponse, InputStream> getStream(String url) throws IOException
    {
        HttpGet get = new HttpGet(url);
        CloseableHttpClient client = HttpClients.custom().setUserAgent("Minecraft Java/" + SharedConstants.getCurrentVersion().getName()).addInterceptorFirst((HttpRequestInterceptor) (request, context) ->
        {
            request.setHeader("X-Minecraft-Username", Minecraft.getInstance().getUser().getName());
            request.setHeader("X-Minecraft-UUID", Minecraft.getInstance().getUser().getUuid());
            request.setHeader("X-Minecraft-Version", SharedConstants.getCurrentVersion().getName());
            request.setHeader("X-Minecraft-Version-ID", SharedConstants.getCurrentVersion().getId());
            request.setHeader("X-Minecraft-Pack-Format", String.valueOf(SharedConstants.getCurrentVersion().getPackVersion()));
        }).build();

        CloseableHttpResponse response = client.execute(get);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != 200)
        {
            IOUtils.closeQuietly(client, response);
            throw new IOException("Failed to connect to '" + url + "'. " + statusLine.getStatusCode() + " " + statusLine.getReasonPhrase());
        }
        return Pair.of(response, new EofSensorInputStream(response.getEntity().getContent(), new EofSensorWatcher()
        {
            @Override
            public boolean eofDetected(InputStream wrapped) throws IOException
            {
                IOUtils.closeQuietly(response);
                client.close();
                return true;
            }

            @Override
            public boolean streamClosed(InputStream wrapped) throws IOException
            {
                IOUtils.closeQuietly(response);
                client.close();
                return true;
            }

            @Override
            public boolean streamAbort(InputStream wrapped) throws IOException
            {
                IOUtils.closeQuietly(response);
                client.close();
                return true;
            }
        }));
    }
}
