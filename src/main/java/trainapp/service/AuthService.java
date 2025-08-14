package trainapp.service;

import trainapp.dao.UserDAO;
import trainapp.dao.AdminDAO;
import trainapp.model.User;
import trainapp.model.Admin;
import trainapp.util.PasswordUtil;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();
    private final AdminDAO adminDAO = new AdminDAO();
    private final SessionManager sessionManager = SessionManager.getInstance();

    /**
     * Login with name/username and password
     * First tries admin login, then user login by name
     */
    public AuthResult login(String nameOrUsername, String password) {
        if (nameOrUsername == null || nameOrUsername.trim().isEmpty()) {
            return AuthResult.error("Name/Username is required");
        }

        if (password == null || password.trim().isEmpty()) {
            return AuthResult.error("Password is required");
        }

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
     * Register a new user
     */
    public AuthResult registerUser(String name, String email, String phone, String password, String confirmPassword) {
        // Check if name already exists
        if (userDAO.nameExists(name)) {
            return AuthResult.error("Name already exists. Please choose a different name.");
        }

        // Check if email already exists
        if (userDAO.emailExists(email)) {
            return AuthResult.error("Email already registered. Please use a different email.");
        }

        // Check if phone already exists
        if (userDAO.phoneExists(phone)) {
            return AuthResult.error("Phone number already registered. Please use a different phone number.");
        }

        // Create new user
        User newUser = new User(name.trim(), email.trim(), phone.trim());

        if (userDAO.createUser(newUser, password)) {
            sessionManager.loginUser(newUser);
            return AuthResult.success("Registration successful! Welcome to TrainApp!", newUser);
        } else {
            return AuthResult.error("Registration failed. Please try again.");
        }
    }

    /**
     * Register a new admin
     */
    public AuthResult registerAdmin(String username, String password, String confirmPassword) {
        // Validation
        if (username == null || username.trim().isEmpty()) {
            return AuthResult.error("Username is required");
        }

        if (username.length() < 3 || username.length() > 50) {
            return AuthResult.error("Username must be between 3 and 50 characters");
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return AuthResult.error("Username can only contain letters, numbers, and underscores");
        }

        if (!PasswordUtil.isValidPassword(password)) {
            return AuthResult.error(PasswordUtil.getPasswordStrengthMessage(password));
        }

        if (!password.equals(confirmPassword)) {
            return AuthResult.error("Password and confirmation password don't match");
        }

        // Check if username already exists
        if (adminDAO.usernameExists(username)) {
            return AuthResult.error("Username already exists. Please choose a different username.");
        }

        // Create new admin
        Admin newAdmin = new Admin(username.trim());

        if (adminDAO.createAdmin(newAdmin, password)) {
            return AuthResult.success("Admin registration successful! You can now login.", newAdmin);
        } else {
            return AuthResult.error("Admin registration failed. Please try again.");
        }
    }

    /**
     * Logout current user/admin
     */
    public void logout() {
        sessionManager.logout();
    }

    /**
     * Check if user is currently logged in
     */
    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    /**
     * Check if current session is admin
     */
    public boolean isAdmin() {
        return sessionManager.isAdmin();
    }

    /**
     * Check if current session is user
     */
    public boolean isUser() {
        return sessionManager.isUser();
    }

    /**
     * Get current user
     */
    public User getCurrentUser() {
        return sessionManager.getCurrentUser();
    }

    /**
     * Get current admin
     */
    public Admin getCurrentAdmin() {
        return sessionManager.getCurrentAdmin();
    }



    /**
     * Authentication result wrapper
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

        public static AuthResult success(String message) {
            return new AuthResult(true, message, null);
        }

        public static AuthResult success(String message, Object data) {
            return new AuthResult(true, message, data);
        }

        public static AuthResult error(String message) {
            return new AuthResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public User getUser() { return data instanceof User ? (User) data : null; }
        public Admin getAdmin() { return data instanceof Admin ? (Admin) data : null; }
        public Object getData() { return data; }
    }
}