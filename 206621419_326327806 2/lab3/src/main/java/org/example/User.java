package org.example;

public class User {

    private final String username;
    private final String password;

    // נתונים דינמיים
    private int failedAttempts = 0;
    private long blockedUntilMillis = 0; // timestamp בשניות מילישניות

    public User(String username, String password) throws Exception {
        // משתמש מקורי מה-Lab1 - שמרתי את הבדיקות הפשוטות
        // עבור הדוגמא נשמור את הקונסטרקטור הפשוט (אפשר לשלב בדיקות נוספות אם צריך)
        this.username = username;
        this.password = password;
    }

    public synchronized boolean isBlocked() {
        long now = System.currentTimeMillis();
        return blockedUntilMillis > now;
    }

    public synchronized long getBlockedUntilMillis() {
        return blockedUntilMillis;
    }

    public synchronized void recordFailedAttemptAndMaybeBlock(int maxAttempts, int blockSeconds) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            blockedUntilMillis = System.currentTimeMillis() + blockSeconds * 1000L;
            // ניתן לאפס את הספירה או להשאיר אותה; נסתיר אותה עד שכל התקופה תיגמר
            failedAttempts = 0;
        }
    }

    public synchronized void resetAttempts() {
        failedAttempts = 0;
    }

    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    // לצורכי הדפסה/לוג
    @Override
    public String toString() {
        return username + " " + password;
    }
}
