package io.github.ocelot.server.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import net.minecraft.Util;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.apache.commons.codec.digest.DigestUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Ocelot
 */
public class ServerHttpHandler extends SimpleChannelInboundHandler<Object>
{
    private final HttpCache cache;
    private HttpRequest request;
    private String contentType = "text/raw";
    private String contentDisposition = null;
    private byte[] responseData = null;

    public ServerHttpHandler(HttpCache cache)
    {
        this.cache = cache;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        ctx.flush();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws URISyntaxException
    {
        if (msg instanceof HttpRequest)
        {
            HttpRequest request = this.request = (HttpRequest) msg;

            if (HttpUtil.is100ContinueExpected(request))
            {
                FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER);
                ctx.write(response);
                return;
            }

            URI uri = new URI(request.uri());
            String path = uri.getPath();
            if (path.startsWith("/"))
                path = path.substring(1);
            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
            String[] paths = path.split("/");
            Map<String, List<String>> parameters = new QueryStringDecoder(request.uri()).parameters();

            this.contentType = "text/raw";
            this.contentDisposition = null;
            this.responseData = null;
            if (!paths[0].isEmpty())
            {
                switch (paths[0])
                {
                    case "favicon.ico":
                    {
                        byte[] data = this.cache.getFavicon().join();
                        if (data != null)
                        {
                            this.contentType = "image/png";
                            this.responseData = data;
                        }
                        break;
                    }
                    case "download":
                    {
                        parameters.getOrDefault("mod", Collections.emptyList()).stream().filter(BlacklistedServerModLoader::isValid).findFirst().ifPresent(modId ->
                        {
                            ModFileInfo info = ModList.get().getModFileById(modId);
                            if (info != null)
                            {
                                this.contentType = "application/java-archive";
                                this.contentDisposition = "attachment; filename=\"" + info.getFile().getFileName() + "\"";
                                this.responseData = this.cache.getFile(info.getFile().getFilePath()).join();
                            }
                        });
                        break;
                    }
//                    case "validate":
//                    {
//                        parameters.getOrDefault("mod", Collections.emptyList()).stream().filter(BlacklistedServerModLoader::isValid).findFirst().ifPresent(modId ->
//                        {
//                            ModFileInfo info = ModList.get().getModFileById(modId);
//                            if (info != null)
//                            {
//                                this.contentType = "text/raw";
//                                this.responseData = this.cache.getFile(info.getFile().getFilePath()).thenApplyAsync(DigestUtils::sha1Hex, Util.ioPool()).join().getBytes(StandardCharsets.UTF_8);
//                            }
//                        });
//                        break;
//                    }
                }
            }
        }

        if (msg instanceof LastHttpContent)
        {
            writeResponse(ctx, (LastHttpContent) msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        ctx.close();
    }

    private void writeResponse(ChannelHandlerContext ctx, LastHttpContent trailer)
    {
        boolean keepAlive = HttpUtil.isKeepAlive(this.request);

        if (this.responseData == null)
        {
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            if (keepAlive)
                httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(httpResponse);
            if (!keepAlive)
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, trailer.decoderResult().isSuccess() ? OK : BAD_REQUEST, Unpooled.copiedBuffer(this.responseData));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, this.contentType);
        if (this.contentDisposition != null)
            httpResponse.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, this.contentDisposition);

        if (keepAlive)
        {
            httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ctx.write(httpResponse);

        if (!keepAlive)
        {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
