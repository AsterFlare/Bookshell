package com.example.testbooks1;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Manila-time helpers for streak day keys, weekday index, gaps, midnight,
 * and Firebase weekly bar rollover ({@link #WEEK_KEY_FIELD}).
 */
public final class StreakCalendar {

    public static final String ZONE_ID = "Asia/Manila";

    /** Firebase field: {@code yyyyMMdd} of the Monday for the current weekly bar week. */
    public static final String WEEK_KEY_FIELD = "weeklyBarWeekKey";

    private StreakCalendar() {
    }

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

    /** {@code yyyyMMdd} of the Monday that starts the current week (Manila). */
    @NonNull
    public static String currentMondayKeyManila() {
        TimeZone tz = zone();
        Calendar c = Calendar.getInstance(tz, Locale.US);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dow + 5) % 7;
        c.add(Calendar.DAY_OF_MONTH, -daysFromMonday);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.US);
        fmt.setTimeZone(tz);
        return fmt.format(c.getTime());
    }

    /**
     * If {@link #WEEK_KEY_FIELD} is missing, sets it without clearing bars (migration).
     * If it differs from this week's Monday key, clears {@code weeklyActivity} and updates the key.
     * Does not modify {@code readingStreak} or {@code lastStreakDate}.
     *
     * @return true if a multi-path update was sent (listener should wait for a new snapshot).
     */
    public static boolean applyWeekRolloverIfNeeded(
            @NonNull DatabaseReference statsRef,
            @NonNull DataSnapshot statsSnapshot) {
        String current = currentMondayKeyManila();
        String stored = statsSnapshot.child(WEEK_KEY_FIELD).getValue(String.class);
        if (stored == null) {
            statsRef.child(WEEK_KEY_FIELD).setValue(current);
            return false;
        }
        if (stored.equals(current)) {
            return false;
        }
        Map<String, Object> updates = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            updates.put("weeklyActivity/" + i, 0L);
        }
        updates.put(WEEK_KEY_FIELD, current);
        statsRef.updateChildren(updates);
        return true;
    }
}
