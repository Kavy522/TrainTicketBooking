package trainapp.dao;

import trainapp.model.User;
import trainapp.util.DBConnection;
import trainapp.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for user account management and authentication.
 * Provides comprehensive CRUD operations, authentication, and user validation functionality.
 *
 * <p>Key Features:
 * <ul>
 *   <li>User registration and account creation</li>
 *   <li>Secure authentication with password hashing</li>
 *   <li>User profile management and updates</li>
 *   <li>Password reset and modification</li>
 *   <li>Uniqueness validation for name, email, and phone</li>
 *   <li>Login tracking and session management</li>
 *   <li>Administrative user management operations</li>
 * </ul>
 *
 * <p>Security features:
 * <ul>
 *   <li>Password hashing using PasswordUtil for secure storage</li>
 *   <li>SQL injection prevention through prepared statements</li>
 *   <li>Input validation and sanitization</li>
 *   <li>Last login tracking for security auditing</li>
 *   <li>Secure password verification</li>
 * </ul>
 */
public class UserDAO {

    // -------------------------------------------------------------------------
    // Database Connection
    // -------------------------------------------------------------------------

    /** Database connection instance for all user operations */
    private final Connection connection;

    /**
     * Constructor that initializes the database connection.
     * Uses DBConnection utility to establish connection to the database.
     */
    public UserDAO() {
        this.connection = DBConnection.getConnection();
    }

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new user account with secure password hashing.
     * Automatically generates user ID and sets creation timestamp.
     *
     * @param user User object containing name, email, and phone information
     * @param password Plain text password to be hashed and stored
     * @return true if user was created successfully and ID assigned, false otherwise
     * @throws IllegalArgumentException if user is null or has invalid data
     */
    public boolean createUser(User user, String password) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }

        String sql = """
                INSERT INTO users (name, email, phone, password_hash, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, PasswordUtil.hashPassword(password));
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            int result = stmt.executeUpdate();
            if (result > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Adds a user with pre-hashed password and existing metadata.
     * Alternative creation method for administrative operations or data migration.
     *
     * @param user User object with all information including hashed password
     * @return true if user was added successfully, false otherwise
     * @throws IllegalArgumentException if user is null or has invalid data
     */
    public boolean addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }

        String sql = "INSERT INTO users (name, email, phone, password_hash, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhone());
            stmt.setString(4, user.getPasswordHash());
            stmt.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now()));

            int result = stmt.executeUpdate();
            if (result > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Authentication Operations
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user by name and password.
     * Verifies password against stored hash and updates last login time on success.
     *
     * @param name user's unique name
     * @param password plain text password to verify
     * @return User object if authentication successful, null otherwise
     */
    public User authenticate(String name, String password) {
        String sql = """
                SELECT user_id, name, email, phone, password_hash, created_at, last_login
                FROM users 
                WHERE name = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        User user = mapResultSetToUser(rs);
                        updateLastLogin(user.getUserId());
                        user.setLastLogin(LocalDateTime.now());
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a user by their unique database ID.
     *
     * @param userId the user's unique identifier
     * @return User object if found, null otherwise
     * @throws IllegalArgumentException if userId is not positive
     */
    public User getUserById(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }

        String sql = """
                SELECT user_id, name, email, phone, password_hash, created_at, last_login
                FROM users 
                WHERE user_id = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves a user by their email address.
     * Used for password reset and email-based operations.
     *
     * @param email the user's email address
     * @return User object if found, null otherwise
     * @throws IllegalArgumentException if email is null or empty
     */
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        String sql = """
                SELECT user_id, name, email, phone, password_hash, created_at, last_login
                FROM users 
                WHERE email = ?
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves all users in the system, ordered by creation date (newest first).
     * Used for administrative user management and reporting.
     *
     * @return List of all User objects, empty list if none found
     */
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, name, email, phone, password_hash, created_at, last_login FROM users ORDER BY created_at DESC";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // -------------------------------------------------------------------------
    // Update Operations
    // -------------------------------------------------------------------------

    /**
     * Updates a user's profile information (name, email, phone).
     * Password updates are handled separately for security reasons.
     *
     * @param user User object with updated information (must have valid user ID)
     * @return true if user was updated successfully, false otherwise
     * @throws IllegalArgumentException if user is null, has invalid ID, or missing required fields
     */
    public boolean updateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUserId() <= 0) {
            throw new IllegalArgumentException("User must have a valid ID for update");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be null or empty");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be null or empty");
        }

        String sql = """
                UPDATE users 
                SET name = ?, email = ?, phone = ?
                WHERE user_id = ?
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPhone());
            stmt.setInt(4, user.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Changes a user's password using their email address.
     * Used primarily for password reset functionality.
     *
     * @param email user's email address
     * @param newPassword new plain text password to be hashed and stored
     * @return true if password was changed successfully, false otherwise
     * @throws IllegalArgumentException if email or password is null or empty
     */
    public boolean changePasswordByEmail(String email, String newPassword) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }

        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setString(2, email);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error changing password by email: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Updates the last login timestamp for a user.
     * Called automatically during successful authentication.
     *
     * @param userId user ID to update login time for
     */
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = ? WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Delete Operations
    // -------------------------------------------------------------------------

    /**
     * Deletes a user from the database by their ID.
     *
     * <p><b>Warning:</b> This operation may fail if the user has associated
     * bookings or other records due to foreign key constraints.
     *
     * @param userId the unique ID of the user to delete
     * @return true if user was deleted successfully, false otherwise
     * @throws IllegalArgumentException if userId is not positive
     */
    public boolean deleteUser(int userId) {
        if (userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }

        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Validation Operations
    // -------------------------------------------------------------------------

    /**
     * Checks if a user name already exists in the database.
     * Used for registration validation to ensure unique names.
     *
     * @param name user name to check
     * @return true if name exists, false if available
     */
    public boolean nameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM users WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking name existence: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks if an email address already exists in the database.
     * Used for registration validation and duplicate prevention.
     *
     * @param email email address to check
     * @return true if email exists, false if available
     */
    public boolean emailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Checks if a phone number already exists in the database.
     * Used for registration validation to ensure unique contact information.
     *
     * @param phone phone number to check
     * @return true if phone exists, false if available
     */
    public boolean phoneExists(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM users WHERE phone = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, phone);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking phone existence: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Analytics & Reporting Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves the total count of users in the system.
     * Used for dashboard statistics and reporting.
     *
     * @return total number of users in the database
     */
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting user count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Maps a ResultSet row to a User object.
     * Internal utility method for consistent object creation from database results.
     *
     * @param rs ResultSet positioned at a valid row
     * @return User object populated with data from the ResultSet
     * @throws SQLException if database access error occurs
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPasswordHash(rs.getString("password_hash"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        return user;
    }
}
