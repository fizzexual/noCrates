package com.nocrates.editor;

/** Small helpers shared by the editor menus. */
public final class EditorUtil {

    private EditorUtil() {
    }

    public static String cycle(String[] options, String current, boolean backwards) {
        int index = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(current)) {
                index = i;
                break;
            }
        }
        index = (index + (backwards ? -1 : 1) + options.length) % options.length;
        return options[index];
    }

    public static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
