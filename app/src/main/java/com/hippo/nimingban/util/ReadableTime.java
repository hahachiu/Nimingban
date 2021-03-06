/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.nimingban.util;

import android.content.Context;
import android.content.res.Resources;

import com.hippo.nimingban.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class ReadableTime {

    private static Resources sResources;

    public static final long SECOND_MILLIS = 1000l;
    public static final long MINUTE_MILLIS = 60l * SECOND_MILLIS;
    public static final long HOUR_MILLIS = 60l * MINUTE_MILLIS;
    public static final long DAY_MILLIS = 24l * HOUR_MILLIS;
    public static final long YEAR_MILLIS = 365l * DAY_MILLIS;

    public static final int SIZE = 5;

    public static final long[] MULTIPLES = {
            YEAR_MILLIS,
            DAY_MILLIS,
            HOUR_MILLIS,
            MINUTE_MILLIS,
            SECOND_MILLIS
    };

    public static final int[] UNITS = {
            R.plurals.year,
            R.plurals.day,
            R.plurals.hour,
            R.plurals.minute,
            R.plurals.second
    };

    /**
     * Parse the time to user
     */
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault());
    private static final Object sDateFormatLock1 = new Object();

    private static final SimpleDateFormat FILENAMABLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault());
    private static final Object sDateFormatLock2 = new Object();

    static {
        // The website use GMT+08:00, so tell user the same
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
    }

    public static void initialize(Context context) {
        sResources = context.getApplicationContext().getResources();
    }

    public static String getDisplayTime(long time) {
        if (Settings.getPrettyTime()) {
            return getTimeAgo(time);
        } else {
            return getPlainTime(time);
        }
    }

    public static String getPlainTime(long time) {
        synchronized (sDateFormatLock1) {
            return DATE_FORMAT.format(new Date(time));
        }
    }

    public static String getTimeAgo(long time) {
        Resources resources = sResources;

        long now = System.currentTimeMillis();
        if (time > now + (2 * MINUTE_MILLIS) || time <= 0) {
            return resources.getString(R.string.from_the_future);
        }

        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return resources.getString(R.string.just_now);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return resources.getQuantityString(R.plurals.some_minutes_ago, 1, 1);
        } else if (diff < 50 * MINUTE_MILLIS) {
            int minutes = (int) (diff / MINUTE_MILLIS);
            return resources.getQuantityString(R.plurals.some_minutes_ago, minutes, minutes);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return resources.getQuantityString(R.plurals.some_hours_ago, 1, 1);
        } else if (diff < 24 * HOUR_MILLIS) {
            int hours = (int) (diff / HOUR_MILLIS);
            return resources.getQuantityString(R.plurals.some_hours_ago, hours, hours);
        } else if (diff < 48 * HOUR_MILLIS) {
            return resources.getString(R.string.yesterday);
        } else {
            int days = (int) (diff / DAY_MILLIS);
            return resources.getString(R.string.some_days_ago, days);
        }
    }

    public static String getTimeInterval(long time) {
        StringBuilder sb = new StringBuilder();
        Resources resources = sResources;

        long leftover = time;
        boolean start = false;

        for (int i = 0; i < SIZE; i++) {
            long multiple = MULTIPLES[i];
            long quotient = leftover / multiple;
            long remainder = leftover % multiple;
            if (start || quotient != 0 || i == SIZE - 1) {
                if (start) {
                    sb.append(" ");
                }
                sb.append(quotient)
                        .append(" ")
                        .append(resources.getQuantityString(UNITS[i], (int) quotient));
                start = true;
            }
            leftover = remainder;
        }

        return sb.toString();
    }

    public static String getFilenamableTime(long time) {
        synchronized (sDateFormatLock2) {
            return FILENAMABLE_DATE_FORMAT.format(new Date(time));
        }
    }
}
