package trainapp.service;

import trainapp.dao.OtpDAO;
import trainapp.dao.UserDAO;
import trainapp.dao.AdminDAO;
import trainapp.model.OtpRecord;
import trainapp.model.User;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class PasswordResetService {

    private final OtpDAO otpDAO = new OtpDAO();
    private final EmailService emailService = new EmailService();
    private final UserDAO userDAO = new UserDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    /**
     * Send OTP for password reset
     */
    public PasswordResetResult sendOtp(String email) {
        // Check if email exists in users table
        boolean emailExists = userDAO.emailExists(email);

        if (!emailExists) {
            return PasswordResetResult.error("Email not found in our records.");
        }

        // Clean up any expired OTPs first
        cleanupExpiredOtps();

        // Generate 6-digit OTP
        String otpCode = generateOtp();

        // Set expiry time (10 minutes from now)
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        // Save OTP to database
        OtpRecord otpRecord = new OtpRecord(email, otpCode, expiryTime);

        if (otpDAO.saveOtp(otpRecord)) {
            // Send OTP via email
            if (emailService.sendOtpEmail(email, otpCode)) {
                return PasswordResetResult.success("Verification code sent to your email.");
            } else {
                return PasswordResetResult.error("Failed to send email. Please check your email address and try again.");
            }
        } else {
            return PasswordResetResult.error("Failed to generate verification code. Please try again.");
        }
    }

    /**
     * Verify OTP code
     */
    public PasswordResetResult verifyOtp(String email, String otpCode) {
        if (email == null || email.trim().isEmpty()) {
            return PasswordResetResult.error("Email is required for verification.");
        }

        if (otpCode == null || otpCode.trim().isEmpty()) {
            return PasswordResetResult.error("Verification code is required.");
        }

        if (otpCode.length() != 6) {
            return PasswordResetResult.error("Verification code must be 6 digits.");
        }

        if (otpDAO.verifyOtp(email, otpCode.trim())) {
            return PasswordResetResult.success("OTP verified successfully.");
        } else {
            return PasswordResetResult.error("Invalid or expired verification code. Please request a new code.");
        }
    }

    /**
     * Reset password after OTP verification
     */
    public PasswordResetResult resetPassword(String email, String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            return PasswordResetResult.error("Email is required.");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return PasswordResetResult.error("New password is required.");
        }

        if (newPassword.length() < 6) {
            return PasswordResetResult.error("Password must be at least 6 characters long.");
        }

        // Check if email exists and update password
        if (userDAO.emailExists(email)) {
            if (userDAO.changePasswordByEmail(email, newPassword)) {
                // Clean up used OTPs for this email
                cleanupOtpsForEmail(email);
                return PasswordResetResult.success("Password reset successfully! You can now login with your new password.");
            }
        }

        return PasswordResetResult.error("Failed to reset password. Please try again or contact support.");
    }

    /**
     * Validate password strength
     */
    public PasswordResetResult validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordResetResult.error("Password is required");
        }

        if (password.length() < 6) {
            return PasswordResetResult.error("Password must be at least 6 characters long");
        }

        if (password.length() > 128) {
            return PasswordResetResult.error("Password must be less than 128 characters");
        }

        // Check for basic password requirements
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");

        if (!hasLetter) {
            return PasswordResetResult.error("Password must contain at least one letter");
        }

        if (!hasDigit) {
            return PasswordResetResult.error("Password must contain at least one number");
        }

        return PasswordResetResult.success("Password is valid");
    }

    /**
     * Generate 6-digit OTP using SecureRandom
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Generates number between 100000-999999
        return String.valueOf(otp);
    }

    /**
     * Get user by email (helper method)
     */
    private User getUserByEmail(String email) {
        return userDAO.getUserByEmail(email);
    }

    /**
     * Clean up expired OTPs periodically
     */
    public void cleanupExpiredOtps() {
        try {
            otpDAO.cleanupExpiredOtps();
        } catch (Exception e) {
            System.err.println("Error cleaning up expired OTPs: " + e.getMessage());
        }
    }

    /**
     * Clean up all OTPs for a specific email (after successful password reset)
     */
    private void cleanupOtpsForEmail(String email) {
        try {
            // You might want to add this method to OtpDAO if needed
            //otpDAO.cleanupOtpsForEmail(email);
        } catch (Exception e) {
            System.err.println("Error cleaning up OTPs for email: " + e.getMessage());
        }
    }

    /**
     * Check if email is valid format
     */
    private boolean isValidEmailFormat(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Rate limiting - check if too many OTP requests from same email
     */
    public PasswordResetResult checkRateLimit(String email) {
        // You can implement rate limiting logic here
        // For example, check if more than 3 OTP requests in last 15 minutes

        // For now, just return success
        return PasswordResetResult.success("Rate limit check passed");
    }

    /**
     * Send password reset success notification email
     */
    public boolean sendPasswordResetSuccessEmail(String email) {
        try {
            User user = getUserByEmail(email);
            if (user != null) {
                return emailService.sendPasswordResetSuccessEmail(email, user.getName());
            }
        } catch (Exception e) {
            System.err.println("Error sending password reset success email: " + e.getMessage());
        }
        return false;
    }


    /**
     * Result wrapper for password reset operations
     */
    public static class PasswordResetResult {
        private final boolean success;
        private final String message;
        private final Object data;

        private PasswordResetResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static PasswordResetResult success(String message) {
            return new PasswordResetResult(true, message, null);
        }

        public static PasswordResetResult success(String message, Object data) {
            return new PasswordResetResult(true, message, data);
        }

        public static PasswordResetResult error(String message) {
            return new PasswordResetResult(false, message, null);
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }

        @Override
        public String toString() {
            return "PasswordResetResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }
}
