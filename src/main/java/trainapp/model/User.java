package trainapp.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User {
    private IntegerProperty userId = new SimpleIntegerProperty();
    private StringProperty name = new SimpleStringProperty();
    private StringProperty email = new SimpleStringProperty();
    private StringProperty phone = new SimpleStringProperty();
    private String passwordHash; // Keep as regular field for security
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String name, String email, String phone) {
        this.name.set(name);
        this.email.set(email);
        this.phone.set(phone);
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters with JavaFX Properties
    public int getUserId() { return userId.get(); }
    public void setUserId(int userId) { this.userId.set(userId); }
    public IntegerProperty userIdProperty() { return userId; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public String getEmail() { return email.get(); }
    public void setEmail(String email) { this.email.set(email); }
    public StringProperty emailProperty() { return email; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public StringProperty phoneProperty() { return phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    // Helper methods for display
    public String getCreatedAtFormatted() {
        return createdAt != null ? createdAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "";
    }

    public String getLastLoginFormatted() {
        return lastLogin != null ? lastLogin.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "Never";
    }

    public String getStatusDisplay() {
        // Simple status based on last login (you can modify this logic)
        if (lastLogin == null) return "New";
        return lastLogin.isAfter(LocalDateTime.now().minusDays(30)) ? "Active" : "Inactive";
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + getUserId() +
                ", name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", phone='" + getPhone() + '\'' +
                '}';
    }
}