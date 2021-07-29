package io.github.ocelot.serverdownloader.mixin.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PlayerList.class)
public class PlayerListMixin
{
    @Shadow
    @Final
    private MinecraftServer server;

    @ModifyArgs(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendTexturePack(Ljava/lang/String;Ljava/lang/String;)V"))
    public void redirectGetResourcePack(Args args)
    {
        String resourcePack = args.get(0);
        if (resourcePack.startsWith("level://"))
            args.set(0, "http://" + this.server.getLocalIp() + ":" + (this.server.getPort() + 1) + "/resources.zip");
    }
}
