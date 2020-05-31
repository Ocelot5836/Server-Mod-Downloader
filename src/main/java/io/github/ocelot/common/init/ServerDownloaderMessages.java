package io.github.ocelot.common.init;

import io.github.ocelot.ServerDownloader;
import io.github.ocelot.common.network.*;
import io.github.ocelot.common.network.handler.ClientMessageHandler;
import io.github.ocelot.common.network.handler.MessageHandler;
import io.github.ocelot.common.network.handler.ServerMessageHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.FMLHandshakeHandler;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Ocelot
 */
@Mod.EventBusSubscriber(modid = ServerDownloader.MOD_ID)
public class ServerDownloaderMessages
{
    public static final ResourceLocation LOGIN_RESOURCE = new ResourceLocation(ServerDownloader.MOD_ID, "login");

    public static final String VERSION = "1.0";
    public static final SimpleChannel LOGIN = NetworkRegistry.newSimpleChannel(LOGIN_RESOURCE, () -> VERSION, s -> true, s -> true);

    @Deprecated
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(ServerDownloader.MOD_ID, "play"), () -> VERSION, s -> true, s -> true);

    private static int index;

    public static void init()
    {
        registerMessage(NotifyFileStatusMessage.class, NetworkDirection.LOGIN_TO_CLIENT, NotifyFileStatusMessage::encode, NotifyFileStatusMessage::decode, (msg, ctx) -> getHandler(ctx).handleNotifyFileStatusMessage(msg, ctx)).
                buildLoginPacketList(local -> local ? Collections.emptyList() : Collections.singletonList(Pair.of(NotifyFileStatusMessage.class.getName(), new NotifyFileStatusMessage()))).
                add();
        registerIndexMessage(NotifyFileStatusResponseMessage.class, NetworkDirection.LOGIN_TO_SERVER, NotifyFileStatusResponseMessage::encode, NotifyFileStatusResponseMessage::decode, (msg, ctx) -> getHandler(ctx).handleNotifyFileStatusResponseMessage(msg, ctx)).add();
        registerMessage(RequestFileResponseMessage.class, NetworkDirection.LOGIN_TO_CLIENT, RequestFileResponseMessage::encode, RequestFileResponseMessage::decode, (msg, ctx) -> getHandler(ctx).handleRequestFileResponseMessage(msg, ctx)).add();
        registerMessage(FileChunkMessage.class, NetworkDirection.LOGIN_TO_CLIENT, FileChunkMessage::encode, FileChunkMessage::decode, (msg, ctx) -> getHandler(ctx).handleFileChunkMessage(msg, ctx)).add();
        registerMessage(FileCompletionMessage.class, NetworkDirection.LOGIN_TO_CLIENT, FileCompletionMessage::encode, FileCompletionMessage::decode, (msg, ctx) -> getHandler(ctx).handleFileCompletionMessage(msg, ctx)).add();
        registerMessage(DownloadCompletionMessage.class, NetworkDirection.LOGIN_TO_CLIENT, DownloadCompletionMessage::encode, DownloadCompletionMessage::decode, (msg, ctx) -> getHandler(ctx).handleDownloadCompletionMessage(msg, ctx)).add();
    }

    private static <MSG> SimpleChannel.MessageBuilder<MSG> registerMessage(Class<MSG> messageType, NetworkDirection direction, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer)
    {
        return LOGIN.messageBuilder(messageType, index++, direction).
                decoder(decoder).
                encoder(encoder).
                consumer(messageConsumer);
    }

    private static <MSG extends LoginMessage> SimpleChannel.MessageBuilder<MSG> registerIndexMessage(Class<MSG> messageType, NetworkDirection direction, BiConsumer<MSG, PacketBuffer> encoder, Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer)
    {
        return LOGIN.messageBuilder(messageType, index++, direction).
                loginIndex(LoginMessage::getAsInt, LoginMessage::setLoginIndex).
                decoder(decoder).
                encoder(encoder).
                consumer(FMLHandshakeHandler.indexFirst((handler, msg, ctx) -> messageConsumer.accept(msg, ctx)));
    }

    private static MessageHandler getHandler(Supplier<NetworkEvent.Context> ctx)
    {
        return ctx.get().getDirection().getReceptionSide().isClient() ? ClientMessageHandler.INSTANCE : ServerMessageHandler.INSTANCE;
    }
}
