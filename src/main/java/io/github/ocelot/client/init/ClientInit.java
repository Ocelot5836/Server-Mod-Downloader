package io.github.ocelot.client.init;

import io.github.ocelot.common.download.ModFileManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@SuppressWarnings("unused")
public class ClientInit
{
    @OnlyIn(Dist.CLIENT)
    public static void init(IEventBus bus)
    {
        ModFileManager.load();
    }

    @OnlyIn(Dist.CLIENT)
    public static void setup(FMLClientSetupEvent event)
    {
    }
}
