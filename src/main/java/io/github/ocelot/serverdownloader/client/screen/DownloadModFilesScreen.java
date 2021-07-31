package io.github.ocelot.serverdownloader.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.client.download.ClientDownload;
import io.github.ocelot.serverdownloader.common.UnitHelper;
import io.github.ocelot.serverdownloader.common.download.DownloadableFile;
import io.github.ocelot.sonar.client.render.ShapeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.*;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Ocelot
 */
public class DownloadModFilesScreen extends Screen
{
    private final Map<DownloadableFile, CompletableFuture<ClientDownload>> downloadingFiles;
    private final long startTime;
    private int downloadedFiles;
    private boolean cancelled;
    private Throwable error;

    private Button cancelButton;

    public DownloadModFilesScreen(ServerData server, String httpServer, Set<DownloadableFile> missingFiles)
    {
        super(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".download", missingFiles.size()));
        this.downloadingFiles = new HashMap<>();
        this.startTime = System.currentTimeMillis();
        this.downloadedFiles = 0;
        this.cancelled = false;

        boolean restartRequired = missingFiles.stream().anyMatch(DownloadableFile::needsRestart);
        missingFiles.forEach(modFile -> this.downloadingFiles.put(modFile, modFile.createDownload(httpServer, download ->
        {
            this.downloadedFiles++;
            if (!this.cancelled && this.downloadingFiles.values().stream().allMatch(future -> future.isDone() && future.join().isDone()))
            {
                if (restartRequired)
                {
                    Minecraft.getInstance().setScreen(new DownloadModFilesCompleteScreen(this.startTime, System.currentTimeMillis(), this.downloadedFiles));
                }
                else
                {
                    Minecraft.getInstance().setScreen(new ConnectScreen(this, Minecraft.getInstance(), server));
                }
            }
        }).handleAsync((download, exception) ->
        {
            if (exception != null)
            {
                exception.printStackTrace();
                this.cancel();
                if (this.error == null)
                    this.error = exception.getCause();
            }
            if (download != null && download.hasFailed())
            {
                try
                {
                    download.getCompletionFuture().join();
                    if (this.error == null)
                    {
                        this.error = new IOException("Failed to download file");
                        this.error.printStackTrace();
                    }
                }
                catch (CompletionException e)
                {
                    e.printStackTrace();
                    if (this.error == null)
                        this.error = e.getCause();
                }

                if (!download.shouldIgnoreErrors()) // If errors are not ignored, report to the user
                {
                    this.cancel();
                }
                else
                {
                    SystemToast.multiline(Minecraft.getInstance(), SystemToast.SystemToastIds.PACK_COPY_FAILURE, new TranslatableComponent("toast." + ServerDownloader.MOD_ID + ".download_failure"), new TextComponent(this.error.getMessage()));
                    this.error = null;
                }
            }
            return download;
        }, Minecraft.getInstance())));
    }

    private void cancel()
    {
        if (this.cancelled)
            return;

        this.downloadingFiles.values().forEach(file -> file.thenAcceptAsync(download -> download.cancel(true), HttpUtil.DOWNLOAD_EXECUTOR));
        this.cancelled = true;
        this.cancelButton.active = false;
        CompletableFuture.allOf(this.downloadingFiles.values().stream().map(future -> future.thenCompose(ClientDownload::getCompletionFuture)).toArray(CompletableFuture[]::new)).exceptionally(e ->
        {
            if (e != null)
                e.printStackTrace();
            return null;
        }).thenRunAsync(() ->
        {
            if (this.error != null)
            {
                this.getMinecraft().setScreen(new DisconnectedScreen(new JoinMultiplayerScreen(new TitleScreen()), new TranslatableComponent("error." + ServerDownloader.MOD_ID + ".download_failure"), new TextComponent(this.error.getMessage())));
            }
            else
            {
                this.getMinecraft().setScreen(new JoinMultiplayerScreen(new TitleScreen()));
            }
        }, this.getMinecraft());
    }

    @Override
    protected void init()
    {
        this.addButton(this.cancelButton = new Button(this.width / 2 - 100, (this.height - 24) - this.height / 8, 200, 20, CommonComponents.GUI_CANCEL, component -> this.cancel()));
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

        ClientDownload hoveredDownload = null;
        int i = 0;
        for (Map.Entry<DownloadableFile, CompletableFuture<ClientDownload>> entry : this.downloadingFiles.entrySet())
        {
            DownloadableFile file = entry.getKey();
            CompletableFuture<ClientDownload> future = entry.getValue();
            if (future.isDone() && future.join() != null && future.join().getBytesDownloaded() > 0 && !future.join().isDone())
            {
                Component displayName = file.getDisplayName();
                this.font.draw(matrixStack, displayName, (this.width - 182) / 2f - this.font.width(displayName) - 4, Mth.fastFloor(this.height / 8F) + ((4 + i) * this.font.lineHeight), 11184810);

                this.getMinecraft().getTextureManager().bind(GUI_ICONS_LOCATION);
                VertexConsumer builder = ShapeRenderer.begin();
                ShapeRenderer.drawRectWithTexture(builder, matrixStack, Mth.fastFloor((this.width - 182) / 2F), Mth.fastFloor(this.height / 8F) + ((4 + i) * this.font.lineHeight) + 1, 0, 64, 182, 5);
                ShapeRenderer.drawRectWithTexture(builder, matrixStack, Mth.fastFloor((this.width - 182) / 2F), Mth.fastFloor(this.height / 8F) + ((4 + i) * this.font.lineHeight) + 1, 0, 69, (float) (future.join().getDownloadPercentage() * 182), 5);
                ShapeRenderer.end();

                if (mouseX >= (this.width - 182) / 2f && mouseX < (this.width + 182) / 2 && mouseY >= Mth.fastFloor(this.height / 8F) + ((4 + i) * this.font.lineHeight) + 1 && mouseY < Mth.fastFloor(this.height / 8F) + ((4 + i) * this.font.lineHeight) + 7)
                {
                    hoveredDownload = future.join();
                }
                i++;
            }
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (hoveredDownload != null)
        {
            MutableComponent component = new TextComponent(UnitHelper.abbreviateSize(hoveredDownload.getBytesDownloaded()));
            if (hoveredDownload.getSize() != -1)
                component.append(" / " + UnitHelper.abbreviateSize(hoveredDownload.getSize()));
            this.renderTooltip(matrixStack, component, mouseX, mouseY);
        }
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return false;
    }
}
