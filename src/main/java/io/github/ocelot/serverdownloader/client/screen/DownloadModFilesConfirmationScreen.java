package io.github.ocelot.serverdownloader.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.client.download.DownloadableResourcePackFile;
import io.github.ocelot.serverdownloader.client.screen.component.ScrollingList;
import io.github.ocelot.serverdownloader.client.screen.component.TooltipRenderer;
import io.github.ocelot.serverdownloader.common.download.DownloadableFile;
import io.github.ocelot.serverdownloader.common.download.DownloadableModFile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TickableWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Ocelot
 */
public class DownloadModFilesConfirmationScreen extends Screen implements TooltipRenderer
{
    private static final Component WARNING = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.warning").withStyle(ChatFormatting.RED);
    private static final Component SECURE = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.secure").withStyle(ChatFormatting.GREEN);
    private static final Component INSECURE = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.insecure").withStyle(ChatFormatting.RED);
    private final ServerData server;
    private final String httpServer;
    private final Collection<DownloadableFile> missingFiles;

    public DownloadModFilesConfirmationScreen(ServerData server, String httpServer, Collection<DownloadableFile> missingFiles)
    {
        super(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download", missingFiles.size()));
        this.server = server;
        this.httpServer = httpServer;
        this.missingFiles = missingFiles;
    }

    private Component getFileText(DownloadableFile file)
    {
        if (file instanceof DownloadableModFile)
        {
            Component tooltip = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.mod.tooltip").withStyle(ChatFormatting.RED);
            return new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.mod").withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))).append(" ").append(file.getDisplayName().copy().withStyle(this.httpServer.startsWith("https") ? ChatFormatting.GREEN : ChatFormatting.RED).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (this.httpServer.startsWith("https") ? SECURE : INSECURE)))));
//                list.addText(file.getDisplayName().copy().withStyle(this.httpServer.startsWith("https") ? ChatFormatting.GREEN : ChatFormatting.RED).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, this.httpServer.startsWith("https") ? SECURE : INSECURE))).append(" ").append(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.mod").withStyle(ChatFormatting.WHITE).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)))));
        }
        else if (file instanceof DownloadableResourcePackFile)
        {
            Component tooltip = new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.resources.tooltip").withStyle(ChatFormatting.GREEN);
            return new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.resources").withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))).append(" ").append(file.getDisplayName().copy().withStyle(((DownloadableResourcePackFile) file).getUrl().startsWith("https") ? ChatFormatting.GREEN : ChatFormatting.RED).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ((DownloadableResourcePackFile) file).getUrl().startsWith("https") ? SECURE : INSECURE))));
//                list.addText(file.getDisplayName().copy().withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, ((DownloadableResourcePackFile) file).getUrl().startsWith("https") ? SECURE : INSECURE))).append(" ").append(new TranslatableComponent("screen." + ServerDownloader.MOD_ID + ".confirm_download.resources").withStyle(ChatFormatting.WHITE).withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip)))));
        }
        else
        {
            return file.getDisplayName().copy().withStyle(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, this.httpServer.startsWith("https") ? SECURE : INSECURE)));
        }
    }

    @Override
    protected void init()
    {
        int listWidth = Math.min(this.width - 40, this.missingFiles.stream().mapToInt(file -> this.font.width(this.getFileText(file)) + 10).filter(i -> i > 200).max().orElse(200));
        int listHeight = Math.min(this.height / 3, 128);
        ScrollingList list = new ScrollingList(this, (this.width - listWidth) / 2, (this.height - listHeight) / 2, listWidth, listHeight);
        this.missingFiles.forEach(file -> list.addText(this.getFileText(file)));
        this.addButton(list);

        this.addButton(new Button(this.width / 2 - 100, (this.height + listHeight + 24) / 2, 200, 20, new TranslatableComponent("button." + ServerDownloader.MOD_ID + ".download"), component -> this.getMinecraft().setScreen(new DownloadModFilesScreen(this.server, this.httpServer, this.missingFiles))));
        this.addButton(new Button(this.width / 2 - 100, (this.height + listHeight + 72) / 2, 200, 20, CommonComponents.GUI_CANCEL, component -> this.getMinecraft().setScreen(new JoinMultiplayerScreen(new TitleScreen()))));
    }

    @Override
    public void tick()
    {
        super.tick();

        for (AbstractWidget widget : this.buttons)
            if (widget instanceof TickableWidget)
                ((TickableWidget) widget).tick();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        partialTicks = Minecraft.getInstance().getFrameTime();
        this.renderBackground(matrixStack);

        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, (this.height - Math.min(this.height / 3, 128) - 65 - this.getMinecraft().font.lineHeight) / 2, 11184810);
        drawCenteredString(matrixStack, this.font, WARNING, this.width / 2, (this.height - Math.min(this.height / 3, 128) - 40 - this.getMinecraft().font.lineHeight) / 2, 11184810);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderTooltip(PoseStack matrixStack, ItemStack stack, int mouseX, int mouseY)
    {
        super.renderTooltip(matrixStack, stack, mouseX, mouseY);
    }

    @Override
    public void renderComponentHoverEffect(PoseStack matrixStack, @Nullable Style style, int mouseX, int mouseY)
    {
        super.renderComponentHoverEffect(matrixStack, style, mouseX, mouseY);
    }
}
