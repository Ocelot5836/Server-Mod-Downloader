package io.github.ocelot.common.download;

import net.minecraft.Util;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
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
@Mod.EventBusSubscriber
public class ModFileManager
{
    private static final Map<String, DownloadableModFile> MOD_FILES = new HashMap<>();

    @SubscribeEvent
    public static void onEvent(AddReloadListenerEvent event)
    {
        event.addListener(new Reloader());
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public static Set<DownloadableModFile> getClientMissingFiles(Set<DownloadableModFile> clientFiles)
    {
        return MOD_FILES.values().stream().filter(serverFile -> !clientFiles.contains(serverFile)).collect(Collectors.toSet());
    }

    @OnlyIn(Dist.CLIENT)
    public static Set<DownloadableModFile> getMissingFiles(Set<DownloadableModFile> serverFiles)
    {
        return serverFiles.stream().filter(serverFile -> !MOD_FILES.containsValue(serverFile)).collect(Collectors.toSet());
    }

    public static Collection<DownloadableModFile> getFiles()
    {
        return MOD_FILES.values();
    }

    private static class Reloader implements PreparableReloadListener
    {
        @Override
        public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller executionProfiler, Executor backgroundExecutor, Executor gameExecutor)
        {
            return CompletableFuture.runAsync(MOD_FILES::clear, gameExecutor).thenCompose(__ -> CompletableFuture.allOf(ModList.get().getModFiles().stream().map(info -> CompletableFuture.supplyAsync(() ->
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
                for (IModInfo modInfo : info.getMods())
                    MOD_FILES.put(modInfo.getModId(), modFile);
            }, gameExecutor)).toArray(CompletableFuture[]::new)));
        }
    }
}
