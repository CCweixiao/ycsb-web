package com.leo.ycsb.common;

import java.util.concurrent.TimeUnit;

/**
 * Turn seconds remaining into more useful units.
 * i.e. if there are hours or days worth of seconds, use them.
 * @author leojie 2022/1/14 11:28 下午
 */
public final class RemainingFormatter {
    private RemainingFormatter() {
    }

    public static StringBuilder format(long seconds) {
        StringBuilder time = new StringBuilder();
        long days = TimeUnit.SECONDS.toDays(seconds);
        if (days > 0) {
            time.append(days).append(days == 1 ? " day " : " days ");
            seconds -= TimeUnit.DAYS.toSeconds(days);
        }
        long hours = TimeUnit.SECONDS.toHours(seconds);
        if (hours > 0) {
            time.append(hours).append(hours == 1 ? " hour " : " hours ");
            seconds -= TimeUnit.HOURS.toSeconds(hours);
        }
        /* Only include minute granularity if we're < 1 day. */
        if (days < 1) {
            long minutes = TimeUnit.SECONDS.toMinutes(seconds);
            if (minutes > 0) {
                time.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
                seconds -= TimeUnit.MINUTES.toSeconds(seconds);
            }
        }
        /* Only bother to include seconds if we're < 1 minute */
        if (time.length() == 0) {
            time.append(seconds).append(time.length() == 1 ? " second " : " seconds ");
        }
        return time;
    }
}
