package io.github.ocelot.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ocelot.ServerDownloader;
import io.github.ocelot.client.download.ClientDownload;
import io.github.ocelot.client.download.ClientDownloadManager;
import io.github.ocelot.common.UnitHelper;
import io.github.ocelot.common.download.DownloadableModFile;
import io.github.ocelot.sonar.client.render.ShapeRenderer;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Ocelot
 */
public class DownloadModFilesScreen extends Screen
{
    public static final DecimalFormat DECIMAL_FORMAT;

    private static final Logger LOGGER = LogManager.getLogger();

    static
    {
        DECIMAL_FORMAT = new DecimalFormat("#.#");
        DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    private final Map<DownloadableModFile, CompletableFuture<ClientDownload>> downloadingFiles;
    private final long startTime;
    private int downloadedFiles;
    private boolean cancelled;

    public DownloadModFilesScreen(String httpServer, Set<DownloadableModFile> missingFiles)
    {
        super(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".download", missingFiles.size()));
        this.downloadingFiles = new HashMap<>();
        this.startTime = System.currentTimeMillis();
        this.downloadedFiles = 0;
        this.cancelled = false;
        missingFiles.forEach(modFile ->
        {
            this.downloadingFiles.put(modFile, ClientDownloadManager.download(modFile.getModId(), httpServer + "/download?mod=" + modFile.getModId(), download ->
            {
                this.minecraft.execute(() ->
                {
                    this.downloadedFiles++;
                    if (!this.cancelled && this.downloadingFiles.values().stream().allMatch(future -> future.isDone() && future.join().isCompleted() && !future.join().isFailed()))
                    {
                        this.minecraft.setScreen(new DownloadModFilesCompleteScreen(this.startTime, System.currentTimeMillis(), this.downloadedFiles));
                    }
                });
            }).handleAsync((download, exception) ->
            {
                if (exception != null)
                {
                    exception.printStackTrace();
                    this.cancel();
                }
                return download;
            }));
//            this.downloadingFiles.put(modFile, ClientDownloadManager.download(httpServer + "?mod=" + modFile.getModId(), data -> this.writeToFile(modFile, data), t ->
//            {
//                LOGGER.error("Failed to download mod file for '" + modFile + "' from '" + modFile.getUrl() + "'", t);
//                this.downloadedFiles++;
//            }));
        });
    }

//    private void writeToFile(ModFile modFile, InputStream data)
//    {
//        Path downloadFolder = Paths.get(this.minecraft.gameDirectory.getAbsolutePath(), ServerDownloader.MOD_ID + "-mod-downloads");
//        Path fileLocation = downloadFolder.resolve(modFile.getModId() + "." + FilenameUtils.getExtension(modFile.getUrl()));
//
//        try
//        {
//            if (!Files.exists(downloadFolder))
//                Files.createDirectories(downloadFolder);
//            if (!Files.exists(fileLocation))
//                Files.createFile(fileLocation);
//            try (FileOutputStream outputStream = new FileOutputStream(fileLocation.toString()))
//            {
//                IOUtils.copyLarge(data, outputStream);
//            }
//        }
//        catch (Exception e)
//        {
//            LOGGER.error("Failed to write '" + modFile.getModId() + "' to '" + fileLocation + "'", e);
//        }
//
//        this.getMinecraft().execute(() ->
//        {
//            if (this.downloadingFiles.get(modFile).isCancelled())
//            {
//                try
//                {
//                    LOGGER.debug("Deleting partial download '" + fileLocation + "'");
//                    Files.deleteIfExists(fileLocation);
//                }
//                catch (IOException e)
//                {
//                    LOGGER.error("Failed to delete '" + fileLocation + "'", e);
//                }
//            }
//            this.downloadedFiles++;
//            if (!this.cancelled && this.downloadingFiles.values().stream().allMatch(OnlineRequest.Request::isDownloaded))
//            {
//                this.getMinecraft().displayGuiScreen(new DownloadModFilesCompleteScreen(this.startTime, System.nanoTime(), this.downloadedFiles));
//            }
//        });
//    }

    private void cancel()
    {
        this.downloadingFiles.values().forEach(file -> file.thenAcceptAsync(ClientDownload::cancel, HttpUtil.DOWNLOAD_EXECUTOR));
        this.cancelled = true;
        this.getMinecraft().setScreen(new TitleScreen());
        // TODO error message
    }

    @Override
    protected void init()
    {
        this.addButton(new Button(this.width / 2 - 100, (this.height - 24) - this.height / 8, 200, 20, new TranslatableComponent("gui.cancel"), component ->
        {
            this.cancel();
            component.active = false;
        }));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);

        long now = System.currentTimeMillis();
        String time = UnitHelper.abbreviateTime(now - this.startTime, TimeUnit.MILLISECONDS) + " elapsed";
        Component count = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".download.count", this.downloadedFiles, this.downloadingFiles.size());

        this.font.draw(matrixStack, this.title, (this.width - this.font.width(this.title)) / 2F, this.height / 8F, 11184810);
        this.font.draw(matrixStack, time, (this.width - this.font.width(time)) / 2F, (this.height - 24) - this.height / 8F + 26, 11184810);
        this.font.draw(matrixStack, count, (this.width - this.font.width(count)) / 2F, (this.height - 24) - this.height / 8F + 30 + this.font.lineHeight, 11184810);

        Map.Entry<DownloadableModFile, CompletableFuture<ClientDownload>> hoveredEntry = null;
        int i = 0;
        for (Map.Entry<DownloadableModFile, CompletableFuture<ClientDownload>> entry : this.downloadingFiles.entrySet())
        {
            DownloadableModFile mod = entry.getKey();
            CompletableFuture<ClientDownload> future = entry.getValue();
            if (future.isDone() && future.join().getBytesDownloaded() > 0 && !future.join().isCompleted())
            {
                this.font.draw(matrixStack, mod.getModId(), (this.width - 182) / 2f - this.font.width(mod.getModId()) - 4, this.height / 8f + ((2 + i) * this.font.lineHeight), 11184810);

                this.getMinecraft().getTextureManager().bind(GUI_ICONS_LOCATION);
                VertexConsumer builder = ShapeRenderer.begin();
                ShapeRenderer.drawRectWithTexture(builder, matrixStack, (this.width - 182) / 2f, this.height / 8f + ((2 + i) * this.font.lineHeight) + 1, 0, 64, 182, 5);
                ShapeRenderer.drawRectWithTexture(builder, matrixStack, (this.width - 182) / 2f, this.height / 8f + ((2 + i) * this.font.lineHeight) + 1, 0, 69, (float) (future.join().getDownloadPercentage() * 182), 5);
                ShapeRenderer.end();

                if (mouseX >= (this.width - 182) / 2f && mouseX < (this.width + 182) / 2f && mouseY >= this.height / 8f + ((2 + i) * this.font.lineHeight) && mouseY < this.height / 8f + ((2 + i) * this.font.lineHeight) + 6)
                {
                    hoveredEntry = entry;
                }
                i++;
            }
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (hoveredEntry != null)
        {
            ClientDownload download = hoveredEntry.getValue().join();
            MutableComponent component = new TextComponent(UnitHelper.abbreviateSize(download.getBytesDownloaded()));
            if (download.getSize() != -1)
                component.append(" / " + UnitHelper.abbreviateSize(download.getSize()));
            this.renderTooltip(matrixStack, component, mouseX, mouseY);
        }
    }

    @Override
    public void removed()
    {
        // TODO delete all files if cancelled

//        this.downloadingFiles.values().forEach(file->file.thenAcceptAsync(ClientDownload::cancel, HttpUtil.DOWNLOAD_EXECUTOR));

//            LOGGER.debug("Deleting " + this.partialDownloadedFiles.size() + " partially downloaded files.");
//            this.partialDownloadedFiles.forEach(path ->
//            {
//                if (!Files.exists(path))
//                    return;
//                try
//                {
//                    Files.delete(path);
//                }
//                catch (IOException e)
//                {
//                    LOGGER.error("Failed to delete file '" + path + "'", e);
//                }
//            });
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }
}
