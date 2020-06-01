package io.github.ocelot.client.screen;

import io.github.ocelot.ServerDownloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TranslationTextComponent;

/**
 * @author Ocelot
 */
public class DownloadModFilesScreen extends Screen
{
    private final Screen screen;

    public DownloadModFilesScreen(Screen screen)
    {
        super(new TranslationTextComponent("screen." + ServerDownloader.MOD_ID + ".download"));
        this.screen = screen;
    }

    @Override
    protected void init()
    {
        this.addButton(new Button(this.width / 2 - 100, Math.min(this.height / 2 + Minecraft.getInstance().fontRenderer.FONT_HEIGHT / 2 + 9, this.height - 30), 200, 20, I18n.format("gui.toMenu"), component ->
        {
            this.getMinecraft().displayGuiScreen(this.screen);
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        partialTicks = Minecraft.getInstance().getRenderPartialTicks();
        this.renderBackground();
        super.render(mouseX, mouseY, partialTicks);
    }
}
