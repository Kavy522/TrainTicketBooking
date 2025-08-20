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
 * Optimized AdminDataStructureService with enhanced performance and no debug output.
 */
public class AdminDataStructureService {

    // -------------------------------------------------------------------------
    // Data Structure Declarations
    // -------------------------------------------------------------------------

    private final LinkedList<String> recentActivities;
    private final HashMap<String, String> routeCache;
    private final Hashtable<String, String> userSessions;
    private final Map<TrainClass, TreeMap<Integer, Double>> fareStructureByClass;
    private final ArrayList<String> trainList;
    private final HashSet<String> uniqueUsers;
    private final HashMap<String, Double> timeBasedMultipliers;
    private final TreeMap<Integer, Double> distanceDiscountTiers;

    // Performance caches
    private final Map<TrainClass, Double> baseFareCache;
    private final Map<TrainClass, Double> reservationChargeCache;
    private final Map<TrainClass, Double> minimumFareCache;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    // -------------------------------------------------------------------------
    // Constructor & Initialization
    // -------------------------------------------------------------------------

    public AdminDataStructureService() {
        recentActivities = new LinkedList<>();
        routeCache = new HashMap<>();
        userSessions = new Hashtable<>();
        fareStructureByClass = new EnumMap<>(TrainClass.class);
        trainList = new ArrayList<>();
        uniqueUsers = new HashSet<>();
        timeBasedMultipliers = new HashMap<>();
        distanceDiscountTiers = new TreeMap<>();

        // Initialize performance caches
        baseFareCache = new EnumMap<>(TrainClass.class);
        reservationChargeCache = new EnumMap<>(TrainClass.class);
        minimumFareCache = new EnumMap<>(TrainClass.class);

        initializeData();
    }

    /**
     * Optimized initialization with batch operations
     */
    private void initializeData() {
        // Pre-populate caches for performance
        initializeFareCaches();

        // LinkedList: Recent Activities
        recentActivities.addAll(Arrays.asList(
                "Enhanced pricing system activated",
                "Time-based fare calculation enabled",
                "Database connection established",
                "Admin logged in",
                "Statistics refreshed"
        ));

        // HashMap: Route Cache
        routeCache.putAll(Map.of(
                "DEL-BOM", "Delhi to Mumbai Express Route - 1384km",
                "BOM-CHN", "Mumbai to Chennai Superfast - 1279km",
                "CHN-BLR", "Chennai to Bangalore Local - 350km",
                "BLR-DEL", "Bangalore to Delhi Rajdhani - 2150km",
                "CLDY-ND", "Calicut to New Delhi Express - 350km"
        ));

        // HashTable: User Sessions
        String currentTime = LocalDateTime.now().format(timeFormatter);
        userSessions.putAll(Map.of(
                "john_traveler", currentTime,
                "mary_explorer", currentTime,
                "admin_user", currentTime,
                "pricing_admin", currentTime
        ));

        // TreeMap: Fare Structure per class (batch initialization)
        int[] distances = {50, 100, 250, 500, 750, 1000, 1500, 2000};
        for (TrainClass trainClass : TrainClass.values()) {
            TreeMap<Integer, Double> classFares = new TreeMap<>();
            for (int distance : distances) {
                classFares.put(distance, calculateSampleFareForClass(trainClass, distance));
            }
            fareStructureByClass.put(trainClass, classFares);
        }

        // ArrayList: Train List
        trainList.addAll(Arrays.asList(
                "Rajdhani Express - Premium",
                "Shatabdi Express - Day Train",
                "Duronto Express - Non-stop",
                "Garib Rath - Budget",
                "Vande Bharat - Modern"
        ));

        // HashSet: Unique Users
        uniqueUsers.addAll(Arrays.asList(
                "john_traveler", "mary_explorer", "frequent_flyer",
                "business_user", "premium_traveler"
        ));

        // Initialize pricing data
        initializePricingData();
    }

    /**
     * Pre-populate fare caches for O(1) access
     */
    private void initializeFareCaches() {
        baseFareCache.put(TrainClass.SL, 0.75);
        baseFareCache.put(TrainClass._3A, 2.25);
        baseFareCache.put(TrainClass._2A, 3.50);
        baseFareCache.put(TrainClass._1A, 5.50);

        reservationChargeCache.put(TrainClass.SL, 30.0);
        reservationChargeCache.put(TrainClass._3A, 50.0);
        reservationChargeCache.put(TrainClass._2A, 75.0);
        reservationChargeCache.put(TrainClass._1A, 125.0);

        minimumFareCache.put(TrainClass.SL, 120.0);
        minimumFareCache.put(TrainClass._3A, 220.0);
        minimumFareCache.put(TrainClass._2A, 320.0);
        minimumFareCache.put(TrainClass._1A, 550.0);
    }

    private void initializePricingData() {
        // Time-based multipliers
        timeBasedMultipliers.putAll(Map.of(
                "PEAK_MORNING", 1.15,
                "PEAK_EVENING", 1.10,
                "WEEKEND", 1.20,
                "HOLIDAY", 1.25,
                "OFF_PEAK", 0.95,
                "REGULAR", 1.00
        ));

        // Distance-based discount tiers
        distanceDiscountTiers.putAll(Map.of(
                100, 1.00,
                300, 0.98,
                500, 0.95,
                800, 0.92,
                1000, 0.90,
                1500, 0.87,
                2000, 0.85
        ));
    }

    private double calculateSampleFareForClass(TrainClass trainClass, int distance) {
        double baseFare = baseFareCache.get(trainClass);
        double reservationCharge = reservationChargeCache.get(trainClass);
        double totalFare = (distance * baseFare) + reservationCharge;

        // Apply distance discount
        Map.Entry<Integer, Double> entry = distanceDiscountTiers.floorEntry(distance);
        if (entry != null) {
            totalFare *= entry.getValue();
        }

        return Math.round(totalFare);
    }

    // -------------------------------------------------------------------------
    // OPTIMIZED: Enhanced Dynamic Fare Calculation
    // -------------------------------------------------------------------------

    /**
     * Optimized dynamic fare calculation with cached values and minimal operations
     */
    public double calculateDynamicFare(TrainClass trainClass, double distanceKm) {
        // Validate and normalize distance
        distanceKm = Math.max(50, Math.min(distanceKm, 2500));

        // Step 1: Get cached base values
        double baseFarePerKm = getOptimizedBaseFare(trainClass, distanceKm);
        double reservationCharge = reservationChargeCache.get(trainClass);
        double baseFare = distanceKm * baseFarePerKm;

        // Step 2: Apply distance discount (single TreeMap lookup)
        Map.Entry<Integer, Double> discountEntry = distanceDiscountTiers.floorEntry((int) distanceKm);
        if (discountEntry != null) {
            baseFare *= discountEntry.getValue();
        }

        // Step 3: Calculate total with reservation
        double totalFare = baseFare + reservationCharge;

        // Step 4: Apply time and class multipliers
        totalFare = applyOptimizedMultipliers(totalFare, trainClass);

        // Step 5: Ensure minimum fare
        double minFare = minimumFareCache.get(trainClass);
        totalFare = Math.max(totalFare, minFare);

        // Step 6: Round to nearest 5
        return Math.round(totalFare / 5) * 5;
    }

    /**
     * Optimized base fare calculation with distance efficiency
     */
    private double getOptimizedBaseFare(TrainClass trainClass, double distance) {
        double baseFare = baseFareCache.get(trainClass);

        // Apply distance efficiency (single conditional block)
        if (distance > 1500) {
            return baseFare * 0.85;
        } else if (distance > 1000) {
            return baseFare * 0.90;
        } else if (distance > 500) {
            return baseFare * 0.95;
        }

        return baseFare;
    }

    /**
     * Optimized multiplier application with cached time calculation
     */
    private double applyOptimizedMultipliers(double fare, TrainClass trainClass) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int dayOfWeek = now.getDayOfWeek().getValue();

        // Single conditional chain for time multiplier
        double timeMultiplier = 1.0;
        if (dayOfWeek >= 6) {
            timeMultiplier = timeBasedMultipliers.get("WEEKEND");
        } else if (hour >= 6 && hour <= 10) {
            timeMultiplier = timeBasedMultipliers.get("PEAK_MORNING");
        } else if (hour >= 17 && hour <= 21) {
            timeMultiplier = timeBasedMultipliers.get("PEAK_EVENING");
        } else if (hour >= 22 || hour <= 5) {
            timeMultiplier = timeBasedMultipliers.get("OFF_PEAK");
        }

        // Apply time multiplier
        fare *= timeMultiplier;

        // Apply class premium (optimized switch)
        switch (trainClass) {
            case _1A:
                return fare * 1.08;
            case _2A:
                return fare * 1.03;
            case _3A:
                return fare * 1.01;
            default:
                return fare;
        }
    }


    // -------------------------------------------------------------------------
    // OPTIMIZED: TreeMap Operations
    // -------------------------------------------------------------------------

    public void setFare(TrainClass trainClass, int distance, double fare) {
        fareStructureByClass.computeIfAbsent(trainClass, k -> new TreeMap<>()).put(distance, fare);
    }

    public TreeMap<Integer, Double> getFareStructureForClass(TrainClass trainClass) {
        return new TreeMap<>(fareStructureByClass.getOrDefault(trainClass, new TreeMap<>()));
    }

    public Map<TrainClass, TreeMap<Integer, Double>> getAllFareStructures() {
        return new EnumMap<>(fareStructureByClass);
    }


    // -------------------------------------------------------------------------
    // OPTIMIZED: Data Structure Utility Methods
    // -------------------------------------------------------------------------

    public void addRecentActivity(String activity) {
        recentActivities.addFirst(LocalDateTime.now().format(timeFormatter) + " - " + activity);

        // Efficient size maintenance
        if (recentActivities.size() > 10) {
            recentActivities.removeLast();
        }
    }

    // -------------------------------------------------------------------------
    // OPTIMIZED: Statistics and Reporting
    // -------------------------------------------------------------------------

    public TravelStatistics getTravelStatistics() {
        TravelStatistics stats = new TravelStatistics();
        try {
            stats.setTotalUsers(new UserDAO().getUserCount());
            stats.setActiveTrains(new TrainDAO().getTrainCount());
            stats.setTotalBookings(new BookingDAO().getBookingCount());
            stats.setTotalRevenue(new BookingDAO().getTotalRevenue());

            addRecentActivity("Statistics fetched successfully");
        } catch (SQLException e) {
            addRecentActivity("Statistics fetch failed: " + e.getMessage());

            // Fallback values
            stats.setTotalUsers(uniqueUsers.size());
            stats.setActiveTrains(trainList.size());
            stats.setTotalBookings(0);
            stats.setTotalRevenue(0.0);
        }
        return stats;
    }
}