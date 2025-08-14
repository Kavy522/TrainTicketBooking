package trainapp.util;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PasswordUtil {

    /**
     * BCrypt strength - log rounds (cost factor)
     * 12 is recommended for production (provides good security with reasonable performance)
     */
    private static final int LOG_ROUNDS = 12;

    /**
     * Hash password using BCrypt
     * @param password plain text password
     * @return BCrypt hashed password (will fit in CHAR(60))
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        try {
            return BCrypt.hashpw(password, BCrypt.gensalt(LOG_ROUNDS));
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    /**
     * Verify password against stored BCrypt hash
     * @param password plain text password to verify
     * @param storedHash stored BCrypt hash from database
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(password, storedHash);
        } catch (Exception e) {
            // Log error in production
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if password meets basic requirements
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        return true;
    }

    /**
     * Get password validation message
     */
    public static String getPasswordStrengthMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password is required";
        }
        if (password.length() < 6) {
            return "Password must be at least 6 characters long";
        }
        if (password.length() < 8) {
            return "Password should be at least 8 characters for better security";
        }
        return "Valid password";
    }

    /**
     * Generate BCrypt hash for admin setup (utility method)
     * Use this for creating admin passwords programmatically
     */
    public static String generateHashForPassword(String plainPassword) {
        return hashPassword(plainPassword);
    }

    /**
     * Test method to verify hash works (for debugging)
     */
    public static void testPasswordHash(String plainPassword, String hashedPassword) {
        boolean isValid = verifyPassword(plainPassword, hashedPassword);
        System.out.println("Password: " + plainPassword);
        System.out.println("Hash: " + hashedPassword);
        System.out.println("Valid: " + (isValid ? "✅" : "❌"));
        System.out.println("Hash length: " + hashedPassword.length());
    }

    /**
     * Check if a hash needs to be upgraded (lower cost factor)
     * This helps maintain security as computing power increases
     */
    public static boolean needsRehash(String hash) {
        try {
            // Extract cost factor from hash
            int currentCost = getCostFromHash(hash);
            return currentCost < LOG_ROUNDS;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract cost factor from BCrypt hash
     */
    private static int getCostFromHash(String hash) {
        if (hash == null || hash.length() < 7) {
            throw new IllegalArgumentException("Invalid hash format");
        }

        // BCrypt hash format: $2a$12$...
        // Extract the cost (rounds) from positions 4-5
        try {
            return Integer.parseInt(hash.substring(4, 6));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot extract cost from hash");
        }
    }
}
