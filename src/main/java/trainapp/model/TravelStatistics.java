package trainapp.model;

public class TravelStatistics {
    private int totalUsers;
    private int activeTrains;
    private int totalBookings;
    private double totalRevenue;

    // Getters and Setters
    public int getTotalUsers() { return totalUsers; }
    public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }

    public int getActiveTrains() { return activeTrains; }
    public void setActiveTrains(int activeTrains) { this.activeTrains = activeTrains; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) { this.totalRevenue = totalRevenue; }
}
