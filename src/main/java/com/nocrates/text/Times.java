package com.nocrates.text;

/** Human cooldown formatting through the language file (time-days/hours/minutes/seconds). */
public final class Times {

    private Times() {
    }

    public static String format(Lang lang, long seconds) {
        long s = Math.max(0, seconds);
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        String key;
        if (d > 0) key = "time-days";
        else if (h > 0) key = "time-hours";
        else if (m > 0) key = "time-minutes";
        else key = "time-seconds";
        return lang.rawString(key)
                .replace("<d>", String.valueOf(d))
                .replace("<h>", String.valueOf(h))
                .replace("<m>", String.valueOf(m))
                .replace("<s>", String.valueOf(sec));
    }
}
