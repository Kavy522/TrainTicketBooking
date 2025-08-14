package trainapp.service;

import trainapp.model.User;
import trainapp.model.Admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private Admin currentAdmin;
    private boolean isLoggedIn;
    private UserType userType;
    private LocalDateTime loginTime;
    private List<SessionListener> listeners;

    // Pending booking data for after login
    private int pendingTrainId;
    private String pendingFromStation;
    private String pendingToStation;
    private LocalDate pendingJourneyDate;
    private boolean hasPendingBooking = false;

    public enum UserType {
        USER, ADMIN, GUEST
    }

    private SessionManager() {
        this.isLoggedIn = false;
        this.userType = UserType.GUEST;
        this.listeners = new ArrayList<>();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ===================== LOGIN METHODS =====================

    /**
     * Login a regular user
     */
    public void loginUser(User user) {
        this.currentUser = user;
        this.currentAdmin = null;
        this.isLoggedIn = true;
        this.userType = UserType.USER;
        this.loginTime = LocalDateTime.now();

        if (user != null) {
            user.setLastLogin(loginTime);
        }

        System.out.println("User logged in: " + user.getName());
        notifyListeners();
    }

    /**
     * Login method compatible with the existing login flow
     */
    public void login(User user) {
        loginUser(user);
    }

    /**
     * Login an admin
     */
    public void loginAdmin(Admin admin) {
        this.currentAdmin = admin;
        this.currentUser = null;
        this.isLoggedIn = true;
        this.userType = UserType.ADMIN;
        this.loginTime = LocalDateTime.now();

        System.out.println("Admin logged in: " + admin.getUsername());
        notifyListeners();
    }

    /**
     * Logout current user/admin
     */
    public void logout() {
        String loggedOutUser = "";
        if (currentUser != null) loggedOutUser = currentUser.getName();
        if (currentAdmin != null) loggedOutUser = currentAdmin.getUsername();

        this.currentUser = null;
        this.currentAdmin = null;
        this.isLoggedIn = false;
        this.userType = UserType.GUEST;
        this.loginTime = null;

        // Clear pending booking on logout
        clearPendingBooking();

        System.out.println("Logged out: " + loggedOutUser);
        notifyListeners();
    }

    // ===================== STATUS GETTERS =====================

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return isLoggedIn && (currentUser != null || currentAdmin != null);
    }

    /**
     * Get current user (null if admin is logged in)
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Get current admin (null if user is logged in)
     */
    public Admin getCurrentAdmin() {
        return currentAdmin;
    }

    /**
     * Get current user type
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * Check if current session is a regular user
     */
    public boolean isUser() {
        return userType == UserType.USER && currentUser != null;
    }

    /**
     * Check if current session is an admin
     */
    public boolean isAdmin() {
        return userType == UserType.ADMIN && currentAdmin != null;
    }

    /**
     * Get login time
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /**
     * Get display name for current user/admin
     */
    public String getCurrentUserDisplayName() {
        if (currentUser != null) {
            return currentUser.getName();
        }
        if (currentAdmin != null) {
            return "Admin: " + currentAdmin.getUsername();
        }
        return "Guest";
    }

    // ===================== PENDING BOOKING METHODS =====================

    /**
     * Set pending booking data for after login
     */
    public void setPendingBooking(int trainId, String fromStation, String toStation, LocalDate journeyDate) {
        this.pendingTrainId = trainId;
        this.pendingFromStation = fromStation;
        this.pendingToStation = toStation;
        this.pendingJourneyDate = journeyDate;
        this.hasPendingBooking = true;

        System.out.println("Pending booking set: Train ID " + trainId +
                " from " + fromStation + " to " + toStation +
                " on " + journeyDate);
    }

    /**
     * Clear pending booking data
     */
    public void clearPendingBooking() {
        this.hasPendingBooking = false;
        this.pendingTrainId = 0;
        this.pendingFromStation = null;
        this.pendingToStation = null;
        this.pendingJourneyDate = null;

        System.out.println("Pending booking cleared");
    }

    /**
     * Check if there's a pending booking
     */
    public boolean hasPendingBooking() {
        return hasPendingBooking;
    }

    /**
     * Get pending train ID
     */
    public int getPendingTrainId() {
        return pendingTrainId;
    }

    /**
     * Get pending from station
     */
    public String getPendingFromStation() {
        return pendingFromStation;
    }

    /**
     * Get pending to station
     */
    public String getPendingToStation() {
        return pendingToStation;
    }

    /**
     * Get pending journey date
     */
    public LocalDate getPendingJourneyDate() {
        return pendingJourneyDate;
    }

    // ===================== SESSION VALIDATION =====================

    /**
     * Check if current session is valid (not expired)
     */
    public boolean isSessionValid() {
        if (!isLoggedIn()) return false;

        // Check if session hasn't expired (24 hours)
        if (loginTime != null) {
            LocalDateTime expiryTime = loginTime.plusHours(24);
            if (LocalDateTime.now().isAfter(expiryTime)) {
                logout(); // Auto logout
                return false;
            }
        }

        return true;
    }

    /**
     * Get session duration in minutes
     */
    public long getSessionDurationMinutes() {
        if (loginTime != null) {
            return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
        }
        return 0;
    }

    /**
     * Get session expiry time
     */
    public LocalDateTime getSessionExpiryTime() {
        if (loginTime != null) {
            return loginTime.plusHours(24);
        }
        return null;
    }

    /**
     * Extend session by resetting login time
     */
    public void extendSession() {
        if (isLoggedIn()) {
            this.loginTime = LocalDateTime.now();
            System.out.println("Session extended for: " + getCurrentUserDisplayName());
            notifyListeners();
        }
    }

    // ===================== REFRESH METHODS =====================

    /**
     * Refresh current user data (for profile updates)
     */
    public void refreshCurrentUser(User updatedUser) {
        if (isUser() && currentUser != null &&
                currentUser.getUserId() == updatedUser.getUserId()) {
            this.currentUser = updatedUser;
            notifyListeners();
            System.out.println("Current user data refreshed: " + updatedUser.getName());
        }
    }

    /**
     * Refresh current admin data (for profile updates)
     */
    public void refreshCurrentAdmin(Admin updatedAdmin) {
        if (isAdmin() && currentAdmin != null &&
                currentAdmin.getAdminId() == updatedAdmin.getAdminId()) {
            this.currentAdmin = updatedAdmin;
            notifyListeners();
            System.out.println("Current admin data refreshed: " + updatedAdmin.getUsername());
        }
    }

    // ===================== SECURITY METHODS =====================

    /**
     * Check if user has specific permission (for future use)
     */
    public boolean hasPermission(String permission) {
        if (isAdmin()) {
            return true; // Admins have all permissions
        }

        if (isUser()) {
            // Add user permission logic here if needed
            return true; // Basic users have basic permissions
        }

        return false; // Guests have no permissions
    }

    /**
     * Get current user's role as string
     */
    public String getCurrentUserRole() {
        if (isAdmin()) {
            return "ADMIN";
        } else if (isUser()) {
            return "USER";
        } else {
            return "GUEST";
        }
    }

    // ===================== LISTENER PATTERN =====================

    /**
     * Interface for session change listeners
     */
    public interface SessionListener {
        void onSessionChanged(boolean isLoggedIn, UserType userType, String displayName);
    }

    /**
     * Add session listener
     */
    public void addSessionListener(SessionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove session listener
     */
    public void removeSessionListener(SessionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify all listeners of session changes
     */
    private void notifyListeners() {
        for (SessionListener listener : listeners) {
            try {
                listener.onSessionChanged(isLoggedIn, userType, getCurrentUserDisplayName());
            } catch (Exception e) {
                System.err.println("Error notifying session listener: " + e.getMessage());
            }
        }
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Get session info as string (for debugging)
     */
    public String getSessionInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Session Info:\n");
        info.append("Logged In: ").append(isLoggedIn()).append("\n");
        info.append("User Type: ").append(userType).append("\n");
        info.append("Display Name: ").append(getCurrentUserDisplayName()).append("\n");
        info.append("Login Time: ").append(loginTime).append("\n");
        info.append("Session Duration: ").append(getSessionDurationMinutes()).append(" minutes\n");
        info.append("Has Pending Booking: ").append(hasPendingBooking()).append("\n");

        if (hasPendingBooking()) {
            info.append("Pending Booking: Train ").append(pendingTrainId)
                    .append(" from ").append(pendingFromStation)
                    .append(" to ").append(pendingToStation)
                    .append(" on ").append(pendingJourneyDate).append("\n");
        }

        return info.toString();
    }

    /**
     * Quick session health check
     */
    public boolean performHealthCheck() {
        try {
            // Basic validation
            if (isLoggedIn() && (currentUser == null && currentAdmin == null)) {
                System.err.println("Session health check failed: No user/admin object");
                logout();
                return false;
            }

            // Session validity check
            if (!isSessionValid()) {
                System.err.println("Session health check failed: Session expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("Session health check error: " + e.getMessage());
            return false;
        }
    }
}