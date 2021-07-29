package io.github.ocelot.serverdownloader.mixin.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.FileInputStream;
import java.io.InputStream;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
    @Shadow
    @Final
    protected LevelStorageSource.LevelStorageAccess storageSource;

    @ModifyArg(method = "detectBundledResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setResourcePack(Ljava/lang/String;Ljava/lang/String;)V"), index = 1)
    public String modifyResourcesHash(String hash)
    {
        try (InputStream input = new FileInputStream(this.storageSource.getLevelPath(LevelResource.MAP_RESOURCE_FILE).toFile()))
        {
            return DigestUtils.sha1Hex(input);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return hash;
        }
    }

//    @ModifyArgs(method = "detectBundledResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setResourcePack(Ljava/lang/String;Ljava/lang/String;)V"), index = 1)
//    public void modifyResourcesHash(Args args)
//    {
//        try (InputStream input = new FileInputStream(this.storageSource.getLevelPath(LevelResource.MAP_RESOURCE_FILE).toFile()))
//        {
//            args.set(1, DigestUtils.sha1Hex(input));
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//            return "";
//        }
//    }
}
