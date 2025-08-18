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
 * AdminDataStructureService demonstrates practical usage of various Java data structures
 * in a train booking administration system context.
 *
 * <p>This service showcases different data structures and their optimal use cases:
 * <ul>
 *   <li><b>LinkedList:</b> Recent activities with efficient FIFO operations</li>
 *   <li><b>HashMap:</b> Route caching for O(1) average access time</li>
 *   <li><b>Hashtable:</b> Thread-safe user session management</li>
 *   <li><b>TreeMap:</b> Auto-sorted fare structures with range queries</li>
 *   <li><b>ArrayList:</b> Indexed train list with O(1) random access</li>
 *   <li><b>HashSet:</b> Unique user tracking without duplicates</li>
 * </ul>
 *
 * <p>Also provides dynamic fare calculation, statistics generation, and performance comparisons.
 */
public class AdminDataStructureService {

    // -------------------------------------------------------------------------
    // Data Structure Declarations
    // -------------------------------------------------------------------------

    /**
     * LinkedList for maintaining chronological order of recent system activities
     */
    private final LinkedList<String> recentActivities;

    /**
     * HashMap for fast route information caching with O(1) average access
     */
    private final HashMap<String, String> routeCache;

    /**
     * Hashtable for thread-safe user session management
     */
    private final Hashtable<String, String> userSessions;

    /**
     * Map of TreeMaps for per-class fare structures, automatically sorted by distance
     */
    private final Map<TrainClass, TreeMap<Integer, Double>> fareStructureByClass;

    /**
     * ArrayList for managing train data with indexed access
     */
    private final ArrayList<String> trainList;

    /**
     * HashSet for tracking unique users without duplicates
     */
    private final HashSet<String> uniqueUsers;

    // -------------------------------------------------------------------------
    // Constructor & Initialization
    // -------------------------------------------------------------------------

    /**
     * Constructs the service and initializes all data structures with sample data.
     * Demonstrates proper initialization patterns for different collection types.
     */
    public AdminDataStructureService() {
        recentActivities = new LinkedList<>();
        routeCache = new HashMap<>();
        userSessions = new Hashtable<>();
        fareStructureByClass = new HashMap<>();
        trainList = new ArrayList<>();
        uniqueUsers = new HashSet<>();

        initializeSampleData();
    }

    /**
     * Initializes all data structures with representative sample data.
     * Demonstrates proper population patterns for each collection type.
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
     * Calculates sample fare for a given class and distance.
     * Used for initialization and fallback calculations.
     *
     * @param trainClass the train class for fare calculation
     * @param distance   distance in kilometers
     * @return calculated sample fare
     */
    private double getSampleFareForClass(TrainClass trainClass, int distance) {
        double baseFare = distance * 0.5;
        switch (trainClass) {
            case SL:
                return baseFare * 1.0;
            case _3A:
                return baseFare * 1.5;
            case _2A:
                return baseFare * 2.0;
            case _1A:
                return baseFare * 3.0;
            default:
                return baseFare;
        }
    }


    // -------------------------------------------------------------------------
    // TreeMap Operations - Dynamic Fare Management
    // -------------------------------------------------------------------------

    /**
     * Sets fare for specific distance and train class.
     * Demonstrates TreeMap.put() with automatic sorting by distance.
     *
     * @param trainClass class of train service
     * @param distance   distance in kilometers
     * @param fare       fare amount for the distance/class combination
     */
    public void setFare(TrainClass trainClass, int distance, double fare) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.computeIfAbsent(trainClass, k -> new TreeMap<>());
        classFares.put(distance, fare);
    }


    /**
     * Gets complete fare structure for a specific train class.
     *
     * @param trainClass class to get fare structure for
     * @return TreeMap of distance to fare mappings
     */
    public TreeMap<Integer, Double> getFareStructureForClass(TrainClass trainClass) {
        return new TreeMap<>(fareStructureByClass.getOrDefault(trainClass, new TreeMap<>()));
    }

    /**
     * Returns all fare structures for all train classes.
     *
     * @return Map containing fare structures for each train class
     */
    public Map<TrainClass, TreeMap<Integer, Double>> getAllFareStructures() {
        return new HashMap<>(fareStructureByClass);
    }

    /**
     * Calculates dynamic fare based on distance and class using intelligent interpolation.
     * Uses TreeMap navigation methods to find exact match or perform linear interpolation
     * between nearest fare points.
     *
     * @param trainClass class of train service
     * @param distance   distance in kilometers
     * @return calculated fare using interpolation or fallback calculation
     */
    public double calculateDynamicFare(TrainClass trainClass, int distance) {
        TreeMap<Integer, Double> fares = getFareStructureForClass(trainClass);
        if (fares.isEmpty()) {
            return getSampleFareForClass(trainClass, distance);
        }

        // Exact match
        if (fares.containsKey(distance)) {
            return fares.get(distance);
        }

        // Get nearest lower and higher
        Entry<Integer, Double> lower = fares.floorEntry(distance);
        Entry<Integer, Double> higher = fares.ceilingEntry(distance);

        if (lower == null && higher == null) {
            return 0.0;
        } else if (lower == null) {
            // Below minimum distance, use proportional scaling
            return higher.getValue() * ((double) distance / higher.getKey());
        } else if (higher == null) {
            // Above maximum distance, use proportional extrapolation
            return lower.getValue() * ((double) distance / lower.getKey());
        } else {
            // Linear interpolation between lower and higher
            double distDiff = higher.getKey() - lower.getKey();
            double fareDiff = higher.getValue() - lower.getValue();
            double ratio = (distance - lower.getKey()) / distDiff;
            return lower.getValue() + (ratio * fareDiff);
        }
    }


    /**
     * Fetches real travel statistics from database.
     * Provides fallback values on database errors.
     *
     * @return TravelStatistics object with current system metrics
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
            // Fallback to zero values
            stats.setTotalUsers(0);
            stats.setActiveTrains(0);
            stats.setTotalBookings(0);
            stats.setTotalRevenue(0.0);
        }
        return stats;
    }
}
