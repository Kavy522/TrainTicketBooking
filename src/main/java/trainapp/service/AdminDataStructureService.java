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
 * in a train booking administration system context with enhanced time-based fare calculation.
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
 * <p>Enhanced features:
 * <ul>
 *   <li>Time-sensitive dynamic fare calculation</li>
 *   <li>Distance-based pricing optimization</li>
 *   <li>Peak/off-peak multipliers</li>
 *   <li>Class-based premium adjustments</li>
 * </ul>
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

    /**
     * HashMap for time-based fare multipliers (peak/off-peak pricing)
     */
    private final HashMap<String, Double> timeBasedMultipliers;

    /**
     * TreeMap for distance-based discount tiers (automatically sorted)
     */
    private final TreeMap<Integer, Double> distanceDiscountTiers;

    // -------------------------------------------------------------------------
    // Constructor & Initialization
    // -------------------------------------------------------------------------

    /**
     * Constructs the service and initializes all data structures with enhanced sample data.
     * Demonstrates proper initialization patterns for different collection types.
     */
    public AdminDataStructureService() {
        recentActivities = new LinkedList<>();
        routeCache = new HashMap<>();
        userSessions = new Hashtable<>();
        fareStructureByClass = new HashMap<>();
        trainList = new ArrayList<>();
        uniqueUsers = new HashSet<>();
        timeBasedMultipliers = new HashMap<>();
        distanceDiscountTiers = new TreeMap<>();

        initializeSampleData();
        initializeEnhancedPricingData();
    }

    /**
     * Initializes all data structures with representative sample data.
     * Demonstrates proper population patterns for each collection type.
     */
    private void initializeSampleData() {
        // LinkedList: Recent Activities (FIFO order important)
        recentActivities.addFirst("Enhanced pricing system activated");
        recentActivities.addFirst("Time-based fare calculation enabled");
        recentActivities.addFirst("Database connection established");
        recentActivities.addFirst("Admin logged in");
        recentActivities.addFirst("Statistics refreshed");

        // HashMap: Route Cache (Key-Value pairs for fast lookup)
        routeCache.put("DEL-BOM", "Delhi to Mumbai Express Route - 1384km");
        routeCache.put("BOM-CHN", "Mumbai to Chennai Superfast - 1279km");
        routeCache.put("CHN-BLR", "Chennai to Bangalore Local - 350km");
        routeCache.put("BLR-DEL", "Bangalore to Delhi Rajdhani - 2150km");
        routeCache.put("CLDY-ND", "Calicut to New Delhi Express - 350km");

        // HashTable: User Sessions (Thread-safe for concurrent access)
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        userSessions.put("john_traveler", currentTime);
        userSessions.put("mary_explorer", currentTime);
        userSessions.put("admin_user", currentTime);
        userSessions.put("pricing_admin", currentTime);

        // TreeMap: Enhanced Fare Structure per class (Automatically sorted by distance)
        for (TrainClass trainClass : TrainClass.values()) {
            TreeMap<Integer, Double> classFares = new TreeMap<>();
            classFares.put(50, getEnhancedSampleFareForClass(trainClass, 50));
            classFares.put(100, getEnhancedSampleFareForClass(trainClass, 100));
            classFares.put(250, getEnhancedSampleFareForClass(trainClass, 250));
            classFares.put(500, getEnhancedSampleFareForClass(trainClass, 500));
            classFares.put(750, getEnhancedSampleFareForClass(trainClass, 750));
            classFares.put(1000, getEnhancedSampleFareForClass(trainClass, 1000));
            classFares.put(1500, getEnhancedSampleFareForClass(trainClass, 1500));
            classFares.put(2000, getEnhancedSampleFareForClass(trainClass, 2000));
            fareStructureByClass.put(trainClass, classFares);
        }

        // ArrayList: Enhanced Train List
        trainList.add("Rajdhani Express - Premium");
        trainList.add("Shatabdi Express - Day Train");
        trainList.add("Duronto Express - Non-stop");
        trainList.add("Garib Rath - Budget");
        trainList.add("Vande Bharat - Modern");

        // HashSet: Unique Users
        uniqueUsers.add("john_traveler");
        uniqueUsers.add("mary_explorer");
        uniqueUsers.add("frequent_flyer");
        uniqueUsers.add("business_user");
        uniqueUsers.add("premium_traveler");
    }

    /**
     * Initialize enhanced pricing data structures for time-based and distance-based pricing.
     */
    private void initializeEnhancedPricingData() {
        // HashMap: Time-based multipliers for different time periods
        timeBasedMultipliers.put("PEAK_MORNING", 1.15);    // 6 AM - 10 AM
        timeBasedMultipliers.put("PEAK_EVENING", 1.10);    // 5 PM - 9 PM
        timeBasedMultipliers.put("WEEKEND", 1.20);         // Saturday/Sunday
        timeBasedMultipliers.put("HOLIDAY", 1.25);         // Public holidays
        timeBasedMultipliers.put("OFF_PEAK", 0.95);        // Late night/early morning
        timeBasedMultipliers.put("REGULAR", 1.00);         // Normal hours

        // TreeMap: Distance-based discount tiers (auto-sorted by distance)
        distanceDiscountTiers.put(100, 1.00);   // No discount for short distances
        distanceDiscountTiers.put(300, 0.98);   // 2% discount for 300+ km
        distanceDiscountTiers.put(500, 0.95);   // 5% discount for 500+ km
        distanceDiscountTiers.put(800, 0.92);   // 8% discount for 800+ km
        distanceDiscountTiers.put(1000, 0.90);  // 10% discount for 1000+ km
        distanceDiscountTiers.put(1500, 0.87);  // 13% discount for 1500+ km
        distanceDiscountTiers.put(2000, 0.85);  // 15% discount for 2000+ km
    }

    /**
     * Enhanced sample fare calculation with realistic Indian Railway pricing.
     */
    private double getEnhancedSampleFareForClass(TrainClass trainClass, int distance) {
        double baseFarePerKm = getBaseFarePerKm(trainClass);
        double reservationCharge = getReservationCharge(trainClass);

        double totalFare = (distance * baseFarePerKm) + reservationCharge;

        // Apply distance discount
        double discountMultiplier = getDistanceDiscountMultiplier(distance);
        totalFare *= discountMultiplier;

        return Math.round(totalFare);
    }

    // -------------------------------------------------------------------------
    // Enhanced Dynamic Fare Calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates dynamic fare with enhanced time-based distance validation and multiple pricing factors.
     * This is the main fare calculation method that considers all pricing variables.
     */
    public double calculateDynamicFare(TrainClass trainClass, double distanceKm) {
        System.out.println("=== Enhanced Dynamic Fare Calculation ===");
        System.out.println("Calculating fare for " + trainClass + " class, distance: " + distanceKm + " km");

        // Validate and normalize distance with enhanced bounds
        if (distanceKm <= 0 || distanceKm > 3000) {
            System.err.println("Invalid distance: " + distanceKm + ", applying correction");
            distanceKm = Math.max(50, Math.min(distanceKm, 2500));
        }

        // Step 1: Calculate base fare components
        double baseFarePerKm = getTimeSensitiveBaseFare(trainClass, distanceKm);
        double reservationCharge = getReservationCharge(trainClass);
        double baseFare = distanceKm * baseFarePerKm;

        System.out.println("Base fare: ₹" + String.format("%.2f", baseFare) +
                " (₹" + baseFarePerKm + "/km × " + distanceKm + "km)");
        System.out.println("Reservation charge: ₹" + reservationCharge);

        // Step 2: Apply distance-based discounts using TreeMap
        double distanceMultiplier = getDistanceDiscountMultiplier((int) distanceKm);
        baseFare *= distanceMultiplier;

        if (distanceMultiplier < 1.0) {
            System.out.println("Distance discount applied: " +
                    String.format("%.1f%%", (1 - distanceMultiplier) * 100));
        }

        // Step 3: Calculate total fare
        double totalFare = baseFare + reservationCharge;

        // Step 4: Apply time-based multipliers
        totalFare = applyTimeBasedMultipliers(totalFare, trainClass);

        // Step 5: Apply class-specific premium adjustments
        totalFare = applyClassPremiumAdjustments(totalFare, trainClass);

        // Step 6: Ensure minimum fare constraints
        double minFare = getMinimumFare(trainClass);
        totalFare = Math.max(totalFare, minFare);

        // Step 7: Round to nearest 5 for user-friendly pricing
        double finalFare = Math.round(totalFare / 5) * 5;

        System.out.println("Final calculated fare: ₹" + finalFare + " for " + trainClass +
                " class, " + distanceKm + " km");
        System.out.println("=== Fare Calculation Complete ===");

        return finalFare;
    }

    /**
     * Get time-sensitive base fare that considers journey duration patterns and efficiency.
     */
    private double getTimeSensitiveBaseFare(TrainClass trainClass, double distance) {
        double baseFare = getBaseFarePerKm(trainClass);

        // Apply distance-based efficiency (longer journeys are more cost-effective per km)
        if (distance > 1500) {
            baseFare *= 0.85; // 15% efficiency for very long journeys
        } else if (distance > 1000) {
            baseFare *= 0.90; // 10% efficiency for long journeys
        } else if (distance > 500) {
            baseFare *= 0.95; // 5% efficiency for medium journeys
        }

        return baseFare;
    }

    /**
     * Get distance-based discount multiplier using TreeMap for efficient range queries.
     */
    private double getDistanceDiscountMultiplier(int distance) {
        // Use TreeMap's floorEntry to find the appropriate discount tier
        Map.Entry<Integer, Double> entry = distanceDiscountTiers.floorEntry(distance);
        return entry != null ? entry.getValue() : 1.0;
    }

    /**
     * Apply time-based multipliers for peak/off-peak pricing using current time.
     */
    private double applyTimeBasedMultipliers(double fare, TrainClass trainClass) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue();

        double timeMultiplier = 1.0;
        String timeCategory = "REGULAR";

        // Determine time category
        if (dayOfWeek >= 6) { // Weekend
            timeMultiplier = timeBasedMultipliers.get("WEEKEND");
            timeCategory = "WEEKEND";
        } else if (hour >= 6 && hour <= 10) { // Peak morning
            timeMultiplier = timeBasedMultipliers.get("PEAK_MORNING");
            timeCategory = "PEAK_MORNING";
        } else if (hour >= 17 && hour <= 21) { // Peak evening
            timeMultiplier = timeBasedMultipliers.get("PEAK_EVENING");
            timeCategory = "PEAK_EVENING";
        } else if (hour >= 22 || hour <= 5) { // Off-peak
            timeMultiplier = timeBasedMultipliers.get("OFF_PEAK");
            timeCategory = "OFF_PEAK";
        } else {
            timeMultiplier = timeBasedMultipliers.get("REGULAR");
        }

        if (timeMultiplier != 1.0) {
            System.out.println("Time-based adjustment (" + timeCategory + "): " +
                    String.format("%.1f%%", (timeMultiplier - 1) * 100));
        }

        return fare * timeMultiplier;
    }

    /**
     * Apply class-specific premium adjustments for luxury and comfort features.
     */
    private double applyClassPremiumAdjustments(double fare, TrainClass trainClass) {
        double premium = 1.0;

        switch (trainClass) {
            case _1A:
                premium = 1.08; // 8% premium for luxury amenities
                System.out.println("First Class premium applied: 8%");
                break;
            case _2A:
                premium = 1.03; // 3% premium for enhanced comfort
                System.out.println("2AC premium applied: 3%");
                break;
            case _3A:
                premium = 1.01; // 1% premium for AC comfort
                break;
            case SL:
            default:
                premium = 1.0; // No premium for basic classes
                break;
        }

        return fare * premium;
    }

    // -------------------------------------------------------------------------
    // Core Pricing Methods
    // -------------------------------------------------------------------------

    /**
     * Enhanced base fare per km with realistic Indian Railway rates.
     */
    private double getBaseFarePerKm(TrainClass trainClass) {
        switch (trainClass) {
            case SL: return 0.75;   // ₹0.75 per km for Sleeper
            case _3A: return 2.25;  // ₹2.25 per km for 3AC
            case _2A: return 3.50;  // ₹3.50 per km for 2AC
            case _1A: return 5.50;  // ₹5.50 per km for 1AC
            default: return 2.25;
        }
    }

    /**
     * Get enhanced reservation charges for each class.
     */
    private double getReservationCharge(TrainClass trainClass) {
        switch (trainClass) {
            case SL: return 30;   // ₹30 reservation for Sleeper
            case _3A: return 50;  // ₹50 reservation for 3AC
            case _2A: return 75;  // ₹75 reservation for 2AC
            case _1A: return 125; // ₹125 reservation for 1AC
            default: return 50;
        }
    }

    /**
     * Get enhanced minimum fare for each class with improved thresholds.
     */
    private double getMinimumFare(TrainClass trainClass) {
        switch (trainClass) {
            case SL: return 120;  // Minimum ₹120 for Sleeper
            case _3A: return 220; // Minimum ₹220 for 3AC
            case _2A: return 320; // Minimum ₹320 for 2AC
            case _1A: return 550; // Minimum ₹550 for 1AC
            default: return 220;
        }
    }

    // -------------------------------------------------------------------------
    // TreeMap Operations - Fare Structure Management
    // -------------------------------------------------------------------------

    /**
     * Sets fare for specific distance and train class.
     * Demonstrates TreeMap.put() with automatic sorting by distance.
     */
    public void setFare(TrainClass trainClass, int distance, double fare) {
        TreeMap<Integer, Double> classFares = fareStructureByClass.computeIfAbsent(trainClass, k -> new TreeMap<>());
        classFares.put(distance, fare);
        System.out.println("Updated fare: " + trainClass + " - " + distance + "km = ₹" + fare);
    }

    /**
     * Gets complete fare structure for a specific train class.
     */
    public TreeMap<Integer, Double> getFareStructureForClass(TrainClass trainClass) {
        return new TreeMap<>(fareStructureByClass.getOrDefault(trainClass, new TreeMap<>()));
    }

    /**
     * Returns all fare structures for all train classes.
     */
    public Map<TrainClass, TreeMap<Integer, Double>> getAllFareStructures() {
        return new HashMap<>(fareStructureByClass);
    }

    /**
     * Get fare for a specific distance range using TreeMap navigation.
     */
    public double getFareForDistance(TrainClass trainClass, int distance) {
        TreeMap<Integer, Double> classFares = getFareStructureForClass(trainClass);

        // Try exact match first
        if (classFares.containsKey(distance)) {
            return classFares.get(distance);
        }

        // Use interpolation between nearest values
        Map.Entry<Integer, Double> lower = classFares.floorEntry(distance);
        Map.Entry<Integer, Double> higher = classFares.ceilingEntry(distance);

        if (lower != null && higher != null) {
            // Linear interpolation
            double ratio = (double)(distance - lower.getKey()) / (higher.getKey() - lower.getKey());
            return lower.getValue() + ratio * (higher.getValue() - lower.getValue());
        } else if (lower != null) {
            // Extrapolate from lower bound
            return lower.getValue() * ((double)distance / lower.getKey());
        } else if (higher != null) {
            // Extrapolate from higher bound
            return higher.getValue() * ((double)distance / higher.getKey());
        }

        // Fallback to dynamic calculation
        return calculateDynamicFare(trainClass, distance);
    }

    // -------------------------------------------------------------------------
    // Data Structure Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Add recent activity to LinkedList (FIFO pattern).
     */
    public void addRecentActivity(String activity) {
        recentActivities.addFirst(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " - " + activity);

        // Maintain maximum 10 recent activities
        if (recentActivities.size() > 10) {
            recentActivities.removeLast();
        }
    }

    /**
     * Get recent activities from LinkedList.
     */
    public List<String> getRecentActivities() {
        return new ArrayList<>(recentActivities);
    }

    /**
     * Cache route information in HashMap.
     */
    public void cacheRoute(String routeKey, String routeInfo) {
        routeCache.put(routeKey.toUpperCase(), routeInfo);
    }

    /**
     * Get cached route information.
     */
    public String getCachedRoute(String routeKey) {
        return routeCache.get(routeKey.toUpperCase());
    }

    /**
     * Track unique users in HashSet.
     */
    public void trackUser(String userId) {
        uniqueUsers.add(userId);
        System.out.println("Total unique users: " + uniqueUsers.size());
    }

    /**
     * Get unique user count.
     */
    public int getUniqueUserCount() {
        return uniqueUsers.size();
    }

    // -------------------------------------------------------------------------
    // Statistics and Reporting
    // -------------------------------------------------------------------------

    /**
     * Fetches real travel statistics from database with enhanced error handling.
     */
    public TravelStatistics getTravelStatistics() {
        TravelStatistics stats = new TravelStatistics();
        try {
            stats.setTotalUsers(new UserDAO().getUserCount());
            stats.setActiveTrains(new TrainDAO().getTrainCount());
            stats.setTotalBookings(new BookingDAO().getBookingCount());
            stats.setTotalRevenue(new BookingDAO().getTotalRevenue());

            addRecentActivity("Statistics fetched successfully");
        } catch (SQLException e) {
            System.err.println("Error fetching travel statistics: " + e.getMessage());
            addRecentActivity("Statistics fetch failed: " + e.getMessage());

            // Enhanced fallback values
            stats.setTotalUsers(uniqueUsers.size()); // Use tracked users
            stats.setActiveTrains(trainList.size());  // Use train list size
            stats.setTotalBookings(0);
            stats.setTotalRevenue(0.0);
        }
        return stats;
    }

    /**
     * Generate comprehensive pricing report using all data structures.
     */
    public Map<String, Object> generatePricingReport() {
        Map<String, Object> report = new HashMap<>();

        // Basic statistics
        report.put("uniqueUsers", uniqueUsers.size());
        report.put("totalRoutesCached", routeCache.size());
        report.put("activeSessions", userSessions.size());
        report.put("trainCount", trainList.size());

        // Fare structure analysis
        Map<String, Integer> fareStructureSize = new HashMap<>();
        for (TrainClass trainClass : fareStructureByClass.keySet()) {
            fareStructureSize.put(trainClass.toString(), fareStructureByClass.get(trainClass).size());
        }
        report.put("fareStructureSizes", fareStructureSize);

        // Recent activities
        report.put("recentActivities", getRecentActivities());

        // Time-based multipliers
        report.put("timeBasedMultipliers", new HashMap<>(timeBasedMultipliers));

        // Distance discount tiers
        report.put("distanceDiscountTiers", new TreeMap<>(distanceDiscountTiers));

        addRecentActivity("Comprehensive pricing report generated");
        return report;
    }
}