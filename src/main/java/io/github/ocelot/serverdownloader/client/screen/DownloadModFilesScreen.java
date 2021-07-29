package io.github.ocelot.serverdownloader.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.client.download.ClientDownload;
import io.github.ocelot.serverdownloader.client.download.ClientDownloadManager;
import io.github.ocelot.serverdownloader.common.UnitHelper;
import io.github.ocelot.serverdownloader.common.download.DownloadableModFile;
import io.github.ocelot.sonar.client.render.ShapeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Ocelot
 */
public class DownloadModFilesScreen extends Screen
{
    private final Map<DownloadableModFile, CompletableFuture<ClientDownload>> downloadingFiles;
    private final long startTime;
    private int downloadedFiles;
    private boolean cancelled;

    private Button cancelButton;

    public DownloadModFilesScreen(String httpServer, Set<DownloadableModFile> missingFiles)
    {
        super(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".download", missingFiles.size()));
        this.downloadingFiles = new HashMap<>();
        this.startTime = System.currentTimeMillis();
        this.downloadedFiles = 0;
        this.cancelled = false;
        missingFiles.forEach(modFile -> this.downloadingFiles.put(modFile, ClientDownloadManager.download(modFile, httpServer + "/download?mod=" + modFile.getModIds()[0], download ->
        {
            this.downloadedFiles++;
            if (!this.cancelled && this.downloadingFiles.values().stream().allMatch(future -> future.isDone() && future.join().isDone()))
            {
                Minecraft.getInstance().setScreen(new DownloadModFilesCompleteScreen(this.startTime, System.currentTimeMillis(), this.downloadedFiles));
            }
        }).handleAsync((download, exception) ->
        {
            if (exception != null)
            {
                exception.printStackTrace();
                this.cancel();
                SystemToast.onPackCopyFailure(Minecraft.getInstance(), exception.getMessage());
            }
            return download;
        }, Minecraft.getInstance())));
    }

    private void cancel()
    {
        this.downloadingFiles.values().forEach(file -> file.thenAcceptAsync(download -> download.cancel(true), HttpUtil.DOWNLOAD_EXECUTOR));
        this.cancelled = true;
        this.cancelButton.active = false;
        CompletableFuture.allOf(this.downloadingFiles.values().stream().map(future -> future.thenCompose(ClientDownload::getCompletionFuture)).toArray(CompletableFuture[]::new)).exceptionally(e ->
        {
            if (e != null)
                e.printStackTrace();
            return null;
        }).thenRunAsync(() -> this.getMinecraft().setScreen(new TitleScreen()), this.getMinecraft());
    }

    @Override
    protected void init()
    {
        this.addButton(this.cancelButton = new Button(this.width / 2 - 100, (this.height - 24) - this.height / 8, 200, 20, new TranslatableComponent("gui.cancel"), component -> this.cancel()));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);

        long now = System.currentTimeMillis();
        String time = UnitHelper.abbreviateTime(now - this.startTime, TimeUnit.MILLISECONDS) + " elapsed";
        Component count = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".download.count", this.downloadedFiles, this.downloadingFiles.size());

        this.font.draw(matrixStack, this.title, Mth.fastFloor((this.width - this.font.width(this.title)) / 2F), Mth.fastFloor(this.height / 8F), 11184810);
        this.font.draw(matrixStack, time, Mth.fastFloor((this.width - this.font.width(time)) / 2F), (this.height - 24) - Mth.fastFloor(this.height / 8F) + 26, 11184810);
        this.font.draw(matrixStack, count, Mth.fastFloor((this.width - this.font.width(count)) / 2F), (this.height - 24) - Mth.fastFloor(this.height / 8F) + 30 + this.font.lineHeight, 11184810);

        Map.Entry<DownloadableModFile, CompletableFuture<ClientDownload>> hoveredEntry = null;
        int i = 0;
        for (Map.Entry<DownloadableModFile, CompletableFuture<ClientDownload>> entry : this.downloadingFiles.entrySet())
        {
            DownloadableModFile mod = entry.getKey();
            CompletableFuture<ClientDownload> future = entry.getValue();
            if (future.isDone() && future.join().getBytesDownloaded() > 0 && !future.join().isDone())
            {
                String ids = String.join(", ", mod.getModIds());
                this.font.draw(matrixStack, ids, (this.width - 182) / 2f - this.font.width(ids) - 4, Mth.fastFloor(this.height / 8F) + ((2 + i) * this.font.lineHeight), 11184810);

                this.getMinecraft().getTextureManager().bind(GUI_ICONS_LOCATION);
                VertexConsumer builder = ShapeRenderer.begin();
                ShapeRenderer.drawRectWithTexture(builder, matrixStack, Mth.fastFloor((this.width - 182) / 2F), Mth.fastFloor(this.height / 8F) + ((2 + i) * this.font.lineHeight) + 1, 0, 64, 182, 5);
                ShapeRenderer.drawRectWithTexture(builder, matrixStack, Mth.fastFloor((this.width - 182) / 2F), Mth.fastFloor(this.height / 8F) + ((2 + i) * this.font.lineHeight) + 1, 0, 69, (float) (future.join().getDownloadPercentage() * 182), 5);
                ShapeRenderer.end();

                if (mouseX >= (this.width - 182) / 2f && mouseX < (this.width + 182) / 2 && mouseY >= this.height / 8 + ((2 + i) * this.font.lineHeight) && mouseY < this.height / 8f + ((2 + i) * this.font.lineHeight) + 6)
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
