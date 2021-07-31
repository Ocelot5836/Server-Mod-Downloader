package io.github.ocelot.serverdownloader.client.screen.component;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.ocelot.sonar.client.render.ShapeRenderer;
import io.github.ocelot.sonar.client.util.ScissorHelper;
import io.github.ocelot.sonar.common.util.ScrollHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TickableWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ocelot
 */
public class ScrollingList extends AbstractWidget implements TickableWidget
{
    private final TooltipRenderer tooltipRenderer;
    private final ScrollHandler scrollHandler;
    private final List<Component> text;
    private final Font font;
    private int padding;

    public ScrollingList(TooltipRenderer renderer, int x, int y, int width, int height)
    {
        super(x, y, width, height, TextComponent.EMPTY);
        this.tooltipRenderer = renderer;
        this.scrollHandler = new ScrollHandler(height, height);
        this.text = new ArrayList<>();
        this.font = Minecraft.getInstance().font;
        this.padding = 5;
        this.calculateSize();
    }

    private void calculateSize()
    {
        this.scrollHandler.setHeight(Math.max(this.height, this.text.size() * (this.font.lineHeight + this.padding / 2) + this.padding));
        this.scrollHandler.setScrollSpeed(this.font.lineHeight + this.padding);
    }

    public void addText(Component text)
    {
        this.text.add(text);
        this.calculateSize();
    }

    @Override
    public void tick()
    {
        this.scrollHandler.update();
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, 0xFF000000 | 11184810);

        float scroll = this.scrollHandler.getInterpolatedScroll(partialTicks);

        Minecraft.getInstance().getTextureManager().bind(GuiComponent.BACKGROUND_LOCATION);
        ShapeRenderer.setColor(0.125F, 0.125F, 0.125F, 1.0F);
        ShapeRenderer.drawRectWithTexture(matrixStack, this.x, this.y, 0, scroll, this.width, this.height, this.width, this.height, 32, 32);

        matrixStack.pushPose();
        ScissorHelper.push(this.x, this.y, this.width, this.height);
        if (this.scrollHandler.getMaxScroll() > 0)
            matrixStack.translate(0, -scroll, 0);
        {
            for (int i = 0; i < this.text.size(); i++)
            {
                int y = i * (this.font.lineHeight + this.padding / 2);
                this.font.draw(matrixStack, this.text.get(i), this.x + this.padding, this.y + y + this.padding, -1);
            }
        }
        ScissorHelper.pop();
        matrixStack.popPose();

        if (this.isHovered())
            this.renderToolTip(matrixStack, mouseX, mouseY);
    }

    @Override
    public void renderToolTip(PoseStack matrixStack, int mouseX, int mouseY)
    {
        Style hoveredStyle = this.getClickedComponentStyleAt(mouseX, mouseY);
        if (hoveredStyle != null && hoveredStyle.getHoverEvent() != null)
            this.tooltipRenderer.renderComponentHoverEffect(matrixStack, hoveredStyle, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount)
    {
        return this.scrollHandler.mouseScrolled(2F, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton)
    {
        Style hoveredStyle = this.getClickedComponentStyleAt(mouseX, mouseY);
        if (hoveredStyle != null && hoveredStyle.getClickEvent() != null)
            return this.tooltipRenderer.handleComponentClicked(hoveredStyle);
        return false;
    }

    private Style getClickedComponentStyleAt(double mouseX, double mouseY)
    {
        double x = mouseX - this.x - this.padding;
        double y = mouseY - this.y - this.padding;
        if (this.scrollHandler.getMaxScroll() > 0)
            y += this.scrollHandler.getScroll();
        if (x >= 0 && y >= 0 && x < this.width && y < this.height)
        {
            int line = (int) (y / (this.font.lineHeight + this.padding / 2));
            if (y % (int) (this.font.lineHeight + this.padding / 2F) < this.font.lineHeight && line >= 0 && line < this.text.size())
            {
                Component text = this.text.get(line);
                if (x < this.font.width(text))
                    return this.font.getSplitter().componentStyleAtWidth(text, (int) x);
            }
        }
        return null;
    }

    public void setPadding(int padding)
    {
        this.padding = padding;
        this.calculateSize();
    }
}
