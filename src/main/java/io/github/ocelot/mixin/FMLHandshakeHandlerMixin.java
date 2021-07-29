package io.github.ocelot.mixin;

import io.github.ocelot.common.network.ServerDownloaderMessages;
import io.github.ocelot.common.network.login.NotifyFileStatusMessage;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import net.minecraftforge.fml.network.FMLHandshakeMessages;
import net.minecraftforge.fml.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(FMLHandshakeHandler.class)
public class FMLHandshakeHandlerMixin
{
    @Inject(method = "handleClientModListOnServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;disconnect(Lnet/minecraft/network/chat/Component;)V", shift = At.Shift.BEFORE), remap = false)
    public void handleServerModListOnClient(FMLHandshakeMessages.C2SModListReply clientModList, Supplier<NetworkEvent.Context> c, CallbackInfo ci)
    {
        ServerDownloaderMessages.LOGIN.reply(new NotifyFileStatusMessage(), c.get());
    }

    @Inject(method = "handleClientModListOnServer", at = @At("HEAD"), remap = false)
    public void handleServerModListOnClientTest(FMLHandshakeMessages.C2SModListReply clientModList, Supplier<NetworkEvent.Context> c, CallbackInfo ci)
    {
        System.out.println("Test");
    }

    @Inject(method = "handleRegistryLoading", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;disconnect(Lnet/minecraft/network/chat/Component;)V", shift = At.Shift.BEFORE), remap = false, cancellable = true)
    public void handleRegistryLoading(Supplier<NetworkEvent.Context> c, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(true);
    }
}
