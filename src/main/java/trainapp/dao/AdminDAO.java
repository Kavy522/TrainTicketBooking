package trainapp.dao;

import trainapp.model.Admin;
import trainapp.util.DBConnection;
import trainapp.util.PasswordUtil;

import java.sql.*;

/**
 * Data Access Object (DAO) for Admin-related database operations.
 * Provides authentication and CRUD operations for administrative users.
 *
 * <p>This DAO handles all database interactions for admin management including:
 * <ul>
 *   <li>Admin authentication with password verification</li>
 *   <li>Admin account management and queries</li>
 *   <li>Secure password handling using PasswordUtil</li>
 * </ul>
 *
 * <p>All methods use prepared statements to prevent SQL injection attacks.
 *
 */
public class AdminDAO {

    // -------------------------------------------------------------------------
    // Database Connection
    // -------------------------------------------------------------------------

    /**
     * Database connection instance for all admin operations
     */
    private final Connection connection;

    /**
     * Constructor that initializes the database connection.
     * Uses DBConnection utility to establish connection to the database.
     */
    public AdminDAO() {
        this.connection = DBConnection.getConnection();
    }

    // -------------------------------------------------------------------------
    // Authentication Operations
    // -------------------------------------------------------------------------

    /**
     * Authenticates an admin user using username and password.
     * Verifies the provided password against the stored hash using secure password verification.
     *
     * <p>Security features:
     * <ul>
     *   <li>Uses prepared statements to prevent SQL injection</li>
     *   <li>Employs secure password hashing via PasswordUtil</li>
     *   <li>Returns complete admin object only on successful authentication</li>
     * </ul>
     *
     * @param username the admin's unique username
     * @param password the plain text password to verify
     * @return Admin object if authentication successful, null otherwise
     * @throws IllegalArgumentException if username or password is null or empty
     */
    public Admin authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        String sql = "SELECT admin_id, username, password_hash FROM admins WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        Admin admin = new Admin();
                        admin.setAdminId(rs.getInt("admin_id"));
                        admin.setUsername(rs.getString("username"));
                        admin.setPasswordHash(storedHash);
                        return admin;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}