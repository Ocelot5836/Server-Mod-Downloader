package io.github.ocelot.client.screen;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import io.github.ocelot.ServerDownloader;
import io.github.ocelot.client.ShapeRenderer;
import io.github.ocelot.common.OnlineRequest;
import io.github.ocelot.common.TimeUtils;
import io.github.ocelot.common.download.ModFile;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
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

    private final Map<ModFile, OnlineRequest.Request> downloadingFiles;
    private final Set<Path> partialDownloadedFiles;
    private final long startTime;
    private int downloadedFiles;
    private boolean cancelled;

    public DownloadModFilesScreen(Set<ModFile> missingFiles)
    {
        super(new TranslationTextComponent("screen." + ServerDownloader.MOD_ID + ".download", missingFiles.size()));
        this.downloadingFiles = new HashMap<>();
        this.partialDownloadedFiles = new HashSet<>();
        this.startTime = System.nanoTime();
        this.downloadedFiles = 0;
        this.cancelled = false;
        missingFiles.forEach(modFile ->
        {
            if (StringUtils.isNullOrEmpty(modFile.getUrl()))
                return;
            this.downloadingFiles.put(modFile, OnlineRequest.make(modFile.getUrl(), data -> this.writeToFile(modFile, data), t -> LOGGER.error("Failed to download mod file for '" + modFile + "' from '" + modFile.getUrl() + "'", t)));
        });
    }

    private void writeToFile(ModFile modFile, InputStream data)
    {
        String fileName = FilenameUtils.getName(modFile.getUrl());
        if (fileName == null)
        {
            LOGGER.warn("Mod file '" + modFile.getModId() + "' had an invalid file name! Using default.");
            fileName = modFile.getModId() + "-" + modFile.getVersion() + ".jar";
        }

        Path downloadFolder = Paths.get(this.getMinecraft().gameDir.getAbsolutePath(), ServerDownloader.MOD_ID + "-mod-downloads");
        Path fileLocation = downloadFolder.resolve(fileName);
        this.partialDownloadedFiles.add(fileLocation);

        try
        {
            if (!Files.exists(downloadFolder))
                Files.createDirectories(downloadFolder);
            if (!Files.exists(fileLocation))
                Files.createFile(fileLocation);
            try (FileOutputStream outputStream = new FileOutputStream(fileLocation.toString()))
            {
                IOUtils.copyLarge(data, outputStream);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to write '" + modFile.getModId() + "' to '" + fileLocation + "'", e);
        }

        this.getMinecraft().execute(() ->
        {
            if (this.downloadingFiles.get(modFile).isCancelled())
            {
                try
                {
                    LOGGER.debug("Deleting partial download '" + fileLocation + "'");
                    Files.deleteIfExists(fileLocation);
                }
                catch (IOException e)
                {
                    LOGGER.error("Failed to delete '" + fileLocation + "'", e);
                }
            }
            this.partialDownloadedFiles.remove(fileLocation);
            this.downloadedFiles++;
            if (!this.cancelled && this.downloadingFiles.values().stream().allMatch(OnlineRequest.Request::isDownloaded))
            {
                this.getMinecraft().displayGuiScreen(new DownloadModFilesCompleteScreen(this.startTime, System.nanoTime(), this.downloadedFiles));
            }
        });
    }

    @Override
    protected void init()
    {
        this.addButton(new Button(this.width / 2 - 100, (this.height + 24) / 2, 200, 20, I18n.format("gui.cancel"), component ->
        {
            this.downloadingFiles.values().forEach(OnlineRequest.Request::cancel);
            this.cancelled = true;
            component.active = false;
            this.getMinecraft().displayGuiScreen(new MainMenuScreen());
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground();

        long now = System.nanoTime();
        this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, this.height / 8, 11184810);
        this.drawCenteredString(this.font, DECIMAL_FORMAT.format(TimeUtils.abbreviateLargestUnit(now - this.startTime, TimeUnit.NANOSECONDS)) + TimeUtils.abbreviate(TimeUtils.getLargestUnit(now - this.startTime, TimeUnit.NANOSECONDS)) + " elapsed", this.width / 2, (this.height + 24) / 2 + 26, 11184810);
        this.drawCenteredString(this.font, I18n.format("screen.serverdownloader.download.count", this.downloadedFiles, this.downloadingFiles.size()), this.width / 2, (this.height + 24) / 2 + 30 + this.font.FONT_HEIGHT, 11184810);

        Map.Entry<ModFile, OnlineRequest.Request> hoveredEntry = null;
        int i = 0;
        for (Map.Entry<ModFile, OnlineRequest.Request> entry : this.downloadingFiles.entrySet())
        {
            ModFile mod = entry.getKey();
            OnlineRequest.Request request = entry.getValue();
            if (!request.isDownloaded())
            {
                this.font.drawString(mod.getModId(), (this.width - 182) / 2f - this.font.getStringWidth(mod.getModId()) - 4, this.height / 8f + ((2 + i) * this.font.FONT_HEIGHT), 11184810);

                this.getMinecraft().getTextureManager().bindTexture(GUI_ICONS_LOCATION);
                IVertexBuilder builder = ShapeRenderer.begin();
                ShapeRenderer.drawRectWithTexture(builder, (this.width - 182) / 2f, this.height / 8f + ((2 + i) * this.font.FONT_HEIGHT) + 1, 0, 64, 182, 5);
                ShapeRenderer.drawRectWithTexture(builder, (this.width - 182) / 2f, this.height / 8f + ((2 + i) * this.font.FONT_HEIGHT) + 1, 0, 69, request.getDownloadPercentage() * 182, 5);
                ShapeRenderer.end();

                if (mouseX >= (this.width - 182) / 2f && mouseX < (this.width + 182) / 2f && mouseY >= this.height / 8f + ((2 + i) * this.font.FONT_HEIGHT) && mouseY < this.height / 8f + ((2 + i) * this.font.FONT_HEIGHT) + 6)
                {
                    hoveredEntry = entry;
                }
                i++;
            }
        }

        super.render(mouseX, mouseY, partialTicks);

        if (hoveredEntry != null)
        {
            this.renderTooltip((int) (hoveredEntry.getValue().getDownloadPercentage() * 100.0) + "%", mouseX, mouseY);
        }
    }

    @Override
    public void removed()
    {
        // TODO delete all files if cancelled

        if (this.downloadingFiles.values().stream().allMatch(OnlineRequest.Request::isDownloaded))
        {
            this.cancelled = true;
        }

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
}
