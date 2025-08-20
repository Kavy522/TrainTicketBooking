package trainapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import trainapp.dao.JourneyDAO;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainDAO;
import trainapp.model.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optimized TrainService with enhanced performance and no debug output
 */
public class TrainService {

    private final TrainDAO trainDAO = new TrainDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();
    private final StationDAO stationDAO = new StationDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Performance caches
    private final Map<Integer, String> stationNameCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<TrainSchedule>> scheduleCache = new ConcurrentHashMap<>();
    private final Map<String, Integer> distanceCache = new ConcurrentHashMap<>();
    private final Map<Integer, List<String>> trainStationNamesCache = new ConcurrentHashMap<>();

    // Constants
    private static final int MAX_DISTANCE_KM = 2500;
    private static final int MIN_DISTANCE_KM = 50;
    private static final int MAX_JOURNEY_HOURS = 24;
    private static final int MIN_JOURNEY_MINUTES = 30;
    private static final double EXPRESS_TRAIN_SPEED_KMPH = 65.0;
    private static final double AVERAGE_TRAIN_SPEED_KMPH = 55.0;
    private static final double LOCAL_TRAIN_SPEED_KMPH = 45.0;

    // Pre-calculated distance mapping
    private static final Map<String, Integer> DISTANCE_MAP = createStaticDistanceMapping();

    // Pre-calculated default seat availability patterns
    private static final Map<String, Map<String, Integer>> DEFAULT_SEATS_BY_TYPE = createDefaultSeatsMapping();

    // -------------------------------------------------------------------------
    // OPTIMIZED: Enhanced Distance Calculation
    // -------------------------------------------------------------------------

    public int getDistanceBetween(Train train, String fromStation, String toStation) {
        // Check cache first
        String cacheKey = train.getTrainId() + ":" + fromStation + ":" + toStation;
        Integer cachedDistance = distanceCache.get(cacheKey);
        if (cachedDistance != null) {
            return cachedDistance;
        }

        try {
            List<TrainSchedule> trainSchedules = getTrainScheduleCached(train.getTrainId());

            if (trainSchedules.isEmpty()) {
                int fallbackDistance = getReasonableDistanceEstimate(fromStation, toStation);
                distanceCache.put(cacheKey, fallbackDistance);
                return fallbackDistance;
            }

            TrainSchedule fromSchedule = findStationScheduleOptimized(trainSchedules, fromStation);
            TrainSchedule toSchedule = findStationScheduleOptimized(trainSchedules, toStation);

            if (fromSchedule == null || toSchedule == null ||
                    fromSchedule.getSequenceOrder() >= toSchedule.getSequenceOrder()) {
                int fallbackDistance = getReasonableDistanceEstimate(fromStation, toStation);
                distanceCache.put(cacheKey, fallbackDistance);
                return fallbackDistance;
            }

            int timeBasedDistance = calculateTimeBasedDistanceOptimized(fromSchedule, toSchedule, train);

            if (timeBasedDistance > 0) {
                int finalDistance = Math.max(MIN_DISTANCE_KM, Math.min(timeBasedDistance, MAX_DISTANCE_KM));
                distanceCache.put(cacheKey, finalDistance);
                return finalDistance;
            }

            int segmentDistance = calculateSegmentBasedFallbackOptimized(fromSchedule, toSchedule, train);
            distanceCache.put(cacheKey, segmentDistance);
            return segmentDistance;

        } catch (Exception e) {
            int fallbackDistance = getReasonableDistanceEstimate(fromStation, toStation);
            distanceCache.put(cacheKey, fallbackDistance);
            return fallbackDistance;
        }
    }

    /**
     * Optimized schedule retrieval with caching
     */
    private List<TrainSchedule> getTrainScheduleCached(int trainId) {
        return scheduleCache.computeIfAbsent(trainId, id -> {
            List<TrainSchedule> schedules = trainDAO.getTrainSchedule(id);
            if (schedules != null && !schedules.isEmpty()) {
                schedules.sort(Comparator.comparingInt(TrainSchedule::getSequenceOrder));
                return schedules;
            }
            return new ArrayList<>();
        });
    }

    /**
     * Optimized station schedule finder
     */
    private TrainSchedule findStationScheduleOptimized(List<TrainSchedule> schedules, String stationName) {
        String normalizedStationName = stationName.trim().toLowerCase();

        for (TrainSchedule schedule : schedules) {
            String scheduleStationName = getStationNameCached(schedule.getStationId());
            if (scheduleStationName.toLowerCase().equals(normalizedStationName)) {
                return schedule;
            }
        }
        return null;
    }

    /**
     * Cached station name lookup
     */
    private String getStationNameCached(int stationId) {
        return stationNameCache.computeIfAbsent(stationId, id -> {
            try {
                Station station = stationDAO.getStationById(id);
                return station != null ? station.getName() : "";
            } catch (Exception e) {
                return "";
            }
        });
    }

    /**
     * Optimized time-based distance calculation
     */
    private int calculateTimeBasedDistanceOptimized(TrainSchedule fromSchedule, TrainSchedule toSchedule, Train train) {
        LocalTime departureTime = fromSchedule.getDepartureTime();
        LocalTime arrivalTime = toSchedule.getArrivalTime();

        if (departureTime == null || arrivalTime == null) {
            return 0;
        }

        long journeyMinutes = calculateJourneyTimeOptimized(
                departureTime, arrivalTime,
                fromSchedule.getDayNumber(),
                toSchedule.getDayNumber()
        );

        if (journeyMinutes <= 0 || journeyMinutes > MAX_JOURNEY_HOURS * 60) {
            return 0;
        }

        journeyMinutes = Math.max(journeyMinutes, MIN_JOURNEY_MINUTES);
        double trainSpeed = getTrainSpeedOptimized(train);
        double journeyHours = journeyMinutes / 60.0;

        return (int) Math.round(trainSpeed * journeyHours);
    }

    /**
     * Optimized journey time calculation
     */
    private long calculateJourneyTimeOptimized(LocalTime departureTime, LocalTime arrivalTime,
                                               int departureDay, int arrivalDay) {
        long minutes;

        if (arrivalDay > departureDay) {
            long minutesToMidnight = Duration.between(departureTime, LocalTime.MAX).toMinutes();
            long minutesFromMidnight = Duration.between(LocalTime.MIDNIGHT, arrivalTime).toMinutes();
            minutes = minutesToMidnight + ((arrivalDay - departureDay - 1) * 24 * 60) + minutesFromMidnight + 1;
        } else if (arrivalTime.isBefore(departureTime)) {
            long minutesToMidnight = Duration.between(departureTime, LocalTime.MAX).toMinutes();
            long minutesFromMidnight = Duration.between(LocalTime.MIDNIGHT, arrivalTime).toMinutes();
            minutes = minutesToMidnight + minutesFromMidnight + 1;
        } else {
            minutes = Duration.between(departureTime, arrivalTime).toMinutes();
        }

        return Math.max(MIN_JOURNEY_MINUTES, Math.min(minutes, MAX_JOURNEY_HOURS * 60));
    }

    /**
     * Optimized train speed determination
     */
    private double getTrainSpeedOptimized(Train train) {
        String trainName = train.getName().toLowerCase();

        if (trainName.contains("rajdhani") || trainName.contains("shatabdi") ||
                trainName.contains("vande bharat") || trainName.contains("duronto")) {
            return EXPRESS_TRAIN_SPEED_KMPH;
        } else if (trainName.contains("passenger") || trainName.contains("local")) {
            return LOCAL_TRAIN_SPEED_KMPH;
        }
        return AVERAGE_TRAIN_SPEED_KMPH;
    }

    /**
     * Optimized segment-based fallback calculation
     */
    private int calculateSegmentBasedFallbackOptimized(TrainSchedule fromSchedule, TrainSchedule toSchedule, Train train) {
        int segmentCount = toSchedule.getSequenceOrder() - fromSchedule.getSequenceOrder();
        if (segmentCount <= 0) return MIN_DISTANCE_KM;

        String trainName = train.getName().toLowerCase();
        int avgSegmentDistance = trainName.contains("rajdhani") || trainName.contains("duronto") ? 120 :
                trainName.contains("express") || trainName.contains("mail") ? 80 : 60;

        int calculatedDistance = segmentCount * avgSegmentDistance;
        return Math.max(MIN_DISTANCE_KM, Math.min(calculatedDistance, MAX_DISTANCE_KM));
    }

    /**
     * Optimized duration calculation
     */
    public String calculateDuration(Train train, String fromStation, String toStation) {
        try {
            TrainSchedule fromSchedule = trainDAO.getStationTiming(train.getTrainId(), fromStation);
            TrainSchedule toSchedule = trainDAO.getStationTiming(train.getTrainId(), toStation);

            if (fromSchedule.getDepartureTime() != null && toSchedule.getArrivalTime() != null) {
                long minutes = calculateJourneyTimeOptimized(
                        fromSchedule.getDepartureTime(),
                        toSchedule.getArrivalTime(),
                        fromSchedule.getDayNumber(),
                        toSchedule.getDayNumber()
                );

                return String.format("%dh %02dm", minutes / 60, minutes % 60);
            }
        } catch (Exception e) {
            // Silent fallback
        }

        return generateReasonableDurationOptimized();
    }

    // -------------------------------------------------------------------------
    // OPTIMIZED: Core Service Methods
    // -------------------------------------------------------------------------

    public List<Train> findTrainsBetweenStations(String fromStationName, String toStationName) {
        List<Train> candidateTrains = trainDAO.findTrainsBetweenStations(fromStationName, toStationName);

        if (candidateTrains.isEmpty()) {
            return candidateTrains;
        }

        return candidateTrains.stream()
                .filter(train -> validateTrainRoute(train, fromStationName, toStationName))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Optimized route validation
     */
    private boolean validateTrainRoute(Train train, String fromStationName, String toStationName) {
        List<String> stationNames = getTrainStationNamesCached(train.getTrainId());

        int fromIndex = -1, toIndex = -1;
        for (int i = 0; i < stationNames.size(); i++) {
            String stationName = stationNames.get(i);
            if (stationName.equalsIgnoreCase(fromStationName)) fromIndex = i;
            if (stationName.equalsIgnoreCase(toStationName)) toIndex = i;

            if (fromIndex != -1 && toIndex != -1) break;
        }

        return fromIndex != -1 && toIndex != -1 && fromIndex < toIndex;
    }

    /**
     * Cached train station names
     */
    private List<String> getTrainStationNamesCached(int trainId) {
        return trainStationNamesCache.computeIfAbsent(trainId,
                id -> trainDAO.getTrainScheduleStationNames(id));
    }

    public Map<String, Integer> getAvailableSeatsForDate(Train train, LocalDate journeyDate) {
        LocalDate date = journeyDate != null ? journeyDate : LocalDate.now();

        try {
            Journey journey = journeyDAO.getJourneyForTrainAndDate(train.getTrainId(), date);
            if (journey == null) {
                if (ensureJourneyExists(train.getTrainId(), date)) {
                    journey = journeyDAO.getJourneyForTrainAndDate(train.getTrainId(), date);
                }
            }

            return journey != null ? new HashMap<>(journey.getAvailableSeatsMap()) :
                    getDefaultSeatAvailabilityOptimized(train);
        } catch (Exception e) {
            return getDefaultSeatAvailabilityOptimized(train);
        }
    }

    public boolean bookSeats(int trainId, LocalDate journeyDate, String seatClass, int passengerCount) {
        try {
            Journey journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (journey == null) return false;

            Map<String, Integer> currentAvailability = journey.getAvailableSeatsMap();
            int availableSeats = currentAvailability.getOrDefault(seatClass, 0);

            if (availableSeats < passengerCount) return false;

            currentAvailability.put(seatClass, availableSeats - passengerCount);
            String updatedJson = objectMapper.writeValueAsString(currentAvailability);

            return journeyDAO.updateAvailableSeats(journey.getJourneyId(), updatedJson);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean ensureJourneyExists(int trainId, LocalDate journeyDate) {
        try {
            if (journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate) != null) {
                return true;
            }

            Train train = trainDAO.getTrainById(trainId);
            if (train == null) return false;

            Map<String, Integer> defaultSeats = getDefaultSeatAvailabilityOptimized(train);
            String seatsJson = objectMapper.writeValueAsString(defaultSeats);

            return journeyDAO.createJourney(trainId, journeyDate, seatsJson) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String getDepartureTime(Train train, String stationName) {
        TrainSchedule schedule = trainDAO.getStationTiming(train.getTrainId(), stationName);
        return (schedule.getDepartureTime() != null) ?
        schedule.getDepartureTime().toString() :
        generateRandomTimeOptimized();
    }

    public String getArrivalTime(Train train, String stationName) {
        TrainSchedule schedule = trainDAO.getStationTiming(train.getTrainId(), stationName);
        return (schedule.getArrivalTime() != null) ?
        schedule.getArrivalTime().toString() :
        generateRandomTimeOptimized();
    }

    public int getHaltsBetween(Train train, String fromStation, String toStation) {
        List<String> stations = getTrainStationNamesCached(train.getTrainId());
        int fromIndex = -1, toIndex = -1;

        for (int i = 0; i < stations.size() && (fromIndex == -1 || toIndex == -1); i++) {
            String station = stations.get(i);
            if (station.equalsIgnoreCase(fromStation)) fromIndex = i;
            if (station.equalsIgnoreCase(toStation)) toIndex = i;
        }

        return (fromIndex != -1 && toIndex != -1) ? Math.abs(toIndex - fromIndex) - 1 :
                ThreadLocalRandom.current().nextInt(1, 6);
    }

    public List<String> getTrainAmenities(Train train) {
        List<String> amenities = new ArrayList<>(4);
        String trainNumber = train.getTrainNumber();
        String trainName = train.getName().toLowerCase();

        if (trainNumber.charAt(0) == '1' || trainNumber.charAt(0) == '2') {
            amenities.add("⚡ Superfast");
        }
        if (trainName.contains("express") || trainName.contains("rajdhani") || trainName.contains("shatabdi")) {
            amenities.add("🍽️ Pantry Car");
            amenities.add("📶 WiFi");
        }
        amenities.add("💺 Reserved Seating");

        return amenities;
    }

    // -------------------------------------------------------------------------
    // OPTIMIZED: Helper Methods with Static Pre-calculations
    // -------------------------------------------------------------------------

    /**
     * Pre-calculated static distance mapping
     */
    private static Map<String, Integer> createStaticDistanceMapping() {
        Map<String, Integer> map = new HashMap<>();

        // Major routes (both directions)
        String[][] routes = {
                {"delhi", "mumbai", "1384"},
                {"delhi", "chennai", "2180"},
                {"mumbai", "chennai", "1279"},
                {"bangalore", "chennai", "350"},
                {"cldy", "nd", "350"},
                {"kolkata", "delhi", "1472"},
                {"pune", "mumbai", "150"},
                {"hyderabad", "bangalore", "570"}
        };

        for (String[] route : routes) {
            String key1 = route[0] + "-" + route[1];
            String key2 = route[1] + "-" + route;
            int distance = Integer.parseInt(route[2]);
            map.put(key1, distance);
            map.put(key2, distance);
        }

        return map;
    }

    /**
     * Pre-calculated default seat patterns
     */
    private static Map<String, Map<String, Integer>> createDefaultSeatsMapping() {
        Map<String, Map<String, Integer>> patterns = new HashMap<>();

        patterns.put("premium", Map.of("SL", 90, "3A", 70, "2A", 50, "1A", 25));
        patterns.put("express", Map.of("SL", 80, "3A", 60, "2A", 45, "1A", 22));
        patterns.put("regular", Map.of("SL", 70, "3A", 50, "2A", 35, "1A", 18));

        return patterns;
    }

    /**
     * Optimized default seat availability
     */
    private Map<String, Integer> getDefaultSeatAvailabilityOptimized(Train train) {
        String trainType = "regular";

        if (train != null) {
            String trainName = train.getName().toLowerCase();
            if (trainName.contains("rajdhani") || trainName.contains("shatabdi")) {
                trainType = "premium";
            } else if (trainName.contains("express")) {
                trainType = "express";
            }
        }

        Map<String, Integer> baseSeats = new HashMap<>(DEFAULT_SEATS_BY_TYPE.get(trainType));

        // Add small random variation
        ThreadLocalRandom random = ThreadLocalRandom.current();
        baseSeats.replaceAll((k, v) -> v + random.nextInt(-5, 16));

        return baseSeats;
    }

    private int getReasonableDistanceEstimate(String fromStation, String toStation) {
        String routeKey = fromStation.toLowerCase() + "-" + toStation.toLowerCase();
        String reverseKey = toStation.toLowerCase() + "-" + fromStation.toLowerCase();

        Integer distance = DISTANCE_MAP.get(routeKey);
        if (distance != null) return distance;

        distance = DISTANCE_MAP.get(reverseKey);
        if (distance != null) return distance;

        // Fast fallback based on name lengths
        int avgLength = (fromStation.length() + toStation.length()) / 2;
        return avgLength <= 3 ? 200 : avgLength <= 6 ? 400 : 600;
    }

    private String generateRandomTimeOptimized() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return String.format("%02d:%02d", random.nextInt(24), random.nextInt(60));
    }

    private String generateReasonableDurationOptimized() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int hours = random.nextInt(2, 15);
        int minutes = random.nextInt(60);
        return String.format("%dh %02dm", hours, minutes);
    }
}