package trainapp.dao;

import trainapp.model.Notification;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Data Access Object (DAO) for Notification-related database operations.
 * Manages the persistence of notification records for booking confirmations.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Create notification records with email/SMS status tracking</li>
 *   <li>Track delivery status and timestamps for audit purposes</li>
 *   <li>Link notifications to specific bookings for traceability</li>
 *   <li>Automatic ID generation and assignment</li>
 * </ul>
 *
 * <p>All operations use prepared statements for SQL injection prevention
 * and proper resource management with try-with-resources.
 */
public class NotificationDAO {

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new notification record in the database.
     * Records the status of email and SMS delivery for a specific booking,
     * along with the timestamp when notifications were sent.
     *
     * <p>The method automatically generates and assigns a notification ID
     * to the provided Notification object upon successful creation.
     *
     * @param notification Notification object containing booking ID, delivery status, and timestamp
     * @return true if notification was created successfully and ID was assigned, false otherwise
     * @throws IllegalArgumentException if notification is null or has invalid data
     */
    public boolean createNotification(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("Notification cannot be null");
        }
        if (notification.getBookingId() <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (notification.getSentAt() == null) {
            throw new IllegalArgumentException("Sent timestamp cannot be null");
        }

        String sql = """
                INSERT INTO notifications (booking_id, email_sent, sms_sent, sent_at)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, notification.getBookingId());
            stmt.setBoolean(2, notification.isEmailSent());
            stmt.setBoolean(3, notification.isSmsSent());
            stmt.setTimestamp(4, Timestamp.valueOf(notification.getSentAt()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        notification.setNotificationId(generatedKeys.getLong(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating notification: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
}
