package com.example.testbooks1;

import android.content.Context;

import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class BadgeRules {

    public static final int TOTAL_BADGES = 6;

    private BadgeRules() {
    }

    public static long readStatLong(DataSnapshot statsSnapshot, String key) {
        if (statsSnapshot == null || !statsSnapshot.exists()) {
            return 0L;
        }
        DataSnapshot child = statsSnapshot.child(key);
        if (!child.exists()) {
            return 0L;
        }
        Long l = child.getValue(Long.class);
        if (l != null) {
            return l;
        }
        Integer i = child.getValue(Integer.class);
        return i != null ? i.longValue() : 0L;
    }

    public static int countUnlocked(long completed, long reviews, long readingLists) {
        int n = 0;
        if (completed >= 1) {
            n++;
        }
        if (completed >= 10) {
            n++;
        }
        if (completed >= 50) {
            n++;
        }
        if (completed >= 100) {
            n++;
        }
        if (reviews >= 1) {
            n++;
        }
        if (readingLists >= 1) {
            n++;
        }
        return n;
    }

    public static class BadgeRow {
        public final String name;
        public final boolean unlocked;
        public final int accentColorRes;

        public BadgeRow(String name, boolean unlocked, int accentColorRes) {
            this.name = name;
            this.unlocked = unlocked;
            this.accentColorRes = accentColorRes;
        }
    }

    private static final int[] BADGE_ACCENTS = {
            R.color.badge_accent_1,
            R.color.badge_accent_2,
            R.color.badge_accent_3,
            R.color.badge_accent_4,
            R.color.badge_accent_5,
            R.color.badge_accent_6,
    };

    public static List<BadgeRow> badgeRowsFromStats(Context ctx, long completed, long reviews, long readingLists) {
        ArrayList<BadgeRow> list = new ArrayList<>(TOTAL_BADGES);
        list.add(new BadgeRow(ctx.getString(R.string.badge_first_voyage), completed >= 1, BADGE_ACCENTS[0]));
        list.add(new BadgeRow(ctx.getString(R.string.badge_novice_reader), completed >= 10, BADGE_ACCENTS[1]));
        list.add(new BadgeRow(ctx.getString(R.string.badge_bookworm), completed >= 50, BADGE_ACCENTS[2]));
        list.add(new BadgeRow(ctx.getString(R.string.badge_ancient_pearl), completed >= 100, BADGE_ACCENTS[3]));
        list.add(new BadgeRow(ctx.getString(R.string.badge_starfish_rater), reviews >= 1, BADGE_ACCENTS[4]));
        list.add(new BadgeRow(ctx.getString(R.string.badge_tidal_curator), readingLists >= 1, BADGE_ACCENTS[5]));
        return list;
    }

    public static String levelNameForUnlockedCount(Context ctx, int unlockedCount) {
        if (unlockedCount >= 6) {
            return ctx.getString(R.string.level_ocean_master);
        }
        if (unlockedCount >= 4) {
            return ctx.getString(R.string.level_sea_mover);
        }
        if (unlockedCount >= 2) {
            return ctx.getString(R.string.level_explorer);
        }
        return ctx.getString(R.string.level_newcomer);
    }

    public static int collectorLevelTier(int unlockedCount) {
        if (unlockedCount >= 6) {
            return 3;
        }
        if (unlockedCount >= 4) {
            return 2;
        }
        if (unlockedCount >= 2) {
            return 1;
        }
        return 0;
    }

    public static int badgeDrawableRes(int badgeIndex, boolean unlocked) {
        switch (badgeIndex) {
            case 0:
                return unlocked ? R.drawable.first_voyage : R.drawable.first_voyage_locked;
            case 1:
                return unlocked ? R.drawable.novice_reader : R.drawable.novice_reader_locked;
            case 2:
                return unlocked ? R.drawable.book_worm : R.drawable.book_worm_locked;
            case 3:
                return unlocked ? R.drawable.ancient_pearl : R.drawable.ancient_pearl_locked;
            case 4:
                return unlocked ? R.drawable.starfish_rater : R.drawable.starfish_rater_locked;
            case 5:
                return unlocked ? R.drawable.tidal_curator : R.drawable.tidal_curator_locked;
            default:
                return unlocked ? R.drawable.first_voyage : R.drawable.first_voyage_locked;
        }
    }

    public static int badgeRequirementTextRes(int badgeIndex) {
        switch (badgeIndex) {
            case 0:
                return R.string.badge_requirement_first_voyage;
            case 1:
                return R.string.badge_requirement_novice_reader;
            case 2:
                return R.string.badge_requirement_bookworm;
            case 3:
                return R.string.badge_requirement_ancient_pearl;
            case 4:
                return R.string.badge_requirement_starfish_rater;
            case 5:
                return R.string.badge_requirement_tidal_curator;
            default:
                return R.string.badge_requirement_first_voyage;
        }
    }
}
