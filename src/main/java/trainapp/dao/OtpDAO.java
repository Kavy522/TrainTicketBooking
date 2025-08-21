package trainapp.dao;

import trainapp.model.OtpRecord;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Data Access Object (DAO) for OTP (One-Time Password) record management.
 * Handles creation, verification, lifecycle management, and cleanup of OTP records
 * used for password reset and email verification workflows.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Save new OTP records with expiration times</li>
 *   <li>Verify OTP codes with automatic expiration checking</li>
 *   <li>Mark OTPs as used to prevent replay attacks</li>
 *   <li>Cleanup expired OTP records for database maintenance</li>
 *   <li>Automatic lifecycle management (creation → verification → cleanup)</li>
 * </ul>
 *
 * <p>Security features include:
 * <ul>
 *   <li>Time-based expiration validation</li>
 *   <li>Single-use enforcement via usage tracking</li>
 *   <li>Automatic cleanup of expired/used records</li>
 *   <li>SQL injection prevention through prepared statements</li>
 * </ul>
 */
public class OtpDAO {

    // -------------------------------------------------------------------------
    // Database Connection
    // -------------------------------------------------------------------------

    /**
     * Database connection instance for all OTP operations
     */
    private final Connection connection;

    /**
     * Constructor that initializes the database connection.
     * Uses DBConnection utility to establish connection to the database.
     */
    public OtpDAO() {
        this.connection = DBConnection.getConnection();
    }

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Saves a new OTP record to the database with automatic ID generation.
     * Creates a time-bound, single-use OTP entry linked to an email address.
     *
     * <p>The generated OTP ID is automatically assigned to the provided OtpRecord
     * object upon successful creation.
     *
     * @param otpRecord OTP record containing email, code, expiry time, and metadata
     * @return true if OTP was saved successfully and ID assigned, false otherwise
     * @throws IllegalArgumentException if otpRecord is null or has invalid data
     */
    public boolean saveOtp(OtpRecord otpRecord) {
        if (otpRecord == null) {
            throw new IllegalArgumentException("OTP record cannot be null");
        }
        if (otpRecord.getEmail() == null || otpRecord.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (otpRecord.getOtpCode() == null || otpRecord.getOtpCode().trim().isEmpty()) {
            throw new IllegalArgumentException("OTP code cannot be null or empty");
        }
        if (otpRecord.getExpiryTime() == null) {
            throw new IllegalArgumentException("Expiry time cannot be null");
        }

        String sql = """
                INSERT INTO otp_records (email, otp_code, expiry_time, is_used, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, otpRecord.getEmail());
            stmt.setString(2, otpRecord.getOtpCode());
            stmt.setTimestamp(3, Timestamp.valueOf(otpRecord.getExpiryTime()));
            stmt.setBoolean(4, otpRecord.isUsed());
            stmt.setTimestamp(5, Timestamp.valueOf(otpRecord.getCreatedAt()));

            int result = stmt.executeUpdate();
            if (result > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        otpRecord.setId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving OTP: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Verification Operations
    // -------------------------------------------------------------------------

    /**
     * Verifies an OTP code for a given email address.
     * Checks for code validity, expiration status, and usage status.
     * Automatically marks valid OTPs as used and cleans up expired ones.
     *
     * <p>Verification process:
     * <ol>
     *   <li>Find the most recent unused OTP for the email/code combination</li>
     *   <li>Check if the OTP has expired</li>
     *   <li>If valid and not expired, mark as used and return true</li>
     *   <li>If expired, delete the OTP and return false</li>
     * </ol>
     *
     * @param email   the email address associated with the OTP
     * @param otpCode the OTP code to verify
     * @return true if OTP is valid and not expired, false otherwise
     */
    public boolean verifyOtp(String email, String otpCode) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            return false;
        }

        String sql = """
                SELECT id, email, otp_code, expiry_time, is_used 
                FROM otp_records 
                WHERE email = ? AND otp_code = ? AND is_used = false
                ORDER BY created_at DESC 
                LIMIT 1
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, otpCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    LocalDateTime expiryTime = rs.getTimestamp("expiry_time").toLocalDateTime();
                    boolean isExpired = LocalDateTime.now().isAfter(expiryTime);

                    if (!isExpired) {
                        // Mark OTP as used to prevent reuse
                        markOtpAsUsed(rs.getInt("id"));
                        return true;
                    } else {
                        // Clean up expired OTP
                        deleteOtp(rs.getInt("id"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Lifecycle Management Operations
    // -------------------------------------------------------------------------

    /**
     * Marks an OTP as used to prevent replay attacks.
     * Called automatically during successful verification.
     *
     * @param otpId the unique ID of the OTP record to mark as used
     */
    private void markOtpAsUsed(int otpId) {
        String sql = "UPDATE otp_records SET is_used = true WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, otpId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking OTP as used: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes a specific OTP record by ID.
     * Used for cleaning up expired or invalid OTPs.
     *
     * @param otpId the unique ID of the OTP record to delete
     */
    private void deleteOtp(int otpId) {
        String sql = "DELETE FROM otp_records WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, otpId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting OTP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Maintenance Operations
    // -------------------------------------------------------------------------

    /**
     * Performs cleanup of all expired OTP records from the database.
     * Should be called periodically to maintain database performance and security.
     *
     * <p>This method removes all OTP records where the expiry_time is before
     * the current timestamp, regardless of usage status.
     *
     * <p>Recommended usage:
     * <ul>
     *   <li>Call during application startup</li>
     *   <li>Schedule as a periodic maintenance task</li>
     *   <li>Call before generating new OTPs in high-volume scenarios</li>
     * </ul>
     */
    public void cleanupExpiredOtps() {
        String sql = "DELETE FROM otp_records WHERE expiry_time < ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            int deleted = stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error cleaning up expired OTPs: " + e.getMessage());
            e.printStackTrace();
        }
    }
}