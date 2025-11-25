package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * מנהל המשתמשים והאימות
 * משתמש ב־ExecutorService כדי ליצור חוטים עבור:
 *  - בדיקת התחברות (באופן אסינכרוני)
 *  - עדכון ניסיונות כושלים וחסימה (חוט נפרד)
 */
public class UserManager {

    private final Map<String, User> usersMap;
    private final ExecutorService executor;

    public UserManager() {
        usersMap = loadUsers("/org/example/users.txt");
        executor = Executors.newCachedThreadPool();
    }

    private Map<String, User> loadUsers(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("users.txt not found in resources: " + resourcePath);
                return Map.of();
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
                return r.lines()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(line -> line.split("\\s+"))
                        .filter(parts -> parts.length >= 2)
                        .map(parts -> {
                            try {
                                return new User(parts[0], parts[1]);
                            } catch (Exception e) {
                                System.err.println("Skipping invalid user line: " + String.join(" ", parts) + " -> " + e.getMessage());
                                return null;
                            }
                        })
                        .filter(u -> u != null)
                        .collect(Collectors.toMap(User::getUsername, u -> u));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of();
        }
    }

    /**
     * מבקש ניסיון התחברות אסינכרוני.
     * הפונקציה מריצה בחוט נפרד בדיקה שמוודאת אם המשתמש חסום וגם אם פרטי הכניסה נכונים.
     * אם פרטי הכניסה שגויים, מריצים חוט נוסף לעדכון ניסיונות וכדי לחסום לפי הצורך.
     *
     * התוצאה מוחזרת דרך ה־callback על ה־Executor (ולכן יש לבצע Platform.runLater ב־UI).
     */
    public void attemptLogin(String username, String password, java.util.function.Consumer<AuthResult> callback) {
        executor.submit(() -> {
            try {
                User user = usersMap.get(username);
                if (user == null) {
                    callback.accept(AuthResult.USER_NOT_FOUND);
                    return;
                }

                // חוט בודק נתונים ומוודא חסימה
                Future<AuthResult> checkFuture = executor.submit(() -> {
                    if (user.isBlocked()) {
                        return AuthResult.BLOCKED;
                    }
                    if (user.getPassword().equals(password)) {
                        return AuthResult.SUCCESS;
                    } else {
                        return AuthResult.INVALID_CREDENTIALS;
                    }
                });

                AuthResult result = checkFuture.get(); // נחכה לתוצאה הקטנה

                if (result == AuthResult.SUCCESS) {
                    // איפוס ניסיונות בחוט נפרד
                    executor.submit(user::resetAttempts);
                    callback.accept(AuthResult.SUCCESS);
                    return;
                }

                if (result == AuthResult.INVALID_CREDENTIALS) {
                    // חוט שמעדכן את מספר הניסיונות הכושלים וישחזר חסימה אם צריך
                    executor.submit(() -> {
                        user.recordFailedAttemptAndMaybeBlock(AuthConfig.getMaxAttempts(), AuthConfig.getBlockSeconds());
                    });
                    callback.accept(AuthResult.INVALID_CREDENTIALS);
                    return;
                }

                if (result == AuthResult.BLOCKED) {
                    callback.accept(AuthResult.BLOCKED);
                    return;
                }

                callback.accept(AuthResult.ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                callback.accept(AuthResult.ERROR);
            }
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
