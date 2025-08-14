package trainapp.model;

import java.time.LocalDateTime;

public class Notification {
    private long notificationId;
    private long bookingId;
    private boolean emailSent;
    private boolean smsSent;
    private LocalDateTime sentAt;

    // Constructors
    public Notification() {
        this.sentAt = LocalDateTime.now();
    }

    public Notification(long bookingId, boolean emailSent, boolean smsSent) {
        this.bookingId = bookingId;
        this.emailSent = emailSent;
        this.smsSent = smsSent;
        this.sentAt = LocalDateTime.now();
    }

    // Getters and Setters
    public long getNotificationId() { return notificationId; }
    public void setNotificationId(long notificationId) { this.notificationId = notificationId; }

    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }

    public boolean isSmsSent() { return smsSent; }
    public void setSmsSent(boolean smsSent) { this.smsSent = smsSent; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", bookingId=" + bookingId +
                ", emailSent=" + emailSent +
                ", smsSent=" + smsSent +
                ", sentAt=" + sentAt +
                '}';
    }
}
