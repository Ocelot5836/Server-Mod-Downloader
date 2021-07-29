package io.github.ocelot.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * <p>Manages the HTTP server for player request handling.</p>
 *
 * @author Ocelot
 */
public class ModFileHttpServer
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static Thread serverThread;
    private static volatile Channel channel;
    private static volatile boolean running;

    /**
     * Opens a new HTTP server for the specified server.
     *
     * @param server The Minecraft server to open a server for
     */
    public static synchronized void open(MinecraftServer server)
    {
        if (serverThread != null)
        {
            LOGGER.error("HTTP server is already running");
            return;
        }
        channel = null;
        running = true;
        serverThread = new Thread(() ->
        {
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try
            {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .handler(FMLLoader.isProduction() ? new ChannelInboundHandlerAdapter() : new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<Channel>()
                        {
                            @Override
                            protected void initChannel(Channel ch)
                            {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(new HttpRequestDecoder());
                                p.addLast(new HttpResponseEncoder());
                                p.addLast(new ServerHttpHandler(new Cache(server)));
                            }
                        });
                channel = bootstrap.bind(server.getPort() + 1).sync().channel();
                LOGGER.info("Opened HTTP server on port " + (server.getPort() + 1));
                channel.closeFuture().sync();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
            channel = null;
            running = false;
        }, "HTTP Server Thread");
        serverThread.start();
    }

    /**
     * Waits until the server is fully started then shuts it down and waits for the thread to terminate.
     */
    public static synchronized void shutdown()
    {
        running = false;
        if (serverThread == null || !serverThread.isAlive())
            return;
        while (channel == null)
        {
        }
        try
        {
            channel.close();
            serverThread.join();
        }
        catch (InterruptedException e)
        {
            LOGGER.error("Failed to shut down HTTP server", e);
        }
        LOGGER.info("Shut down HTTP server");
        serverThread = null;
        channel = null;
    }

    /**
     * @return Whether or not the server thread is running
     */
    public static boolean isRunning()
    {
        return running;
    }

    private static class Cache implements HttpCache
    {
        private final MinecraftServer server;
        private final Map<Path, CompletableFuture<byte[]>> fileFutures;
        private volatile CompletableFuture<byte[]> favicon;

        private Cache(MinecraftServer server)
        {
            this.server = server;
            this.fileFutures = new ConcurrentHashMap<>();
            this.favicon = null;
        }

        private static byte[] createIco(MinecraftServer server)
        {
            File file = server.getFile("server-icon.png");
            if (!file.exists())
            {
                LevelStorageSource.LevelStorageAccess save = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, server, "field_71310_m");
                file = save != null ? save.getIconFile() : null;
            }

            if (file != null && file.isFile())
            {
                try
                {
                    BufferedImage bufferedimage = ImageIO.read(file);
                    Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                    BufferedImage img = new BufferedImage(bufferedimage.getWidth(), bufferedimage.getHeight() * 2, BufferedImage.TYPE_INT_RGB);// note the double height
                    Graphics g = img.getGraphics();
                    g.drawImage(bufferedimage, 0, bufferedimage.getHeight(), null);// added 16 to y coordinate
                    byte[] imgBytes = getImgBytes(img);
                    int fileSize = imgBytes.length + 22;
                    ByteBuffer bytes = ByteBuffer.allocate(fileSize);
                    bytes.order(ByteOrder.LITTLE_ENDIAN);
                    bytes.putShort((short) 0);//Reserved must be 0
                    bytes.putShort((short) 1);//Image type
                    bytes.putShort((short) 1);//Number of images in file
                    bytes.put((byte) img.getWidth());//image width
                    bytes.put((byte) (img.getHeight() >> 1));//image height, half the BMP height
                    bytes.put((byte) 0);//number of colors in color palette
                    bytes.put((byte) 0);//reserved must be 0
                    bytes.putShort((short) 0);//color planes
                    bytes.putShort((short) 0);//bits per pixel
                    bytes.putInt(imgBytes.length);//image size
                    bytes.putInt(22);//image offset
                    bytes.put(imgBytes);
                    return bytes.array();
                }
                catch (Exception e)
                {
                    LOGGER.error("Couldn't load server icon", e);
                }
            }
            return null;
        }

        private static byte[] getImgBytes(BufferedImage img) throws IOException
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "bmp", bos);
            byte[] bytes = bos.toByteArray();
            return Arrays.copyOfRange(bytes, 14, bytes.length);
        }

        @Override
        public <T> CompletableFuture<T> runServerTask(Function<MinecraftServer, T> request)
        {
            if (this.server == null)
                throw new UnsupportedOperationException("No Minecraft Server is running.");
            return CompletableFuture.supplyAsync(() -> request.apply(this.server), this.server);
        }

        @Override
        public synchronized CompletableFuture<byte[]> getFavicon()
        {
            if (this.favicon == null)
                this.favicon = CompletableFuture.supplyAsync(() -> createIco(this.server), Util.ioPool());
            return this.favicon;
        }

        @Override
        public synchronized CompletableFuture<byte[]> getFile(Path location)
        {
            return this.fileFutures.computeIfAbsent(location, file -> CompletableFuture.supplyAsync(() ->
            {
                if (!Files.exists(file))
                {
                    LOGGER.warn("File could not be found at: " + file);
                    return null;
                }

                try (FileInputStream is = new FileInputStream(file.toFile()))
                {
                    return IOUtils.toByteArray(is);
                }
                catch (Exception e)
                {
                    LOGGER.error("Failed to load file from: " + file, e);
                    return null;
                }
            }, Util.ioPool()));
        }
    }
}
