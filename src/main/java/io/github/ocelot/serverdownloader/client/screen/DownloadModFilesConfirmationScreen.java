package io.github.ocelot.serverdownloader.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.common.download.DownloadableFile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Set;

/**
 * @author Ocelot
 */
public class DownloadModFilesConfirmationScreen extends Screen
{
    private final ServerData server;
    private final String httpServer;
    private final Set<DownloadableFile> missingFiles;

    public DownloadModFilesConfirmationScreen(ServerData server, String httpServer, Set<DownloadableFile> missingFiles)
    {
        super(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download", missingFiles.size()));
        this.server = server;
        this.httpServer = httpServer;
        this.missingFiles = missingFiles;
    }

    @Override
    protected void init()
    {
        this.addButton(new Button(this.width / 2 - 100, (this.height - 24) / 2, 200, 20, new TranslatableComponent("button." + ServerDownloader.MOD_ID + ".download"), component -> this.getMinecraft().setScreen(new DownloadModFilesScreen(this.server, this.httpServer, this.missingFiles))));
        this.addButton(new Button(this.width / 2 - 100, (this.height + 24) / 2, 200, 20, CommonComponents.GUI_BACK, component -> this.getMinecraft().setScreen(new JoinMultiplayerScreen(new TitleScreen()))));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        partialTicks = Minecraft.getInstance().getDeltaFrameTime();
        this.renderBackground(matrixStack);

        this.font.draw(matrixStack, this.title, (this.width - this.font.width(this.title)) / 2F, (this.height - 66 - this.getMinecraft().font.lineHeight) / 2F, 11184810);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
