package io.github.ocelot.serverdownloader.client.screen.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Ocelot
 */
public interface TooltipRenderer
{
    void renderTooltip(PoseStack matrixStack, ItemStack stack, int mouseX, int mouseY);

    List<Component> getTooltipFromItem(ItemStack stack);

    void renderTooltip(PoseStack matrixStack, Component tooltip, int mouseX, int mouseY);

    void renderComponentTooltip(PoseStack matrixStack, List<Component> tooltips, int mouseX, int p_243308_4_);

    void renderWrappedToolTip(PoseStack matrixStack, List<? extends FormattedText> tooltips, int mouseX, int mouseY, Font font);

    void renderTooltip(PoseStack matrixStack, List<? extends FormattedCharSequence> tooltips, int mouseX, int mouseY);

    void renderToolTip(PoseStack matrixStack, List<? extends FormattedCharSequence> tooltips, int mouseX, int mouseY, Font font);

    void renderComponentHoverEffect(PoseStack matrixStack, @Nullable Style style, int mouseX, int mouseY);

    boolean handleComponentClicked(@Nullable Style style);
}
