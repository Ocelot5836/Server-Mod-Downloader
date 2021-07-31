package io.github.ocelot.serverdownloader.common.network;

import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.common.network.handler.DownloaderClientLoginHandler;
import io.github.ocelot.serverdownloader.common.network.handler.DownloaderServerLoginHandler;
import io.github.ocelot.serverdownloader.common.network.login.ClientboundNotifyFileStatusMessage;
import io.github.ocelot.serverdownloader.common.network.login.ServerboundNotifyFileStatusResponseMessage;
import io.github.ocelot.sonar.common.network.SonarNetworkManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Collections;

/**
 * @author Ocelot
 */
public class ServerDownloaderMessages
{
    public static final String VERSION = "1";
    public static final SimpleChannel LOGIN = NetworkRegistry.newSimpleChannel(new ResourceLocation(ServerDownloader.MOD_ID, "login"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init()
    {
        SonarNetworkManager loginManager = new SonarNetworkManager(LOGIN, () -> DownloaderClientLoginHandler::new, () -> DownloaderServerLoginHandler::new);
        loginManager.registerLogin(ClientboundNotifyFileStatusMessage.class, ClientboundNotifyFileStatusMessage::new, local -> Collections.emptyList(), NetworkDirection.LOGIN_TO_CLIENT);
        loginManager.registerLoginReply(ServerboundNotifyFileStatusResponseMessage.class, ServerboundNotifyFileStatusResponseMessage::new, NetworkDirection.LOGIN_TO_SERVER);
    }
}
