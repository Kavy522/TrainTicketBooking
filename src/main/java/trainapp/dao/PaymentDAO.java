package trainapp.dao;

import trainapp.model.Payment;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    public boolean createPayment(Payment payment) {
        String sql = """
            INSERT INTO payments (booking_id, method, transaction_id, amount, status, provider, payment_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, payment.getBookingId());
            stmt.setString(2, payment.getMethod());
            stmt.setString(3, payment.getTransactionId());
            stmt.setDouble(4, payment.getAmount());
            stmt.setString(5, payment.getStatus());
            stmt.setString(6, payment.getProvider());
            stmt.setTimestamp(7, Timestamp.valueOf(payment.getPaymentTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        payment.setPaymentId(generatedKeys.getLong(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating payment: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public Payment getPaymentByBookingId(long bookingId) {
        String sql = """
            SELECT payment_id, booking_id, method, transaction_id, amount, status, provider, payment_time
            FROM payments WHERE booking_id = ? ORDER BY payment_time DESC LIMIT 1
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bookingId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Payment payment = new Payment();
                    payment.setPaymentId(rs.getLong("payment_id"));
                    payment.setBookingId(rs.getLong("booking_id"));
                    payment.setMethod(rs.getString("method"));
                    payment.setTransactionId(rs.getString("transaction_id"));
                    payment.setAmount(rs.getDouble("amount"));
                    payment.setStatus(rs.getString("status"));
                    payment.setProvider(rs.getString("provider"));
                    payment.setPaymentTime(rs.getTimestamp("payment_time").toLocalDateTime());
                    return payment;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting payment by booking ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}
