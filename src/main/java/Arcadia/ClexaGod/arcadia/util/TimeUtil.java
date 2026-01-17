package Arcadia.ClexaGod.arcadia.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TimeUtil {

    public static String formatDuration(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            out.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            out.append(minutes).append("m ");
        }
        out.append(seconds).append("s");
        return out.toString().trim();
    }
}
