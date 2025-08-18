package trainapp.service;

import trainapp.model.User;
import trainapp.model.Admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SessionManager handles login/logout, session status, pending booking data,
 * permissions, and session listeners for user and admin sessions.
 * <p>
 * Methods are grouped by functionality for clarity and maintainability.
 */
public class SessionManager {

    private static SessionManager instance;

    // Session state
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

    /**
     * Distinguishes user, admin, and guest session types.
     */
    public enum UserType {USER, ADMIN, GUEST}

    // -------------------------------------------------------------------------
    // Singleton Constructor
    // -------------------------------------------------------------------------
    private SessionManager() {
        this.isLoggedIn = false;
        this.userType = UserType.GUEST;
        this.listeners = new ArrayList<>();
    }

    /**
     * Returns the singleton instance for session management.
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ===================== LOGIN METHODS =====================

    /**
     * Log in as a regular user.
     *
     * @param user the user object to log in with
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
     * Login (delegates to loginUser for regular users).
     *
     * @param user the user to log in
     */
    public void login(User user) {
        loginUser(user);
    }

    /**
     * Log in as an administrator.
     *
     * @param admin admin object to log in with
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
     * Log out the current user or admin.
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

        clearPendingBooking();

        System.out.println("Logged out: " + loggedOutUser);
        notifyListeners();
    }

    // ===================== STATUS GETTERS =====================

    /**
     * @return true if any user or admin is logged in
     */
    public boolean isLoggedIn() {
        return isLoggedIn && (currentUser != null || currentAdmin != null);
    }

    /**
     * @return the current logged-in user (null if admin)
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * @return the current logged-in admin (null if user)
     */
    public Admin getCurrentAdmin() {
        return currentAdmin;
    }

    /**
     * @return the current session type (user/admin/guest)
     */
    public UserType getUserType() {
        return userType;
    }

    /**
     * @return true if the session is a user session
     */
    public boolean isUser() {
        return userType == UserType.USER && currentUser != null;
    }

    /**
     * @return true if the session is an admin session
     */
    public boolean isAdmin() {
        return userType == UserType.ADMIN && currentAdmin != null;
    }

    /**
     * @return the login timestamp (null if not logged in)
     */
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    /**
     * @return display name for the current session holder (user, admin, or guest)
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
     * Set pending booking data (for use after successful login).
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
     * Clear any saved pending booking after handling.
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
     * @return true if there is currently a pending booking
     */
    public boolean hasPendingBooking() {
        return hasPendingBooking;
    }

    /**
     * @return pending train id, 0 if none
     */
    public int getPendingTrainId() {
        return pendingTrainId;
    }

    /**
     * @return pending source station name, null if none
     */
    public String getPendingFromStation() {
        return pendingFromStation;
    }

    /**
     * @return pending destination station name, null if none
     */
    public String getPendingToStation() {
        return pendingToStation;
    }

    /**
     * @return pending journey date, null if none
     */
    public LocalDate getPendingJourneyDate() {
        return pendingJourneyDate;
    }

    // ===================== SESSION LISTENER PATTERN =====================

    /**
     * Interface for objects that listen for session change events.
     */
    public interface SessionListener {
        void onSessionChanged(boolean isLoggedIn, UserType userType, String displayName);
    }

    /**
     * Notify all listeners of any change in session status.
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
}