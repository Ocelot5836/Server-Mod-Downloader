package io.github.ocelot.serverdownloader.mixin.server;

import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.common.network.ServerDownloaderMessages;
import io.github.ocelot.serverdownloader.common.network.login.ClientboundNotifyFileStatusMessage;
import io.github.ocelot.serverdownloader.server.ModFileHttpServer;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(NetworkRegistry.class)
public class NetworkRegistryMixin
{
    @Inject(method = "gatherLoginPayloads", at = @At("TAIL"), remap = false, cancellable = true)
    private static void gatherLoginPayloads(NetworkDirection direction, boolean isLocal, CallbackInfoReturnable<List<NetworkRegistry.LoginPayload>> cir)
    {
        if (isLocal || !ModFileHttpServer.isRunning())
            return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
        ServerDownloaderMessages.LOGIN.encodeMessage(new ClientboundNotifyFileStatusMessage(server.getResourcePack(), server.getResourcePackHash(), ModFileHttpServer.isSecure()), pb);
        cir.getReturnValue().add(0, new NetworkRegistry.LoginPayload(pb, new ResourceLocation(ServerDownloader.MOD_ID, "login"), ClientboundNotifyFileStatusMessage.class.getName()));
    }
}
