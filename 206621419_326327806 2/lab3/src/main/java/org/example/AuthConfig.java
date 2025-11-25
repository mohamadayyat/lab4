package org.example;

/**
 * הגדרות גלובליות שנקבעות ב־App.init() מהפרמטרים של שורת הפקודה
 */
public class AuthConfig {
    private static int maxAttempts = 3;
    private static int blockSeconds = 30;

    public static void init(int n, int t) {
        maxAttempts = n;
        blockSeconds = t;
    }

    public static int getMaxAttempts() {
        return maxAttempts;
    }

    public static int getBlockSeconds() {
        return blockSeconds;
    }
}
