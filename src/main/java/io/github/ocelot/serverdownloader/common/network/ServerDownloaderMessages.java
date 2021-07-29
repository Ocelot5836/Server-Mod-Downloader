package io.github.ocelot.serverdownloader.common.network;

import io.github.ocelot.serverdownloader.ServerDownloader;
import io.github.ocelot.serverdownloader.common.network.handler.DownloaderClientLoginHandler;
import io.github.ocelot.serverdownloader.common.network.handler.DownloaderServerLoginHandler;
import io.github.ocelot.serverdownloader.common.network.login.NotifyFileStatusMessage;
import io.github.ocelot.serverdownloader.common.network.login.NotifyFileStatusResponseMessage;
import io.github.ocelot.sonar.common.network.SonarNetworkManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;

/**
 * @author Ocelot
 */
public class ServerDownloaderMessages
{
    public static final String VERSION = "1";
    public static final SimpleChannel LOGIN = net.minecraftforge.fml.network.NetworkRegistry.newSimpleChannel(new ResourceLocation(ServerDownloader.MOD_ID, "login"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void init()
    {
        SonarNetworkManager loginManager = new SonarNetworkManager(LOGIN, () -> DownloaderClientLoginHandler::new, () -> DownloaderServerLoginHandler::new);
        loginManager.registerLogin(NotifyFileStatusMessage.class, NotifyFileStatusMessage::new, local -> local ? Collections.emptyList() : Collections.singletonList(Pair.of(NotifyFileStatusMessage.class.getName(), new NotifyFileStatusMessage())), NetworkDirection.LOGIN_TO_CLIENT);
        loginManager.registerLoginReply(NotifyFileStatusResponseMessage.class, NotifyFileStatusResponseMessage::new, NetworkDirection.LOGIN_TO_SERVER);
    }
}
