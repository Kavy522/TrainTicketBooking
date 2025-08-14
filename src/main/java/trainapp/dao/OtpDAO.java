package trainapp.dao;

import trainapp.model.OtpRecord;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class OtpDAO {

    private final Connection connection;

    public OtpDAO() {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Save OTP record to database
     */
    public boolean saveOtp(OtpRecord otpRecord) {
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
        }

        return false;
    }

    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String email, String otpCode) {
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
                        // Mark OTP as used
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
        }

        return false;
    }

    /**
     * Mark OTP as used
     */
    private void markOtpAsUsed(int otpId) {
        String sql = "UPDATE otp_records SET is_used = true WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, otpId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking OTP as used: " + e.getMessage());
        }
    }

    /**
     * Delete expired or used OTP
     */
    private void deleteOtp(int otpId) {
        String sql = "DELETE FROM otp_records WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, otpId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting OTP: " + e.getMessage());
        }
    }

    /**
     * Clean up expired OTPs
     */
    public void cleanupExpiredOtps() {
        String sql = "DELETE FROM otp_records WHERE expiry_time < ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            int deleted = stmt.executeUpdate();
            System.out.println("Cleaned up " + deleted + " expired OTP records");
        } catch (SQLException e) {
            System.err.println("Error cleaning up expired OTPs: " + e.getMessage());
        }
    }
}
