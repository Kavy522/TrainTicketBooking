package trainapp.service;

import trainapp.dao.UserDAO;
import trainapp.dao.AdminDAO;
import trainapp.model.User;
import trainapp.model.Admin;
import trainapp.util.PasswordUtil;

/**
 * AuthService manages authentication and registration for users and admins.
 *
 * Responsibilities:
 * <ul>
 *   <li>Authenticate users/admins and manage session</li>
 *   <li>Register new users with validation</li>
 *   <li>Manage login/logout state via SessionManager</li>
 *   <li>Expose convenience accessors for session information</li>
 * </ul>
 */
public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final AdminDAO adminDAO = new AdminDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // -------------------------------------------------------------------------
    // Auth & Registration API Methods
    // -------------------------------------------------------------------------

    /**
     * Performs login using username/name and password.
     * Attempts admin authentication first, then falls back to user login.
     * If successful, stores state in the SessionManager.
     *
     * @param nameOrUsername Admin username or User name
     * @param password Raw password (plain text)
     * @return AuthResult indicating success/failure, and user/admin info if applicable
     */
    public AuthResult login(String nameOrUsername, String password) {
        // Try admin login first (since username field is unique to admins)
        Admin admin = adminDAO.authenticate(nameOrUsername.trim(), password);
        if (admin != null) {
            sessionManager.loginAdmin(admin);
            return AuthResult.success("Welcome back, Administrator!", admin);
        }
        // Try user login by name
        User user = userDAO.authenticate(nameOrUsername.trim(), password);
        if (user != null) {
            sessionManager.loginUser(user);
            return AuthResult.success("Welcome back, " + user.getName() + "!", user);
        }
        return AuthResult.error("Invalid name or password");
    }

    /**
     * Registers a new user (not admin) with provided information.
     * Validates uniqueness for name, email, phone, and sets up session.
     *
     * @param name Desired display name
     * @param email User's email (must be unique)
     * @param phone User's phone number (unique)
     * @param password Initial password
     * @param confirmPassword Confirmed password
     * @return AuthResult indicating outcome of registration and user info.
     */
    public AuthResult registerUser(String name, String email, String phone, String password, String confirmPassword) {
        if (userDAO.nameExists(name)) {
            return AuthResult.error("Name already exists. Please choose a different name.");
        }
        if (userDAO.emailExists(email)) {
            return AuthResult.error("Email already registered. Please use a different email.");
        }
        if (userDAO.phoneExists(phone)) {
            return AuthResult.error("Phone number already registered. Please use a different phone number.");
        }

        User newUser = new User(name.trim(), email.trim(), phone.trim());
        if (userDAO.createUser(newUser, password)) {
            sessionManager.loginUser(newUser);
            return AuthResult.success("Registration successful! Welcome to TrainApp!", newUser);
        } else {
            return AuthResult.error("Registration failed. Please try again.");
        }
    }

    /**
     * Logs out the current session (either user or admin).
     */
    public void logout() {
        sessionManager.logout();
    }

    // -------------------------------------------------------------------------
    // Session State & Accessors
    // -------------------------------------------------------------------------

    /**
     * @return true if any user or admin is logged in
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    /**
     * @return true if the current session holder is admin
     */
    public boolean isAdmin() {
        return sessionManager.isAdmin();
    }

    /**
     * @return true if the current session holder is user
     */
    public boolean isUser() {
        return sessionManager.isUser();
    }

    /**
     * Returns instance of the currently logged in User, or null if not a user session.
     */
    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    /**
     * Returns instance of the currently logged in Admin, or null if not an admin session.
     */
    public Admin getCurrentAdmin() {
        return sessionManager.getCurrentAdmin();
    }

    // -------------------------------------------------------------------------
    // Auth Operation Result Wrapper
    // -------------------------------------------------------------------------

    /**
     * Wrapper/result class for any authentication-related operation,
     * including login, registration, and session state checks.
     * Can carry user/admin info on success, error message otherwise.
     */
    public static class AuthResult {
        private final boolean success;
        private final String message;
        private final Object data;

        private AuthResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        /**
         * Static factory for successful auth action with message only.
         */
        public static AuthResult success(String message) {
            return new AuthResult(true, message, null);
        }

        /**
         * Static factory for successful auth action with result data (user/admin).
         */
        public static AuthResult success(String message, Object data) {
            return new AuthResult(true, message, data);
        }

        /**
         * Static factory for failed auth action with error message.
         */
        public static AuthResult error(String message) {
            return new AuthResult(false, message, null);
        }

        /** @return true if operation succeeded */
        public boolean isSuccess() { return success; }

        /** @return explanatory message (may be success or error) */
        public String getMessage() { return message; }

        /** @return user object if result data is an instance of User, else null */
        public User getUser() { return data instanceof User ? (User) data : null; }

        /** @return admin object if result data is an instance of Admin, else null */
        public Admin getAdmin() { return data instanceof Admin ? (Admin) data : null; }

        /** @return raw underlying data (User/Admin/null) */
        public Object getData() { return data; }
    }
}
