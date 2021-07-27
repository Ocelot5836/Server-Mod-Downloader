package io.github.ocelot.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ocelot.ServerDownloader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * @author Ocelot
 */
public class DownloadModFilesCompleteScreen extends Screen
{
    public DownloadModFilesCompleteScreen(long startTime, long endTime, int downloadedFiles)
    {
        super(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".complete_download", downloadedFiles, "lol I dunno"/*DownloadModFilesScreen.DECIMAL_FORMAT.format(TimeUtils.abbreviateLargestUnit(endTime - startTime, TimeUnit.NANOSECONDS)) + TimeUtils.abbreviate(TimeUtils.getLargestUnit(endTime - startTime, TimeUnit.NANOSECONDS))*/));
    }

    @Override
    protected void init()
    {
        this.addButton(new Button(this.width / 2 - 100, (this.height - 24) / 2, 200, 20, new TranslatableComponent("menu.quit"), component -> this.getMinecraft().stop()));
        this.addButton(new Button(this.width / 2 - 100, (this.height + 24) / 2, 200, 20, new TranslatableComponent("gui.toTitle"), component -> this.getMinecraft().setScreen(new TitleScreen())));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        this.font.draw(matrixStack, this.title, (this.width - this.font.width(this.title)) / 2F, (this.height - 66 - this.getMinecraft().font.lineHeight) / 2F, 11184810);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
