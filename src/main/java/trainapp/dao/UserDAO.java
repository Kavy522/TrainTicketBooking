package trainapp.dao;

import trainapp.model.User;
import trainapp.util.DBConnection;
import trainapp.util.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private final Connection connection;

    public UserDAO() {
        this.connection = DBConnection.getConnection();
    }

    // Create a new user
    public boolean createUser(User user, String password) {
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
        }

        return false;
    }

    // Authenticate user by name
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
        }

        return null;
    }

    // Get all users (for admin panel and lists)
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
        }
        return users;
    }

    // Get user by email
    public User getUserByEmail(String email) {
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
        }

        return null;
    }

    // Get user by ID
    public User getUserById(int userId) {
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
        }

        return null;
    }

    // Check if name exists
    public boolean nameExists(String name) {
        String sql = "SELECT COUNT(*) FROM users WHERE name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking name existence: " + e.getMessage());
        }

        return false;
    }

    // Check if email exists
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
        }

        return false;
    }

    // Check if phone exists
    public boolean phoneExists(String phone) {
        String sql = "SELECT COUNT(*) FROM users WHERE phone = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, phone);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking phone existence: " + e.getMessage());
        }

        return false;
    }

    // Update user profile
    public boolean updateUser(User user) {
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
        }
        return false;
    }


    // Change user password by email
    public boolean changePasswordByEmail(String email, String newPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, PasswordUtil.hashPassword(newPassword));
            stmt.setString(2, email);

            int result = stmt.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error changing password by email: " + e.getMessage());
        }

        return false;
    }

    // Update last login time
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = ? WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating last login: " + e.getMessage());
        }
    }

    // Map ResultSet to User object
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

    // Get total number of users
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting user count: " + e.getMessage());
        }
        return 0;
    }

    // Delete user
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }

    public boolean addUser(User user) {
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
        }
        return false;
    }


}