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
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
}
