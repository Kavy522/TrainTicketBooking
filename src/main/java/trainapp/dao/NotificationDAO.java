package trainapp.dao;

import trainapp.model.Notification;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class NotificationDAO {

    public boolean createNotification(Notification notification) {
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