package trainapp.model;

import java.time.LocalDateTime;

public class OtpRecord {
    private int id;
    private String email;
    private String otpCode;
    private LocalDateTime expiryTime;
    private boolean isUsed;
    private LocalDateTime createdAt;

    // Constructors
    public OtpRecord() {}

    public OtpRecord(String email, String otpCode, LocalDateTime expiryTime) {
        this.email = email;
        this.otpCode = otpCode;
        this.expiryTime = expiryTime;
        this.isUsed = false;
        this.createdAt = expiryTime.minusMinutes(10);
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { isUsed = used; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryTime);
    }
}
