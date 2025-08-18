package trainapp.dao;

import trainapp.model.Payment;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for payment record management.
 * Handles creation and retrieval of payment transactions associated with train bookings.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Create payment records with transaction details and status tracking</li>
 *   <li>Retrieve payment information by booking ID</li>
 *   <li>Support multiple payment methods and providers (Razorpay, etc.)</li>
 *   <li>Track payment status (success, failed, pending, refunded)</li>
 *   <li>Automatic payment ID generation and assignment</li>
 * </ul>
 *
 * <p>Security and reliability features:
 * <ul>
 *   <li>SQL injection prevention through prepared statements</li>
 *   <li>Proper resource management with try-with-resources</li>
 *   <li>Transaction ID tracking for audit and reconciliation</li>
 *   <li>Timestamp recording for payment processing analytics</li>
 * </ul>
 */
public class PaymentDAO {

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new payment record in the database with automatic ID generation.
     * Records payment transaction details including method, provider, amount, and status.
     *
     * <p>The generated payment ID is automatically assigned to the provided Payment
     * object upon successful creation for further reference.
     *
     * @param payment Payment object containing booking ID, transaction details, amount, and status
     * @return true if payment was created successfully and ID assigned, false otherwise
     * @throws IllegalArgumentException if payment is null or has invalid data
     */
    public boolean createPayment(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }
        if (payment.getBookingId() <= 0) {
            throw new IllegalArgumentException("Payment must have a valid booking ID");
        }
        if (payment.getAmount() < 0) {
            throw new IllegalArgumentException("Payment amount cannot be negative");
        }
        if (payment.getPaymentTime() == null) {
            throw new IllegalArgumentException("Payment time cannot be null");
        }

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

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves the most recent payment record for a specific booking.
     * Useful for finding the latest payment attempt or successful transaction.
     *
     * <p>If multiple payments exist for a booking (e.g., failed attempts followed by success),
     * this method returns the most recent one based on payment_time.
     *
     * @param bookingId the booking ID to find payment for
     * @return Payment object if found, null if no payment exists for the booking
     */
    public Payment getPaymentByBookingId(long bookingId) {
        if (bookingId <= 0) {
            return null;
        }

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