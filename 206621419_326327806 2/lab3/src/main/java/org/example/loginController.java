package org.example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class loginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;

    private final UserManager userManager = new UserManager();

    // ספירת ניסיונות גלובלית בסשן זה (כדי לממש דרישה 4)
    private final AtomicInteger sessionFailedAttempts = new AtomicInteger(0);
    private volatile boolean sessionLocked = false;

    private Timer sessionTimer = new Timer(true);

    @FXML
    private void initialize() {
        // אפשר להציג בהתחלה מידע קטן על הגבולות
        messageLabel.setText("Enter username and password");
    }

    @FXML
    private void handleLogin() {
        if (sessionLocked) {
            messageLabel.setText("System locked temporarily. Please wait.");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            return;
        }

        // ננעל UI בזמן עיבוד
        setUIEnabled(false);
        messageLabel.setText("Checking...");

        userManager.attemptLogin(username, password, result -> {
            Platform.runLater(() -> {
                setUIEnabled(true);
                switch (result) {
                    case SUCCESS:
                        sessionFailedAttempts.set(0); // איפוס גלובלי בסשן אחרי הצלחה
                        openWelcomeScreen(username);
                        break;

                    case USER_NOT_FOUND:
                        messageLabel.setText("User not found");
                        incrementSessionFailureAndMaybeLock();
                        break;

                    case INVALID_CREDENTIALS:
                        messageLabel.setText("Invalid username or password");
                        incrementSessionFailureAndMaybeLock();
                        break;

                    case BLOCKED:
                        // היוזר חסום לפי User.blockedUntilMillis
                        messageLabel.setText("User is temporarily blocked. Try later.");
                        break;

                    default:
                        messageLabel.setText("Error during authentication");
                        break;
                }
            });
        });
    }

    private void openWelcomeScreen(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/welcome.fxml"));
            Scene scene = new Scene(loader.load());
            WelcomeController wc = loader.getController();
            wc.setWelcomeText("Welcome, " + username + "!");
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Failed to open welcome screen");
        }
    }

    private void incrementSessionFailureAndMaybeLock() {
        int cur = sessionFailedAttempts.incrementAndGet();
        int max = AuthConfig.getMaxAttempts();
        if (cur >= max) {
            // נעצור ניסיון נוסף, נציין למשתמש ונמנע כניסות למשך t שניות
            int t = AuthConfig.getBlockSeconds();
            sessionLocked = true;
            messageLabel.setText("Too many failed attempts. System locked for " + t + " seconds.");
            setUIEnabled(false);

            // schedule unlock after t seconds
            sessionTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    sessionLocked = false;
                    sessionFailedAttempts.set(0);
                    Platform.runLater(() -> {
                        messageLabel.setText("You can try again");
                        setUIEnabled(true);
                    });
                }
            }, t * 1000L);
        } else {
            int remaining = max - cur;
            messageLabel.setText("Invalid credentials. " + remaining + " attempts left in this cycle.");
        }
    }

    private void setUIEnabled(boolean enabled) {
        usernameField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        loginButton.setDisable(!enabled);
    }

    // נקרא כשפורקים את ה־Controller (לא חובה)
    public void shutdown() {
        userManager.shutdown();
        sessionTimer.cancel();
    }
}
