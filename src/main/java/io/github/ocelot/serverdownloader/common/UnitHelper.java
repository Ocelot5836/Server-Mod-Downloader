package io.github.ocelot.serverdownloader.common;

import net.minecraft.Util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

/**
 * @author Ocelot
 */
public class UnitHelper
{
    private static final DecimalFormat FORMAT = Util.make(new DecimalFormat("#.#"), format -> format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

    public static String abbreviateSize(long bytes)
    {
        if (bytes <= 1024)
            return bytes + " b";
        if (bytes <= 1024 * 1024)
            return FORMAT.format(bytes / 1024.0) + " Kib";
        if (bytes <= 1024 * 1024 * 1024)
            return FORMAT.format(bytes / 1024.0 / 1024.0) + " Mib";
        return FORMAT.format(bytes / 1024.0 / 1024.0 / 1024.0) + " Gib";
    }

    public static String abbreviateTime(long sourceTime, TimeUnit sourceUnit)
    {
        TimeUnit unit = chooseUnit(sourceUnit.toNanos(sourceTime));
        double value = (double) sourceTime / sourceUnit.convert(1, unit);
        return (int) value + abbreviate(unit);
    }

    private static TimeUnit chooseUnit(long nanos)
    {
        if (DAYS.convert(nanos, NANOSECONDS) > 0)
        {
            return DAYS;
        }
        if (HOURS.convert(nanos, NANOSECONDS) > 0)
        {
            return HOURS;
        }
        if (MINUTES.convert(nanos, NANOSECONDS) > 0)
        {
            return MINUTES;
        }
        if (SECONDS.convert(nanos, NANOSECONDS) > 0)
        {
            return SECONDS;
        }
        if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0)
        {
            return MILLISECONDS;
        }
        if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0)
        {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    private static String abbreviate(TimeUnit unit)
    {
        switch (unit)
        {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                return "\u03bcs"; // μs
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new AssertionError();
        }
    }
}
