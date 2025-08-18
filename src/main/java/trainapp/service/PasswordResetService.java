package trainapp.service;

import trainapp.dao.OtpDAO;
import trainapp.dao.UserDAO;
import trainapp.dao.AdminDAO;
import trainapp.model.OtpRecord;
import trainapp.model.User;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * PasswordResetService handles OTP generation, verification,
 * password reset flows, and related helper utilities for users.
 *
 * Sections: OTP management, password validation, notifications,
 * rate limiting, helpers, and result wrapper.
 */
public class PasswordResetService {

    private final OtpDAO otpDAO = new OtpDAO();
    private final EmailService emailService = new EmailService();
    private final UserDAO userDAO = new UserDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    // -------------------------------------------------------------------------
    // OTP Generation & Verification
    // -------------------------------------------------------------------------

    /**
     * Send OTP code for password reset to user's email.
     * @param email Target user's email
     * @return PasswordResetResult indicating outcome
     */
    public PasswordResetResult sendOtp(String email) {
        boolean emailExists = userDAO.emailExists(email);

        if (!emailExists) {
            return PasswordResetResult.error("Email not found in our records.");
        }

        cleanupExpiredOtps();

        String otpCode = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);
        OtpRecord otpRecord = new OtpRecord(email, otpCode, expiryTime);

        if (otpDAO.saveOtp(otpRecord)) {
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
     * Verifies the OTP code entered by the user.
     * @param email Email for which OTP was sent
     * @param otpCode Entered OTP code
     * @return PasswordResetResult response
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

    // -------------------------------------------------------------------------
    // Password Reset Workflow
    // -------------------------------------------------------------------------

    /**
     * Resets the password after a successful OTP verification.
     * @param email Target user's email
     * @param newPassword New password string
     * @return PasswordResetResult response
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
        if (userDAO.emailExists(email)) {
            if (userDAO.changePasswordByEmail(email, newPassword)) {
                cleanupOtpsForEmail(email);
                return PasswordResetResult.success("Password reset successfully! You can now login with your new password.");
            }
        }
        return PasswordResetResult.error("Failed to reset password. Please try again or contact support.");
    }

    // -------------------------------------------------------------------------
    // Password Validation Helper
    // -------------------------------------------------------------------------

    /**
     * Checks password strength against basic requirements.
     * @param password Password to validate
     * @return PasswordResetResult indicating validation status
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

    // -------------------------------------------------------------------------
    // OTP & Email Helper Methods
    // -------------------------------------------------------------------------

    /**
     * Generates a secure random 6-digit OTP code.
     * @return 6-digit OTP as string
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Retrieves user object by email.
     * @param email user's email
     * @return User object or null
     */
    private User getUserByEmail(String email) {
        return userDAO.getUserByEmail(email);
    }

    /**
     * Removes expired OTP records from the database.
     */
    public void cleanupExpiredOtps() {
        try {
            otpDAO.cleanupExpiredOtps();
        } catch (Exception e) {
            System.err.println("Error cleaning up expired OTPs: " + e.getMessage());
        }
    }

    /**
     * Removes all OTP records for a user's email (e.g. after password reset).
     * @param email User's email address.
     */
    private void cleanupOtpsForEmail(String email) {
        try {
            // You might want to add this method to OtpDAO if needed
            // otpDAO.cleanupOtpsForEmail(email);
        } catch (Exception e) {
            System.err.println("Error cleaning up OTPs for email: " + e.getMessage());
        }
    }

    /**
     * Validates email format for basic syntax (regex).
     * @param email Email address
     * @return true if format is valid
     */
    private boolean isValidEmailFormat(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // -------------------------------------------------------------------------
    // Rate Limiting & Notifications
    // -------------------------------------------------------------------------

    /**
     * Implements rate-limiting logic for OTP requests from same email.
     * (Currently mock implementation, always returns success.)
     * @param email Target email to check
     * @return PasswordResetResult for rate limiting outcome
     */
    public PasswordResetResult checkRateLimit(String email) {
        // Placeholder for logic: e.g., max 3 OTP requests per 15min
        return PasswordResetResult.success("Rate limit check passed");
    }

    /**
     * Sends a password reset success notification to user's email.
     * @param email User's email address
     * @return true if email sent successfully
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

    // -------------------------------------------------------------------------
    // Result Wrapper for Password Reset Operations
    // -------------------------------------------------------------------------

    /**
     * Wraps status/result/messages for password reset and related flows.
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

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }

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