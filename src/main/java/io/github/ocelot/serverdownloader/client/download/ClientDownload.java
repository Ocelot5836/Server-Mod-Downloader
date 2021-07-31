package io.github.ocelot.serverdownloader.client.download;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <p>A download for a file that will be completed in the future.</p>
 *
 * @author Ocelot
 */
public class ClientDownload implements Future<Void>
{
    private final long size;
    private final CompletableFuture<Void> completionFuture;
    private volatile Status status;
    private volatile long bytesDownloaded;
    private volatile boolean cancelled;

    ClientDownload(long size)
    {
        this.size = size;
        this.completionFuture = new CompletableFuture<>();
        this.status = Status.DOWNLOADING;
        this.bytesDownloaded = 0;
        this.cancelled = false;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        if (this.status != Status.DOWNLOADING || this.cancelled)
            return false;
        this.cancelled = true;
        return true;
    }

    /**
     * @return The known size of the download or <code>-1</code> if not specified
     */
    public long getSize()
    {
        return size;
    }

    /**
     * @return The count of bytes downloaded
     */
    public long getBytesDownloaded()
    {
        return bytesDownloaded;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public boolean isDone()
    {
        return this.status != Status.DOWNLOADING;
    }

    @Override
    public Void get()
    {
        throw new UnsupportedOperationException("ClientDownload does not have a result");
    }

    @Override
    public Void get(long timeout, @NotNull TimeUnit unit)
    {
        throw new UnsupportedOperationException("ClientDownload does not have a result");
    }

    /**
     * @return If this file failed to download
     */
    public boolean hasFailed()
    {
        return this.status == Status.FAILED || this.status == Status.FAILED_IGNORE_ERRORS;
    }

    /**
     * @return If download errors should be ignored
     */
    public boolean shouldIgnoreErrors()
    {
        return this.status == Status.FAILED_IGNORE_ERRORS;
    }

    /**
     * @return The current percentage of files downloaded
     */
    public double getDownloadPercentage()
    {
        return this.size == -1 ? 1.0 : (float) this.bytesDownloaded / (float) this.size;
    }

    /**
     * @return A future that will complete when {@link #isDone()} is true
     */
    public CompletableFuture<Void> getCompletionFuture()
    {
        return completionFuture;
    }

    synchronized void addBytesDownloaded(long amount)
    {
        this.bytesDownloaded += amount;
    }

    synchronized void completeSuccessfully()
    {
        this.status = Status.SUCCESS;
        this.completionFuture.complete(null);
    }

    synchronized void completeExceptionally(Throwable t, boolean ignoreErrors)
    {
        this.status = ignoreErrors ? Status.FAILED_IGNORE_ERRORS : Status.FAILED;
        this.completionFuture.completeExceptionally(t);
    }

    synchronized void completeCancelled()
    {
        this.status = Status.FAILED;
        this.completionFuture.complete(null);
    }

    /**
     * <p>The status of a file download.</p>
     *
     * @author Ocelot
     */
    public enum Status
    {
        DOWNLOADING, SUCCESS, FAILED, FAILED_IGNORE_ERRORS
    }
}
