package io.github.ocelot.server.http;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * <p>Caches and delegates http requests to the main server when necessary.</p>
 *
 * @author Ocelot
 */
public interface HttpCache
{
    /**
     * Executes a server query at some point in the future.
     *
     * @param request The request to make once the server responds
     * @param <T>     The type of data requested
     * @return A future of the data requested
     */
    <T> CompletableFuture<T> runServerTask(Function<MinecraftServer, T> request);

    /**
     * @return The site favicon image that will exist at some point in the future
     */
    CompletableFuture<byte[]> getFavicon();

    /**
     * Retrieves a file from the server file system.
     *
     * @param location The location of the file to retrieve
     * @return A future of the file data
     */
    CompletableFuture<byte[]> getFile(Path location);
}
