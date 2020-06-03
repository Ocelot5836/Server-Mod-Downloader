package io.github.ocelot.client.screen;

import io.github.ocelot.ServerDownloader;
import io.github.ocelot.common.download.ModFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Set;

/**
 * @author Ocelot
 */
public class DownloadModFilesConfirmationScreen extends Screen
{
    private final Set<ModFile> missingFiles;

    public DownloadModFilesConfirmationScreen(Set<ModFile> missingFiles)
    {
        super(new TranslationTextComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download", missingFiles.size()));
        this.missingFiles = missingFiles;
    }

    @Override
    protected void init()
    {
        System.out.println("Missing " + this.missingFiles);
        this.addButton(new Button(this.width / 2 - 100, (this.height - 24) / 2, 200, 20, I18n.format("menu.quit"), component -> this.getMinecraft().displayGuiScreen(new DownloadModFilesScreen(this.missingFiles))));
        this.addButton(new Button(this.width / 2 - 100, (this.height + 24) / 2, 200, 20, I18n.format("gui.toTitle"), component -> this.getMinecraft().displayGuiScreen(new MainMenuScreen())));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        partialTicks = Minecraft.getInstance().getRenderPartialTicks();
        this.renderBackground();

        this.drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, (this.height - 66 - this.getMinecraft().fontRenderer.FONT_HEIGHT) / 2, 11184810);

        super.render(mouseX, mouseY, partialTicks);
    }
}
