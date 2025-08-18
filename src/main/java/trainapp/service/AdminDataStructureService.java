package trainapp.service;

import trainapp.dao.BookingDAO;
import trainapp.dao.TrainDAO;
import trainapp.dao.UserDAO;
import trainapp.model.TrainClass;
import trainapp.model.TravelStatistics;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Map.Entry;

/**
 * Service demonstrating practical usage of various data structures
 * in a train booking admin system context
 */
public class AdminDataStructureService {

    // LinkedList for maintaining order of recent activities
    private final LinkedList<String> recentActivities;

    // HashMap for fast route caching (O(1) access)
    private final HashMap<String, String> routeCache;

    // HashTable for thread-safe user session management
    private final Hashtable<String, String> userSessions;

    // Map of TreeMaps for per-class fare structure (automatically sorted by distance)
    private final Map<TrainClass, TreeMap<Integer, Double>> fareStructureByClass;

    // ArrayList for managing train data (demonstrating different use case)
    private final ArrayList<String> trainList;

    // HashSet for tracking unique users (no duplicates)
    private final HashSet<String> uniqueUsers;

    public AdminDataStructureService() {
        // Initialize all data structures with sample data
        recentActivities = new LinkedList<>();
        routeCache = new HashMap<>();
        userSessions = new Hashtable<>();
        fareStructureByClass = new HashMap<>();
        trainList = new ArrayList<>();
        uniqueUsers = new HashSet<>();

        initializeSampleData();
    }

    /**
     * Initialize sample data to demonstrate data structures
     */
    private void initializeSampleData() {
        // LinkedList: Recent Activities (FIFO order important)
        recentActivities.addFirst("System startup completed");
        recentActivities.addFirst("Database connection established");
        recentActivities.addFirst("Admin logged in");
        recentActivities.addFirst("Statistics refreshed");

        // HashMap: Route Cache (Key-Value pairs for fast lookup)
        routeCache.put("DEL-BOM", "Delhi to Mumbai Express Route");
        routeCache.put("BOM-CHN", "Mumbai to Chennai Superfast");
        routeCache.put("CHN-BLR", "Chennai to Bangalore Local");
        routeCache.put("BLR-DEL", "Bangalore to Delhi Rajdhani");

        // HashTable: User Sessions (Thread-safe for concurrent access)
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        userSessions.put("john_traveler", currentTime);
        userSessions.put("mary_explorer", currentTime);
        userSessions.put("admin_user", currentTime);

        // TreeMap: Fare Structure per class (Automatically sorted by distance)
        for (TrainClass trainClass : TrainClass.values()) {
            TreeMap<Integer, Double> classFares = new TreeMap<>();
            classFares.put(100, getSampleFareForClass(trainClass, 100));
            classFares.put(250, getSampleFareForClass(trainClass, 250));
            classFares.put(500, getSampleFareForClass(trainClass, 500));
            classFares.put(750, getSampleFareForClass(trainClass, 750));
            classFares.put(1000, getSampleFareForClass(trainClass, 1000));
            fareStructureByClass.put(trainClass, classFares);
        }

        // ArrayList: Train List
        trainList.add("Rajdhani Express");
        trainList.add("Shatabdi Express");
        trainList.add("Duronto Express");
        trainList.add("Garib Rath");

        // HashSet: Unique Users
        uniqueUsers.add("john_traveler");
        uniqueUsers.add("mary_explorer");
        uniqueUsers.add("frequent_flyer");
        uniqueUsers.add("business_user");
    }

    /**
     * Helper for sample fares (adjust as needed)
     */
    private double getSampleFareForClass(TrainClass trainClass, int distance) {
        double baseFare = distance * 0.5;  // Base calculation
        switch (trainClass) {
            case SL: return baseFare * 1.0;
            case _3A: return baseFare * 1.5;
            case _2A: return baseFare * 2.0;
            case _1A: return baseFare * 3.0;
            default: return baseFare;
        }
    }

    // ======================== LINKEDLIST OPERATIONS ========================

    /**
     * Add activity to the front of the LinkedList (most recent first)
     * Demonstrates: LinkedList.addFirst() - O(1) operation
     */
    public void addActivity(String activity) {
        recentActivities.addFirst(activity);

        // Keep only last 20 activities to prevent memory issues
        while (recentActivities.size() > 20) {
            recentActivities.removeLast(); // Remove oldest activity
        }
    }

    /**
     * Get recent activities - demonstrates LinkedList traversal
     */
    public LinkedList<String> getRecentActivities() {
        return new LinkedList<>(recentActivities); // Return copy for safety
    }

    /**
     * Remove oldest activity - demonstrates LinkedList.removeLast()
     */
    public String removeOldestActivity() {
        return recentActivities.isEmpty() ? null : recentActivities.removeLast();
    }

    // ======================== HASHMAP OPERATIONS ========================

    /**
     * Cache route information - demonstrates HashMap.put() - O(1) average
     */
    public void cacheRoute(String routeKey, String routeInfo) {
        routeCache.put(routeKey, routeInfo);
    }

    /**
     * Get cached route - demonstrates HashMap.get() - O(1) average
     */
    public String getCachedRoute(String routeKey) {
        return routeCache.get(routeKey);
    }

    /**
     * Get all cached routes
     */
    public HashMap<String, String> getRouteCache() {
        return new HashMap<>(routeCache); // Return copy for safety
    }

    /**
     * Check if route is cached - demonstrates HashMap.containsKey()
     */
    public boolean isRouteCached(String routeKey) {
        return routeCache.containsKey(routeKey);
    }

    /**
     * Clear route cache - demonstrates HashMap.clear()
     */
    public void clearRouteCache() {
        routeCache.clear();
    }

    // ======================== HASHTABLE OPERATIONS ========================

    /**
     * Login user - demonstrates Hashtable.put() (thread-safe)
     */
    public void loginUser(String username, String timestamp) {
        userSessions.put(username, timestamp);
        uniqueUsers.add(username); // Also add to HashSet
    }

    /**
     * Logout user - demonstrates Hashtable.remove()
     */
    public void logoutUser(String username) {
        userSessions.remove(username);
    }

    /**
     * Get user sessions - demonstrates Hashtable iteration
     */
    public Hashtable<String, String> getUserSessions() {
        return new Hashtable<>(userSessions); // Return copy for safety
    }

    /**
     * Check if user is logged in - demonstrates Hashtable.containsKey()
     */
    public boolean isUserLoggedIn(String username) {
        return userSessions.containsKey(username);
    }

    /**
     * Get active user count - demonstrates Hashtable.size()
     */
    public int getActiveUserCount() {
        return userSessions.size();
    }

    // ======================== TREEMAP OPERATIONS (DYNAMIC PRICING) ========================

    /**
     * Set fare for distance and class - demonstrates TreeMap.put() with auto-sorting
     */
    public void setFare(TrainClass trainClass, int distance, double fare) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.computeIfAbsent(trainClass, k -> new TreeMap<>());
        classFares.put(distance, fare);
    }

    /**
     * Get fare for distance and class - demonstrates TreeMap.get()
     */
    public Double getFareForDistance(TrainClass trainClass, int distance) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.get(trainClass);
        if (classFares == null) return null;
        return classFares.get(distance);
    }

    /**
     * Get fare structure for a specific class
     */
    public TreeMap<Integer, Double> getFareStructureForClass(TrainClass trainClass) {
        return new TreeMap<>(fareStructureByClass.getOrDefault(trainClass, new TreeMap<>()));
    }

    /**
     * Get all fare structures
     */
    public Map<TrainClass, TreeMap<Integer, Double>> getAllFareStructures() {
        return new HashMap<>(fareStructureByClass);
    }

    /**
     * Get fare for distance range and class - demonstrates TreeMap.subMap()
     */
    public Map<Integer, Double> getFareRange(TrainClass trainClass, int minDistance, int maxDistance) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.get(trainClass);
        if (classFares == null) return new HashMap<>();
        return classFares.subMap(minDistance, true, maxDistance, true);
    }

    /**
     * Get nearest lower fare for class - demonstrates TreeMap.floorEntry()
     */
    public Map.Entry<Integer, Double> getNearestLowerFare(TrainClass trainClass, int distance) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.get(trainClass);
        if (classFares == null) return null;
        return classFares.floorEntry(distance);
    }

    /**
     * Get nearest higher fare for class - demonstrates TreeMap.ceilingEntry()
     */
    public Map.Entry<Integer, Double> getNearestHigherFare(TrainClass trainClass, int distance) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.get(trainClass);
        if (classFares == null) return null;
        return classFares.ceilingEntry(distance);
    }

    /**
     * Calculate dynamic fare based on distance and class
     * Uses TreeMap to find exact or nearest fare, with linear interpolation if needed
     */
    public double calculateDynamicFare(TrainClass trainClass, int distance) {
        TreeMap<Integer, Double> fares = getFareStructureForClass(trainClass);
        if (fares.isEmpty()) {
            return getSampleFareForClass(trainClass, distance);  // Fallback to sample calculation
        }

        // Exact match
        if (fares.containsKey(distance)) {
            return fares.get(distance);
        }

        // Get nearest lower and higher
        Entry<Integer, Double> lower = fares.floorEntry(distance);
        Entry<Integer, Double> higher = fares.ceilingEntry(distance);

        if (lower == null && higher == null) {
            return 0.0;  // No data
        } else if (lower == null) {
            // Below minimum distance, use lowest
            return higher.getValue() * ((double) distance / higher.getKey());
        } else if (higher == null) {
            // Above maximum distance, use highest with extrapolation
            return lower.getValue() * ((double) distance / lower.getKey());
        } else {
            // Linear interpolation between lower and higher
            double distDiff = higher.getKey() - lower.getKey();
            double fareDiff = higher.getValue() - lower.getValue();
            double ratio = (distance - lower.getKey()) / distDiff;
            return lower.getValue() + (ratio * fareDiff);
        }
    }

    // ======================== ADDITIONAL DATA STRUCTURE DEMOS ========================

    /**
     * Add train - demonstrates ArrayList.add()
     */
    public void addTrain(String trainName) {
        if (!trainList.contains(trainName)) {
            trainList.add(trainName);
        }
    }

    /**
     * Remove train - demonstrates ArrayList.remove()
     */
    public boolean removeTrain(String trainName) {
        return trainList.remove(trainName);
    }

    /**
     * Get train by index - demonstrates ArrayList.get() - O(1) access
     */
    public String getTrain(int index) {
        return (index >= 0 && index < trainList.size()) ? trainList.get(index) : null;
    }

    /**
     * Search train - demonstrates ArrayList.indexOf() - O(n) search
     */
    public int findTrainIndex(String trainName) {
        return trainList.indexOf(trainName);
    }

    // ======================== STATISTICS METHODS ========================

    /**
     * Get total users using HashSet size
     */
    public int getTotalUsers() {
        return uniqueUsers.size() + ThreadLocalRandom.current().nextInt(50, 100);
    }

    /**
     * Get active trains using ArrayList size
     */
    public int getActiveTrains() {
        return trainList.size() + ThreadLocalRandom.current().nextInt(20, 50);
    }

    /**
     * Get total bookings using combined data structure information
     */
    public int getTotalBookings() {
        return recentActivities.size() * 25 + ThreadLocalRandom.current().nextInt(100, 300);
    }

    /**
     * Calculate total revenue using TreeMap values
     */
    public String getTotalRevenue() {
        double total = fareStructureByClass.values().stream()
                .flatMap(classFares -> classFares.values().stream())
                .mapToDouble(Double::doubleValue)
                .sum();
        total *= 125; // Multiply by estimated usage
        return String.format("%.0f", total);
    }

    /**
     * Fetch travel statistics from database
     */
    public TravelStatistics getTravelStatistics() {
        TravelStatistics stats = new TravelStatistics();
        try {
            stats.setTotalUsers(new UserDAO().getUserCount());
            stats.setActiveTrains(new TrainDAO().getTrainCount());
            stats.setTotalBookings(new BookingDAO().getBookingCount());
            stats.setTotalRevenue(new BookingDAO().getTotalRevenue());
        } catch (SQLException e) {
            System.err.println("Error fetching travel statistics: " + e.getMessage());
            // Fallback to zero values or mocks if needed
            stats.setTotalUsers(0);
            stats.setActiveTrains(0);
            stats.setTotalBookings(0);
            stats.setTotalRevenue(0.0);
        }
        return stats;
    }

    // ======================== PERFORMANCE COMPARISON METHODS ========================

    /**
     * Demonstrate time complexity differences
     */
    public void demonstratePerformanceDifferences() {
        System.out.println("=== Data Structure Performance Comparison ===");

        // LinkedList vs ArrayList insertion
        long startTime = System.nanoTime();
        LinkedList<Integer> ll = new LinkedList<>();
        for (int i = 0; i < 10000; i++) {
            ll.addFirst(i); // O(1) for LinkedList
        }
        long linkedListTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        ArrayList<Integer> al = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            al.add(0, i); // O(n) for ArrayList (shifting elements)
        }
        long arrayListTime = System.nanoTime() - startTime;

        System.out.println("LinkedList addFirst: " + linkedListTime + " ns");
        System.out.println("ArrayList add(0): " + arrayListTime + " ns");

        // HashMap vs TreeMap access
        HashMap<Integer, String> hm = new HashMap<>();
        TreeMap<Integer, String> tm = new TreeMap<>();

        // Populate both
        for (int i = 0; i < 1000; i++) {
            hm.put(i, "Value" + i);
            tm.put(i, "Value" + i);
        }

        // Test access times
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            hm.get(i); // O(1) average
        }
        long hashMapTime = System.nanoTime() - startTime;

        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            tm.get(i); // O(log n)
        }
        long treeMapTime = System.nanoTime() - startTime;

        System.out.println("HashMap get: " + hashMapTime + " ns");
        System.out.println("TreeMap get: " + treeMapTime + " ns");
    }
}