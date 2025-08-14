package trainapp.dao;

import trainapp.model.Admin;
import trainapp.util.DBConnection;
import trainapp.util.PasswordUtil;

import java.sql.*;

public class AdminDAO {

    private final Connection connection;

    public AdminDAO() {
        this.connection = DBConnection.getConnection();
    }

    /**
     * Create a new admin
     */
    public boolean createAdmin(Admin admin, String password) {
        String sql = "INSERT INTO admins (username, password_hash) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, admin.getUsername());
            stmt.setString(2, PasswordUtil.hashPassword(password));

            int result = stmt.executeUpdate();

            if (result > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        admin.setAdminId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating admin: " + e.getMessage());
        }

        return false;
    }

    /**
     * Authenticate admin by username
     */
    public Admin authenticate(String username, String password) {
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
            System.err.println("Error authenticating admin: " + e.getMessage());
        }

        return null;
    }

    /**
     * Check if admin username exists
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM admins WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking admin username existence: " + e.getMessage());
        }

        return false;
    }
}
