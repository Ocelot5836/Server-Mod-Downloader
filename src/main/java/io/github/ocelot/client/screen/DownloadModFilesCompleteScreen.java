package io.github.ocelot.client.screen;

import io.github.ocelot.ServerDownloader;
import io.github.ocelot.common.TimeUtils;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.concurrent.TimeUnit;

/**
 * @author Ocelot
 */
public class DownloadModFilesCompleteScreen extends Screen
{
    public DownloadModFilesCompleteScreen(long startTime, long endTime, int downloadedFiles)
    {
        super(new TranslationTextComponent("screen." + ServerDownloader.MOD_ID + ".complete_download", downloadedFiles, DownloadModFilesScreen.DECIMAL_FORMAT.format(TimeUtils.abbreviateLargestUnit(endTime - startTime, TimeUnit.NANOSECONDS)) + TimeUtils.abbreviate(TimeUtils.getLargestUnit(endTime - startTime, TimeUnit.NANOSECONDS))));
    }

    @Override
    protected void init()
    {
        this.addButton(new Button(this.width / 2 - 100, (this.height - 24) / 2, 200, 20, I18n.format("button." + ServerDownloader.MOD_ID + ".restart"), component -> this.getMinecraft().shutdown()));
        this.addButton(new Button(this.width / 2 - 100, (this.height + 24) / 2, 200, 20, I18n.format("gui.toTitle"), component -> this.getMinecraft().displayGuiScreen(new MainMenuScreen())));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground();
        this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, (this.height - 66 - this.getMinecraft().fontRenderer.FONT_HEIGHT) / 2, 11184810);
        super.render(mouseX, mouseY, partialTicks);
    }
}
