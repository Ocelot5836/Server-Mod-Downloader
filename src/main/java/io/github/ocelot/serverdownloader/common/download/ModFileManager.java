package io.github.ocelot.serverdownloader.common.download;

import io.github.ocelot.serverdownloader.server.BlacklistedServerModLoader;
import net.minecraft.Util;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author Ocelot
 */
public class ModFileManager
{
    private static final Map<String, DownloadableModFile> MOD_FILES = new HashMap<>();
    private static final Reloader RELOADER = new Reloader();

    /**
     * @return The reloader to add for reloading
     */
    public static PreparableReloadListener getReloader()
    {
        return RELOADER;
    }

    /**
     * Checks the provided list of client files for all missing files on the server
     *
     * @param clientFiles The list of files sent by the client
     * @return The files the client failed to have
     */
    @OnlyIn(Dist.DEDICATED_SERVER)
    public static Set<DownloadableModFile> getClientMissingFiles(Set<DownloadableModFile> clientFiles)
    {
        return MOD_FILES.values().stream().filter(serverFile -> !clientFiles.contains(serverFile)).collect(Collectors.toSet());
    }

    /**
     * Checks the provided list of server files for all missing files on the client
     *
     * @param serverFiles The list of files sent by the server
     * @return The files the client failed to have
     */
    @OnlyIn(Dist.CLIENT)
    public static Set<DownloadableFile> getMissingFiles(Set<DownloadableModFile> serverFiles)
    {
        return serverFiles.stream().filter(serverFile -> !MOD_FILES.containsValue(serverFile)).collect(Collectors.toSet());
    }

    /**
     * @return A set of all mod files that will be sent to the client
     */
    public static Collection<DownloadableModFile> getFiles()
    {
        return MOD_FILES.values();
    }

    private static class Reloader implements PreparableReloadListener
    {
        private final Dist dist = FMLLoader.getDist();

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller executionProfiler, Executor backgroundExecutor, Executor gameExecutor)
        {
            return CompletableFuture.runAsync(MOD_FILES::clear, gameExecutor).thenCompose(__ -> (this.dist.isClient() ? CompletableFuture.completedFuture(null) : BlacklistedServerModLoader.INSTANCE.reload(barrier, resourceManager, preparationsProfiler, executionProfiler, backgroundExecutor, gameExecutor)).thenCompose(___ -> CompletableFuture.allOf(ModList.get().getModFiles().stream().map(info -> CompletableFuture.supplyAsync(() ->
            {
                ModFile file = info.getFile();
                if (!Files.isRegularFile(file.getFilePath()))
                    return null;
                try (FileInputStream is = new FileInputStream(file.getFilePath().toFile()))
                {
                    return new DownloadableModFile(info.getMods().stream().map(IModInfo::getModId).toArray(String[]::new), DigestUtils.sha1Hex(is));
                }
                catch (IOException e)
                {
                    throw new CompletionException("Failed to read mod file: " + file.getFileName(), e);
                }
            }, Util.ioPool()).thenCompose(barrier::wait).thenAcceptAsync(modFile ->
            {
                if (modFile == null)
                    return;
                if (this.dist.isDedicatedServer() && !info.getMods().stream().map(IModInfo::getModId).allMatch(BlacklistedServerModLoader::isValid))
                    return;
                for (IModInfo modInfo : info.getMods())
                    MOD_FILES.put(modInfo.getModId(), modFile);
            }, gameExecutor)).toArray(CompletableFuture[]::new))));
        }
    }
}
