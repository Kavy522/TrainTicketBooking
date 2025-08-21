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

/**
 * AdminDataStructureService manages comprehensive administrative data structures and dynamic fare calculations.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>Dynamic Fare Calculation</b> - Advanced pricing algorithms with time-based, distance-based, and class-based multipliers</li>
 *   <li><b>Data Structure Management</b> - Efficient management of various data structures for admin operations</li>
 *   <li><b>Performance Optimization</b> - Cached calculations and optimized algorithms for high-performance operations</li>
 *   <li><b>Statistics and Reporting</b> - Real-time travel statistics and administrative reporting</li>
 *   <li><b>Activity Tracking</b> - System activity logging and recent activity management</li>
 *   <li><b>Session Management</b> - User session tracking and management</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Multi-tier fare calculation with distance efficiency and time-based pricing</li>
 *   <li>High-performance caching mechanisms for O(1) fare lookups</li>
 *   <li>Comprehensive data structure demonstrations (LinkedList, HashMap, TreeMap, HashSet, etc.)</li>
 *   <li>Real-time statistics aggregation from multiple data sources</li>
 *   <li>Optimized batch operations for initialization and updates</li>
 *   <li>Thread-safe operations with Hashtable for session management</li>
 * </ul>
 *
 * <h2>Performance Optimizations:</h2>
 * <ul>
 *   <li>Pre-computed fare caches for instant access</li>
 *   <li>TreeMap-based distance discounts for efficient range queries</li>
 *   <li>Batch initialization operations</li>
 *   <li>Minimal conditional logic in fare calculations</li>
 *   <li>EnumMap usage for type-safe and efficient class-based operations</li>
 * </ul>
 *
 * <h2>Fare Calculation Algorithm:</h2>
 * <ol>
 *   <li>Base fare calculation using distance and class multipliers</li>
 *   <li>Distance-based discount application using TreeMap floor entry</li>
 *   <li>Time-based multiplier application (peak/off-peak/weekend)</li>
 *   <li>Class premium adjustments</li>
 *   <li>Minimum fare enforcement</li>
 *   <li>Rounding to nearest ₹5 for user-friendly pricing</li>
 * </ol>
 */
public class AdminDataStructureService {

    // =========================================================================
    // DATA STRUCTURE DECLARATIONS
    // =========================================================================

    // Activity and Cache Management
    /** LinkedList for efficient recent activity management with LIFO operations */
    private final LinkedList<String> recentActivities;

    /** HashMap for route information caching with O(1) lookup */
    private final HashMap<String, String> routeCache;

    /** Thread-safe Hashtable for active user session management */
    private final Hashtable<String, String> userSessions;

    // Fare Structure Management
    /** EnumMap with TreeMap for efficient fare structure by class and distance */
    private final Map<TrainClass, TreeMap<Integer, Double>> fareStructureByClass;

    /** ArrayList for train list management with indexed access */
    private final ArrayList<String> trainList;

    /** HashSet for unique user tracking with O(1) contains operations */
    private final HashSet<String> uniqueUsers;

    // Pricing Configuration
    /** HashMap for time-based fare multipliers */
    private final HashMap<String, Double> timeBasedMultipliers;

    /** TreeMap for distance-based discount tiers with efficient range queries */
    private final TreeMap<Integer, Double> distanceDiscountTiers;

    // Performance Optimization Caches
    /** EnumMap cache for base fare per km by class */
    private final Map<TrainClass, Double> baseFareCache;

    /** EnumMap cache for reservation charges by class */
    private final Map<TrainClass, Double> reservationChargeCache;

    /** EnumMap cache for minimum fare requirements by class */
    private final Map<TrainClass, Double> minimumFareCache;

    // Utility Components
    /** Optimized time formatter for consistent time representation */
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // =========================================================================
    // INITIALIZATION AND SETUP
    // =========================================================================

    /**
     * Initializes the AdminDataStructureService with optimized data structures and caching.
     *
     * <h3>Initialization Process:</h3>
     * <ol>
     *   <li>Creates and initializes all data structure containers</li>
     *   <li>Pre-populates performance caches for O(1) access</li>
     *   <li>Loads sample data and configuration</li>
     *   <li>Sets up fare calculation parameters</li>
     * </ol>
     */
    public AdminDataStructureService() {
        // Initialize core data structures
        recentActivities = new LinkedList<>();
        routeCache = new HashMap<>();
        userSessions = new Hashtable<>();
        fareStructureByClass = new EnumMap<>(TrainClass.class);
        trainList = new ArrayList<>();
        uniqueUsers = new HashSet<>();
        timeBasedMultipliers = new HashMap<>();
        distanceDiscountTiers = new TreeMap<>();

        // Initialize performance optimization caches
        baseFareCache = new EnumMap<>(TrainClass.class);
        reservationChargeCache = new EnumMap<>(TrainClass.class);
        minimumFareCache = new EnumMap<>(TrainClass.class);

        // Perform optimized data initialization
        initializeData();
    }

    /**
     * Performs optimized initialization with batch operations for maximum performance.
     * Uses bulk operations and pre-computed values to minimize startup time.
     */
    private void initializeData() {
        // Step 1: Initialize performance caches first
        initializeFareCaches();

        // Step 2: Populate core data structures
        initializeActivities();
        initializeRouteCache();
        initializeUserSessions();
        initializeFareStructures();
        initializeTrainList();
        initializeUniqueUsers();

        // Step 3: Set up pricing configuration
        initializePricingData();
    }

    /**
     * Pre-populates fare caches for O(1) access during fare calculations.
     * Critical for performance as these values are accessed frequently.
     */
    private void initializeFareCaches() {
        // Base fare per kilometer by class
        baseFareCache.put(TrainClass.SL, 0.75);
        baseFareCache.put(TrainClass._3A, 2.25);
        baseFareCache.put(TrainClass._2A, 3.50);
        baseFareCache.put(TrainClass._1A, 5.50);

        // Reservation charges by class
        reservationChargeCache.put(TrainClass.SL, 30.0);
        reservationChargeCache.put(TrainClass._3A, 50.0);
        reservationChargeCache.put(TrainClass._2A, 75.0);
        reservationChargeCache.put(TrainClass._1A, 125.0);

        // Minimum fare requirements by class
        minimumFareCache.put(TrainClass.SL, 120.0);
        minimumFareCache.put(TrainClass._3A, 220.0);
        minimumFareCache.put(TrainClass._2A, 320.0);
        minimumFareCache.put(TrainClass._1A, 550.0);
    }

    /**
     * Initializes recent activities LinkedList with system startup events.
     */
    private void initializeActivities() {
        recentActivities.addAll(Arrays.asList(
                "Enhanced pricing system activated",
                "Time-based fare calculation enabled",
                "Database connection established",
                "Admin logged in",
                "Statistics refreshed"
        ));
    }

    /**
     * Initializes route cache with popular route information.
     */
    private void initializeRouteCache() {
        routeCache.putAll(Map.of(
                "DEL-BOM", "Delhi to Mumbai Express Route - 1384km",
                "BOM-CHN", "Mumbai to Chennai Superfast - 1279km",
                "CHN-BLR", "Chennai to Bangalore Local - 350km",
                "BLR-DEL", "Bangalore to Delhi Rajdhani - 2150km",
                "CLDY-ND", "Calicut to New Delhi Express - 350km"
        ));
    }

    /**
     * Initializes user sessions with current timestamp.
     */
    private void initializeUserSessions() {
        String currentTime = LocalDateTime.now().format(timeFormatter);
        userSessions.putAll(Map.of(
                "john_traveler", currentTime,
                "mary_explorer", currentTime,
                "admin_user", currentTime,
                "pricing_admin", currentTime
        ));
    }

    /**
     * Initializes fare structures for all train classes with sample distance points.
     */
    private void initializeFareStructures() {
        int[] distances = {50, 100, 250, 500, 750, 1000, 1500, 2000};
        for (TrainClass trainClass : TrainClass.values()) {
            TreeMap<Integer, Double> classFares = new TreeMap<>();
            for (int distance : distances) {
                classFares.put(distance, calculateSampleFareForClass(trainClass, distance));
            }
            fareStructureByClass.put(trainClass, classFares);
        }
    }

    /**
     * Initializes train list with sample train information.
     */
    private void initializeTrainList() {
        trainList.addAll(Arrays.asList(
                "Rajdhani Express - Premium",
                "Shatabdi Express - Day Train",
                "Duronto Express - Non-stop",
                "Garib Rath - Budget",
                "Vande Bharat - Modern"
        ));
    }

    /**
     * Initializes unique users set with sample user data.
     */
    private void initializeUniqueUsers() {
        uniqueUsers.addAll(Arrays.asList(
                "john_traveler", "mary_explorer", "frequent_flyer",
                "business_user", "premium_traveler"
        ));
    }

    /**
     * Initializes pricing configuration data for time-based and distance-based calculations.
     */
    private void initializePricingData() {
        // Time-based multipliers for dynamic pricing
        timeBasedMultipliers.putAll(Map.of(
                "PEAK_MORNING", 1.15,
                "PEAK_EVENING", 1.10,
                "WEEKEND", 1.20,
                "HOLIDAY", 1.25,
                "OFF_PEAK", 0.95,
                "REGULAR", 1.00
        ));

        // Distance-based discount tiers (longer distances get better rates)
        distanceDiscountTiers.putAll(Map.of(
                100, 1.00,   // No discount
                300, 0.98,   // 2% discount
                500, 0.95,   // 5% discount
                800, 0.92,   // 8% discount
                1000, 0.90,  // 10% discount
                1500, 0.87,  // 13% discount
                2000, 0.85   // 15% discount
        ));
    }

    /**
     * Calculates sample fare for a given class and distance during initialization.
     *
     * @param trainClass Train class for fare calculation
     * @param distance Distance in kilometers
     * @return Calculated sample fare
     */
    private double calculateSampleFareForClass(TrainClass trainClass, int distance) {
        double baseFare = baseFareCache.get(trainClass);
        double reservationCharge = reservationChargeCache.get(trainClass);
        double totalFare = (distance * baseFare) + reservationCharge;

        // Apply distance discount using TreeMap floor entry
        Map.Entry<Integer, Double> entry = distanceDiscountTiers.floorEntry(distance);
        if (entry != null) {
            totalFare *= entry.getValue();
        }

        return Math.round(totalFare);
    }

    // =========================================================================
    // DYNAMIC FARE CALCULATION ENGINE
    // =========================================================================

    /**
     * Calculates dynamic fare using optimized algorithms and cached values.
     *
     * <h3>Calculation Process:</h3>
     * <ol>
     *   <li>Validates and normalizes input distance</li>
     *   <li>Retrieves cached base fare values</li>
     *   <li>Applies distance-based discounts using TreeMap floor entry</li>
     *   <li>Applies time-based multipliers (peak/off-peak/weekend)</li>
     *   <li>Applies class-specific premiums</li>
     *   <li>Enforces minimum fare requirements</li>
     *   <li>Rounds to nearest ₹5 for user-friendly pricing</li>
     * </ol>
     *
     * <h3>Performance Optimizations:</h3>
     * <ul>
     *   <li>O(1) cache lookups for base values</li>
     *   <li>O(log n) TreeMap floor entry for distance discounts</li>
     *   <li>Minimal conditional logic</li>
     *   <li>Single-pass multiplier application</li>
     * </ul>
     *
     * @param trainClass Train class for fare calculation
     * @param distanceKm Distance in kilometers (clamped to 50-2500 km range)
     * @return Calculated fare rounded to nearest ₹5
     */
    public double calculateDynamicFare(TrainClass trainClass, double distanceKm) {
        // Step 1: Validate and normalize distance
        distanceKm = Math.max(50, Math.min(distanceKm, 2500));

        // Step 2: Get cached base values (O(1) operations)
        double baseFarePerKm = getOptimizedBaseFare(trainClass, distanceKm);
        double reservationCharge = reservationChargeCache.get(trainClass);
        double baseFare = distanceKm * baseFarePerKm;

        // Step 3: Apply distance discount (O(log n) TreeMap operation)
        Map.Entry<Integer, Double> discountEntry = distanceDiscountTiers.floorEntry((int) distanceKm);
        if (discountEntry != null) {
            baseFare *= discountEntry.getValue();
        }

        // Step 4: Calculate total with reservation charge
        double totalFare = baseFare + reservationCharge;

        // Step 5: Apply time and class multipliers
        totalFare = applyOptimizedMultipliers(totalFare, trainClass);

        // Step 6: Ensure minimum fare compliance
        double minFare = minimumFareCache.get(trainClass);
        totalFare = Math.max(totalFare, minFare);

        // Step 7: Round to nearest ₹5 for user-friendly pricing
        return Math.round(totalFare / 5) * 5;
    }

    /**
     * Calculates optimized base fare with distance efficiency bonuses.
     * Longer distances receive better per-km rates to encourage long-distance travel.
     *
     * @param trainClass Train class for base fare lookup
     * @param distance Distance in kilometers
     * @return Optimized base fare per kilometer
     */
    private double getOptimizedBaseFare(TrainClass trainClass, double distance) {
        double baseFare = baseFareCache.get(trainClass);

        // Apply distance efficiency bonuses (single conditional chain)
        if (distance > 1500) {
            return baseFare * 0.85;  // 15% efficiency bonus for very long distances
        } else if (distance > 1000) {
            return baseFare * 0.90;  // 10% efficiency bonus for long distances
        } else if (distance > 500) {
            return baseFare * 0.95;  // 5% efficiency bonus for medium distances
        }

        return baseFare;  // No bonus for short distances
    }

    /**
     * Applies optimized multipliers for time-based and class-based pricing.
     *
     * <h3>Time-Based Multipliers:</h3>
     * <ul>
     *   <li>Weekend: 20% surcharge</li>
     *   <li>Peak Morning (6-10 AM): 15% surcharge</li>
     *   <li>Peak Evening (5-9 PM): 10% surcharge</li>
     *   <li>Off-Peak Night (10 PM-5 AM): 5% discount</li>
     *   <li>Regular Hours: No change</li>
     * </ul>
     *
     * <h3>Class-Based Premiums:</h3>
     * <ul>
     *   <li>AC First Class (1A): 8% premium</li>
     *   <li>AC 2-Tier (2A): 3% premium</li>
     *   <li>AC 3-Tier (3A): 1% premium</li>
     *   <li>Sleeper (SL): No premium</li>
     * </ul>
     *
     * @param fare Base fare amount
     * @param trainClass Train class for premium calculation
     * @return Fare with applied multipliers
     */
    private double applyOptimizedMultipliers(double fare, TrainClass trainClass) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue();

        // Step 1: Calculate time multiplier (single conditional chain)
        double timeMultiplier = 1.0;
        if (dayOfWeek >= 6) {  // Weekend
            timeMultiplier = timeBasedMultipliers.get("WEEKEND");
        } else if (hour >= 6 && hour <= 10) {  // Peak morning
            timeMultiplier = timeBasedMultipliers.get("PEAK_MORNING");
        } else if (hour >= 17 && hour <= 21) {  // Peak evening
            timeMultiplier = timeBasedMultipliers.get("PEAK_EVENING");
        } else if (hour >= 22 || hour <= 5) {  // Off-peak night
            timeMultiplier = timeBasedMultipliers.get("OFF_PEAK");
        }
        // Regular hours default to 1.0 (no change)

        // Step 2: Apply time multiplier
        fare *= timeMultiplier;

        // Step 3: Apply class premium (optimized switch expression)
        return switch (trainClass) {
            case _1A -> fare * 1.08;  // 8% premium for luxury
            case _2A -> fare * 1.03;  // 3% premium for comfort
            case _3A -> fare * 1.01;  // 1% premium for AC
            default -> fare;          // No premium for sleeper
        };
    }

    // =========================================================================
    // FARE STRUCTURE MANAGEMENT
    // =========================================================================

    /**
     * Sets fare for a specific train class and distance.
     * Updates the fare structure TreeMap for the given class.
     *
     * @param trainClass Train class to update fare for
     * @param distance Distance in kilometers
     * @param fare Fare amount to set
     */
    public void setFare(TrainClass trainClass, int distance, double fare) {
        fareStructureByClass.computeIfAbsent(trainClass, k -> new TreeMap<>()).put(distance, fare);
        addRecentActivity("Fare updated for " + trainClass + " at " + distance + "km: ₹" + fare);
    }

    /**
     * Retrieves all fare structures for administrative purposes.
     * Returns a defensive copy to prevent external modification.
     *
     * @return Map of train classes to their fare structures (defensive copy)
     */
    public Map<TrainClass, TreeMap<Integer, Double>> getAllFareStructures() {
        Map<TrainClass, TreeMap<Integer, Double>> result = new EnumMap<>(TrainClass.class);
        fareStructureByClass.forEach((key, value) -> result.put(key, new TreeMap<>(value)));
        return result;
    }

    // =========================================================================
    // ACTIVITY TRACKING AND MANAGEMENT
    // =========================================================================

    /**
     * Adds a new activity to the recent activities list.
     * Uses LinkedList for efficient LIFO operations and automatic size management.
     *
     * <h3>Features:</h3>
     * <ul>
     *   <li>Automatic timestamp addition</li>
     *   <li>LIFO ordering (most recent first)</li>
     *   <li>Automatic size limitation to 10 entries</li>
     *   <li>O(1) insertion at head</li>
     * </ul>
     *
     * @param activity Activity description to add
     */
    public void addRecentActivity(String activity) {
        String timestampedActivity = LocalDateTime.now().format(timeFormatter) + " - " + activity;
        recentActivities.addFirst(timestampedActivity);

        // Maintain efficient size limit (O(1) removal from tail)
        if (recentActivities.size() > 10) {
            recentActivities.removeLast();
        }
    }


    // =========================================================================
    // STATISTICS AND REPORTING
    // =========================================================================

    /**
     * Retrieves comprehensive travel statistics from multiple data sources.
     *
     * <h3>Statistics Include:</h3>
     * <ul>
     *   <li>Total registered users</li>
     *   <li>Active train count</li>
     *   <li>Total bookings processed</li>
     *   <li>Total revenue generated</li>
     * </ul>
     *
     * <h3>Fallback Strategy:</h3>
     * If database access fails, returns cached statistics to ensure system availability.
     *
     * @return TravelStatistics object with current system statistics
     */
    public TravelStatistics getTravelStatistics() {
        TravelStatistics stats = new TravelStatistics();
        try {
            // Attempt to fetch real-time statistics from database
            stats.setTotalUsers(new UserDAO().getUserCount());
            stats.setActiveTrains(new TrainDAO().getTrainCount());
            stats.setTotalBookings(new BookingDAO().getBookingCount());
            stats.setTotalRevenue(new BookingDAO().getTotalRevenue());

            addRecentActivity("Statistics fetched successfully from database");
        } catch (SQLException e) {
            addRecentActivity("Statistics fetch failed, using cached values: " + e.getMessage());

            // Fallback to cached values to ensure system availability
            stats.setTotalUsers(uniqueUsers.size());
            stats.setActiveTrains(trainList.size());
            stats.setTotalBookings(0);
            stats.setTotalRevenue(0.0);
        }
        return stats;
    }
}