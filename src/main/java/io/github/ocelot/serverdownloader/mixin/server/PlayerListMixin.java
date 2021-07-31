package io.github.ocelot.serverdownloader.mixin.server;

import io.github.ocelot.serverdownloader.server.ModFileHttpServer;
import io.github.ocelot.serverdownloader.server.ServerConfig;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Mixin(PlayerList.class)
public class PlayerListMixin
{
    @Unique
    private Connection placingConnection;

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    public void captureConnection(Connection connection, ServerPlayer player, CallbackInfo ci)
    {
        this.placingConnection = connection;
    }

    @ModifyArgs(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendTexturePack(Ljava/lang/String;Ljava/lang/String;)V"))
    public void modifyResources(Args args)
    {
        String resourcePack = args.get(0);
        if (ModFileHttpServer.isRunning() && resourcePack.startsWith("level://"))
        {
            SocketAddress address = this.placingConnection.channel().localAddress();
            if (!(address instanceof InetSocketAddress))
                return;
            args.set(0, (ModFileHttpServer.isSecure() ? "https://" : "http://") + ((InetSocketAddress) address).getAddress().getHostAddress() + ":" + ServerConfig.INSTANCE.httpServerPort.get() + "/resources.zip");
        }
    }
}
