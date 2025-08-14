package trainapp.service;

import trainapp.dao.UserDAO;
import trainapp.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class UserProfileService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Get user statistics
     */
    public UserStatistics getUserStatistics(int userId) {
        // TODO: Implement actual database queries for bookings
        // For now, return mock data
        UserStatistics stats = new UserStatistics();
        stats.setTotalBookings(12);
        stats.setCompletedTrips(8);
        stats.setCancelledBookings(2);
        stats.setPendingBookings(2);
        return stats;
    }

    /**
     * Get recent user activity
     */
    public List<ActivityItem> getRecentActivity(int userId, int limit) {
        // TODO: Implement actual database queries for activity
        // For now, return mock data
        List<ActivityItem> activities = new ArrayList<>();

        activities.add(new ActivityItem(
                "Booked ticket for Mumbai to Delhi",
                LocalDateTime.now().minusDays(1)
        ));

        activities.add(new ActivityItem(
                "Profile information updated",
                LocalDateTime.now().minusDays(3)
        ));

        activities.add(new ActivityItem(
                "Password changed successfully",
                LocalDateTime.now().minusDays(5)
        ));

        return activities.subList(0, Math.min(activities.size(), limit));
    }

    /**
     * Update user profile
     */
    public boolean updateUserProfile(int userId, String name, String email, String phone) {
        User user = userDAO.getUserById(userId);
        if (user == null) {
            return false;
        }

        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);

        return userDAO.updateUser(user);
    }

    /**
     * User Statistics inner class
     */
    public static class UserStatistics {
        private int totalBookings;
        private int completedTrips;
        private int cancelledBookings;
        private int pendingBookings;

        // Getters and Setters
        public int getTotalBookings() { return totalBookings; }
        public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

        public int getCompletedTrips() { return completedTrips; }
        public void setCompletedTrips(int completedTrips) { this.completedTrips = completedTrips; }

        public int getCancelledBookings() { return cancelledBookings; }
        public void setCancelledBookings(int cancelledBookings) { this.cancelledBookings = cancelledBookings; }

        public int getPendingBookings() { return pendingBookings; }
        public void setPendingBookings(int pendingBookings) { this.pendingBookings = pendingBookings; }
    }

    /**
     * Activity Item inner class
     */
    public static class ActivityItem {
        private String description;
        private LocalDateTime timestamp;

        public ActivityItem(String description, LocalDateTime timestamp) {
            this.description = description;
            this.timestamp = timestamp;
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

        public String getFormattedTime() {
            LocalDateTime now = LocalDateTime.now();
            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(timestamp.toLocalDate(), now.toLocalDate());

            if (daysDiff == 0) {
                return "Today at " + timestamp.format(DateTimeFormatter.ofPattern("h:mm a"));
            } else if (daysDiff == 1) {
                return "Yesterday at " + timestamp.format(DateTimeFormatter.ofPattern("h:mm a"));
            } else {
                return timestamp.format(DateTimeFormatter.ofPattern("MMM dd 'at' h:mm a"));
            }
        }
    }
}
