package io.github.ocelot.client.download;

import java.nio.file.Path;

/**
 * @author Ocelot
 */
public class ClientDownload
{
    private final String url;
    private final long size;
    private final Path location;
    private long bytesDownloaded;
    private Status status;
    private boolean cancelled;

    ClientDownload(String url, long size, Path location)
    {
        this.url = url;
        this.size = size;
        this.location = location;
        this.status = Status.DOWNLOADING;
        this.cancelled = false;
    }

    public void cancel()
    {
        if (this.status != Status.DOWNLOADING)
            return;
        this.cancelled = true;
    }

    public String getUrl()
    {
        return url;
    }

    /**
     * @return The known size of the download or <code>-1</code> if not specified
     */
    public long getSize()
    {
        return size;
    }

    public Path getLocation()
    {
        return location;
    }

    /**
     * @return The count of bytes downloaded
     */
    public long getBytesDownloaded()
    {
        return bytesDownloaded;
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    public boolean isCompleted()
    {
        return this.status != Status.DOWNLOADING;
    }

    public boolean isFailed()
    {
        return this.status == Status.FAILED;
    }

    void addBytesDownloaded(long amount)
    {
        this.bytesDownloaded += amount;
    }

    void setStatus(Status status)
    {
        this.status = status;
    }

    public double getDownloadPercentage()
    {
        return this.size == -1 ? 1.0 : (float) this.bytesDownloaded / (float) this.size;
    }

    public enum Status
    {
        DOWNLOADING, SUCCESS, FAILED
    }
}
