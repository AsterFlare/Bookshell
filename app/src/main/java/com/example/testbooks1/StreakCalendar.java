package com.example.testbooks1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;

@SuppressWarnings("unused")
public final class StreakCalendar {

    public static final String ZONE_ID = "Asia/Manila";

    private StreakCalendar() {}

    public static TimeZone zone() {
        return TimeZone.getTimeZone(ZONE_ID);
    }

    public static String streakDayKey() {
        SimpleDateFormat dayKey = new SimpleDateFormat("yyyyMMdd", Locale.US);
        dayKey.setTimeZone(zone());
        return dayKey.format(Calendar.getInstance().getTime());
    }

    /** Monday=0 … Sunday=6 in {@link #zone()}. */
    public static int dayOfWeekIndexStreak() {
        Calendar cal = Calendar.getInstance(zone(), Locale.US);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return (dow + 5) % 7;
    }

    public static int dayGapStreak(@Nullable String lastDay, @NonNull String todayDay) {
        if (lastDay == null || lastDay.isEmpty()) {
            return Integer.MAX_VALUE;
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
            fmt.setLenient(false);
            fmt.setTimeZone(zone());
            Date dLast = fmt.parse(lastDay);
            Date dToday = fmt.parse(todayDay);
            if (dLast == null || dToday == null) {
                return Integer.MAX_VALUE;
            }
            long lastMs = dLast.getTime();
            long todayMs = dToday.getTime();
            long days = (todayMs - lastMs) / (24L * 60L * 60L * 1000L);
            return (int) days;
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public static long millisUntilNextStreakMidnight() {
        TimeZone tz = zone();
        Calendar now = Calendar.getInstance(tz, Locale.US);
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_MONTH, 1);
        }
        return Math.max(1L, next.getTimeInMillis() - now.getTimeInMillis());
    }
}
