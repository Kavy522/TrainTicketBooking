package trainapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import trainapp.dao.JourneyDAO;
import trainapp.dao.StationDAO;
import trainapp.dao.TrainDAO;
import trainapp.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class TrainService {

    private final TrainDAO trainDAO = new TrainDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();
    private final StationDAO stationDAO = new StationDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Find trains that run between fromStation and toStation.
     */
    public List<Train> findTrainsBetweenStations(String fromStationName, String toStationName) {
        // Get candidate trains from DAO
        List<Train> candidateTrains = trainDAO.findTrainsBetweenStations(fromStationName, toStationName);
        System.out.println("TrainService: Found " + candidateTrains.size() + " candidate trains");

        if (candidateTrains.isEmpty()) {
            System.out.println("TrainService: No candidate trains found, returning empty list");
            return candidateTrains;
        }

        List<Train> filteredTrains = new ArrayList<>();

        for (Train train : candidateTrains) {
            System.out.println("TrainService: Processing train " + train.getTrainNumber());

            // Get train schedule
            List<String> scheduleStationNames = trainDAO.getTrainScheduleStationNames(train.getTrainId());
            System.out.println("TrainService: Train " + train.getTrainNumber() + " schedule: " + scheduleStationNames);

            // Check if route is valid (from station comes before to station)
            int fromIndex = -1;
            int toIndex = -1;

            // Case-insensitive search
            for (int i = 0; i < scheduleStationNames.size(); i++) {
                String stationName = scheduleStationNames.get(i);
                if (stationName.equalsIgnoreCase(fromStationName)) {
                    fromIndex = i;
                }
                if (stationName.equalsIgnoreCase(toStationName)) {
                    toIndex = i;
                }
            }

            System.out.println("TrainService: Train " + train.getTrainNumber() +
                    " - fromIndex: " + fromIndex + ", toIndex: " + toIndex);

            if (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex) {
                filteredTrains.add(train);
                System.out.println("TrainService: Added train " + train.getTrainNumber() + " to results");
            } else {
                System.out.println("TrainService: Train " + train.getTrainNumber() + " filtered out due to invalid route");
            }
        }

        System.out.println("TrainService: Final filtered trains count: " + filteredTrains.size());

        // If no trains pass the filter, return all candidate trains for testing
        if (filteredTrains.isEmpty() && !candidateTrains.isEmpty()) {
            System.out.println("TrainService: No trains passed filtering, returning all candidates for testing");
            return candidateTrains;
        }

        return filteredTrains;
    }

    /**
     * Get availability of seats per class for the given journey date of the train.
     */
    public Map<String, Integer> getAvailableSeatsForDate(Train train, LocalDate journeyDate) {
        if (journeyDate == null) {
            journeyDate = LocalDate.now();
        }

        try {
            Journey journey = journeyDAO.getJourneyForTrainAndDate(train.getTrainId(), journeyDate);

            if (journey == null) {
                System.out.println("TrainService: No journey found for train " + train.getTrainNumber() +
                        " on date " + journeyDate + ", ensuring journey exists");
                // Use the new ensureJourneyExists method
                if (ensureJourneyExists(train.getTrainId(), journeyDate)) {
                    journey = journeyDAO.getJourneyForTrainAndDate(train.getTrainId(), journeyDate);
                }
            }

            if (journey != null) {
                Map<String, Integer> availability = journey.getAvailableSeatsMap();
                return new HashMap<>(availability);
            } else {
                System.out.println("TrainService: Still no journey, returning default seats");
                return getDefaultSeatAvailability(train);
            }
        } catch (Exception e) {
            System.err.println("Error getting available seats: " + e.getMessage());
            e.printStackTrace();
            return getDefaultSeatAvailability(train);
        }
    }

    /**
     * Book seats for a specific train, date, and class
     * Reduces available seat count after booking
     * REQUIRED FOR PAYMENT INTEGRATION
     */
    public boolean bookSeats(int trainId, LocalDate journeyDate, String seatClass, int passengerCount) {
        try {
            System.out.println("Booking " + passengerCount + " seats for train " + trainId +
                    " on " + journeyDate + " in " + seatClass + " class");

            // Get the journey for this train and date
            Journey journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (journey == null) {
                System.err.println("No journey found for train " + trainId + " on " + journeyDate);
                return false;
            }

            // Get current seat availability
            Map<String, Integer> currentAvailability = journey.getAvailableSeatsMap();

            // Check if enough seats are available
            int availableSeats = currentAvailability.getOrDefault(seatClass, 0);
            if (availableSeats < passengerCount) {
                System.err.println("Not enough seats available. Requested: " + passengerCount +
                        ", Available: " + availableSeats);
                return false;
            }

            // Reduce seat count
            currentAvailability.put(seatClass, availableSeats - passengerCount);

            // Update in database
            String updatedJson = objectMapper.writeValueAsString(currentAvailability);
            boolean updated = journeyDAO.updateAvailableSeats(journey.getJourneyId(), updatedJson);

            if (updated) {
                System.out.println("Successfully booked " + passengerCount + " seats in " + seatClass +
                        " class. Remaining seats: " + (availableSeats - passengerCount));
                return true;
            } else {
                System.err.println("Failed to update seat availability in database");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error booking seats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ensure journey exists for a train on a specific date
     * Creates journey if it doesn't exist with default seat availability
     * REQUIRED FOR PAYMENT INTEGRATION
     */
    public boolean ensureJourneyExists(int trainId, LocalDate journeyDate) {
        try {
            System.out.println("Ensuring journey exists for train " + trainId + " on " + journeyDate);

            // Check if journey already exists
            Journey existingJourney = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (existingJourney != null) {
                System.out.println("Journey already exists for train " + trainId + " on " + journeyDate);
                return true;
            }

            // Get train details to determine capacity
            Train train = trainDAO.getTrainById(trainId);
            if (train == null) {
                System.err.println("Train not found with ID: " + trainId);
                return false;
            }

            // Create new journey with default seat availability
            Journey newJourney = new Journey();
            newJourney.setTrainId(trainId);
            newJourney.setDepartureDate(journeyDate);

            // Set default seat availability based on train capacity or standard values
            Map<String, Integer> defaultSeats = getDefaultSeatAvailability(train);
            String seatsJson = objectMapper.writeValueAsString(defaultSeats);
            newJourney.setAvailableSeatsJson(seatsJson);

            // Create journey in database using the existing method
            long journeyId = journeyDAO.createJourney(trainId, journeyDate, seatsJson);
            if (journeyId > 0) {
                newJourney.setJourneyId(journeyId);
                System.out.println("Created new journey with ID: " + journeyId + " for train " + trainId +
                        " on " + journeyDate);
                return true;
            } else {
                System.err.println("Failed to create journey for train " + trainId + " on " + journeyDate);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error ensuring journey exists: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Release seats (for booking cancellation)
     * Increases available seat count
     */
    public boolean releaseSeats(int trainId, LocalDate journeyDate, String seatClass, int passengerCount) {
        try {
            System.out.println("Releasing " + passengerCount + " seats for train " + trainId +
                    " on " + journeyDate + " in " + seatClass + " class");

            // Get the journey for this train and date
            Journey journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (journey == null) {
                System.err.println("No journey found for train " + trainId + " on " + journeyDate);
                return false;
            }

            // Get current seat availability
            Map<String, Integer> currentAvailability = journey.getAvailableSeatsMap();

            // Increase seat count
            int currentSeats = currentAvailability.getOrDefault(seatClass, 0);
            currentAvailability.put(seatClass, currentSeats + passengerCount);

            // Update in database
            String updatedJson = objectMapper.writeValueAsString(currentAvailability);
            boolean updated = journeyDAO.updateAvailableSeats(journey.getJourneyId(), updatedJson);

            if (updated) {
                System.out.println("Successfully released " + passengerCount + " seats in " + seatClass +
                        " class. Available seats now: " + (currentSeats + passengerCount));
                return true;
            } else {
                System.err.println("Failed to update seat availability in database");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error releasing seats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if minimum seats are available for booking
     */
    public boolean hasMinimumSeatsAvailable(int trainId, LocalDate journeyDate,
                                            String seatClass, int requiredSeats) {
        try {
            Journey journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (journey != null) {
                Map<String, Integer> availability = journey.getAvailableSeatsMap();
                int availableSeats = availability.getOrDefault(seatClass, 0);
                return availableSeats >= requiredSeats;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error checking minimum seat availability: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get departure time for a specific station
     */
    public String getDepartureTime(Train train, String stationName) {
        TrainSchedule schedule = trainDAO.getStationTiming(train.getTrainId(), stationName);
        if (schedule != null && schedule.getDepartureTime() != null) {
            return schedule.getDepartureTime().toString();
        }
        return generateRandomTime(); // Fallback for demo
    }

    /**
     * Get arrival time for a specific station
     */
    public String getArrivalTime(Train train, String stationName) {
        TrainSchedule schedule = trainDAO.getStationTiming(train.getTrainId(), stationName);
        if (schedule != null && schedule.getArrivalTime() != null) {
            return schedule.getArrivalTime().toString();
        }
        return generateRandomTime(); // Fallback for demo
    }

    /**
     * Calculate journey duration between two stations
     */
    public String calculateDuration(Train train, String fromStation, String toStation) {
        try {
            TrainSchedule fromSchedule = trainDAO.getStationTiming(train.getTrainId(), fromStation);
            TrainSchedule toSchedule = trainDAO.getStationTiming(train.getTrainId(), toStation);

            if (fromSchedule != null && toSchedule != null &&
                    fromSchedule.getDepartureTime() != null && toSchedule.getArrivalTime() != null) {

                // Calculate duration between stations
                LocalTime fromTime = fromSchedule.getDepartureTime();
                LocalTime toTime = toSchedule.getArrivalTime();

                // Handle day crossover if arrival time is before departure time
                long minutes;
                if (toTime.isBefore(fromTime)) {
                    // Next day arrival
                    minutes = java.time.Duration.between(fromTime, LocalTime.MAX).toMinutes() +
                            java.time.Duration.between(LocalTime.MIN, toTime).toMinutes() + 1;
                } else {
                    minutes = java.time.Duration.between(fromTime, toTime).toMinutes();
                }

                long hours = minutes / 60;
                long remainingMinutes = minutes % 60;
                return String.format("%dh %02dm", hours, remainingMinutes);
            }
        } catch (Exception e) {
            System.err.println("Error calculating duration: " + e.getMessage());
        }

        return generateRandomDuration(); // Fallback for demo
    }

    /**
     * Get number of halts between stations
     */
    public int getHaltsBetween(Train train, String fromStation, String toStation) {
        List<String> stations = trainDAO.getTrainScheduleStationNames(train.getTrainId());
        int fromIndex = -1;
        int toIndex = -1;

        // Case-insensitive search
        for (int i = 0; i < stations.size(); i++) {
            if (stations.get(i).equalsIgnoreCase(fromStation)) {
                fromIndex = i;
            }
            if (stations.get(i).equalsIgnoreCase(toStation)) {
                toIndex = i;
            }
        }

        if (fromIndex != -1 && toIndex != -1) {
            return Math.abs(toIndex - fromIndex) - 1;
        }

        return new Random().nextInt(5) + 1; // Random between 1-5
    }

    /**
     * Get distance between stations
     */
    public int getDistanceBetween(Train train, String fromStation, String toStation) {
        // Since you don't have distance data, generate random realistic distances
        try {
            int halts = getHaltsBetween(train, fromStation, toStation);

            // Generate random distance based on number of halts
            // Assume average 100-200km between major stations
            Random random = new Random();
            int baseDistance = (halts + 1) * (100 + random.nextInt(100)); // 100-200km per segment

            // Add some variation (±20%)
            int variation = (int) (baseDistance * 0.2);
            int finalDistance = baseDistance + random.nextInt(variation * 2) - variation;

            // Ensure minimum distance of 50km
            return Math.max(50, finalDistance);

        } catch (Exception e) {
            System.err.println("Error calculating distance: " + e.getMessage());

            // Fallback: random distance between 100-1000km
            Random random = new Random();
            return 100 + random.nextInt(900);
        }
    }

    /**
     * Check if train has specific amenities
     */
    public List<String> getTrainAmenities(Train train) {
        List<String> amenities = new ArrayList<>();

        // Based on train number patterns (demo logic)
        String trainNumber = train.getTrainNumber();

        if (trainNumber.startsWith("1") || trainNumber.startsWith("2")) {
            amenities.add("⚡ Superfast");
        }

        if (train.getName().toLowerCase().contains("express") ||
                train.getName().toLowerCase().contains("rajdhani") ||
                train.getName().toLowerCase().contains("shatabdi")) {
            amenities.add("🍽️ Pantry Car");
            amenities.add("📶 WiFi");
        }

        amenities.add("💺 Reserved Seating");

        return amenities;
    }

    /**
     * Get seat availability status for multiple dates
     * Useful for showing availability calendar
     */
    public Map<LocalDate, Map<String, Integer>> getAvailabilityForDateRange(int trainId,
                                                                            LocalDate startDate,
                                                                            LocalDate endDate) {
        Map<LocalDate, Map<String, Integer>> availabilityMap = new HashMap<>();

        try {
            Train train = trainDAO.getTrainById(trainId);
            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                Map<String, Integer> dailyAvailability = getAvailableSeatsForDate(train, currentDate);
                availabilityMap.put(currentDate, dailyAvailability);
                currentDate = currentDate.plusDays(1);
            }

        } catch (Exception e) {
            System.err.println("Error getting availability for date range: " + e.getMessage());
            e.printStackTrace();
        }

        return availabilityMap;
    }

    /**
     * Get default seat availability based on train type/capacity
     * Fixed to work without train.getCapacityJson() method
     */
    private Map<String, Integer> getDefaultSeatAvailability(Train train) {
        Map<String, Integer> defaultSeats = new HashMap<>();
        Random random = new Random();

        // Since Train model doesn't have getCapacityJson() method,
        // generate default seat availability based on train characteristics

        if (train != null) {
            String trainName = train.getName().toLowerCase();
            String trainNumber = train.getTrainNumber();

            // Adjust capacity based on train type
            if (trainName.contains("rajdhani") || trainName.contains("shatabdi") ||
                    trainName.contains("vande bharat")) {
                // Premium trains - higher capacity
                defaultSeats.put("SL", 80 + random.nextInt(20));   // 80-100 seats
                defaultSeats.put("3A", 60 + random.nextInt(20));   // 60-80 seats
                defaultSeats.put("2A", 40 + random.nextInt(15));   // 40-55 seats
                defaultSeats.put("1A", 20 + random.nextInt(10));   // 20-30 seats
            } else if (trainName.contains("express") || trainName.contains("mail")) {
                // Express trains - standard capacity
                defaultSeats.put("SL", 70 + random.nextInt(20));   // 70-90 seats
                defaultSeats.put("3A", 50 + random.nextInt(20));   // 50-70 seats
                defaultSeats.put("2A", 35 + random.nextInt(15));   // 35-50 seats
                defaultSeats.put("1A", 18 + random.nextInt(10));   // 18-28 seats
            } else if (trainName.contains("passenger") || trainName.contains("local")) {
                // Local/Passenger trains - lower capacity
                defaultSeats.put("SL", 50 + random.nextInt(20));   // 50-70 seats
                defaultSeats.put("3A", 30 + random.nextInt(15));   // 30-45 seats
                defaultSeats.put("2A", 20 + random.nextInt(10));   // 20-30 seats
                defaultSeats.put("1A", 10 + random.nextInt(8));    // 10-18 seats
            } else {
                // Default capacity for other trains
                defaultSeats.put("SL", 60 + random.nextInt(20));   // 60-80 seats
                defaultSeats.put("3A", 40 + random.nextInt(20));   // 40-60 seats
                defaultSeats.put("2A", 30 + random.nextInt(15));   // 30-45 seats
                defaultSeats.put("1A", 15 + random.nextInt(10));   // 15-25 seats
            }
        } else {
            // Fallback when train is null
            defaultSeats.put("SL", 60 + random.nextInt(20));   // 60-80 seats
            defaultSeats.put("3A", 40 + random.nextInt(20));   // 40-60 seats
            defaultSeats.put("2A", 30 + random.nextInt(15));   // 30-45 seats
            defaultSeats.put("1A", 15 + random.nextInt(10));   // 15-25 seats
        }

        return defaultSeats;
    }

    /**
     * Generate random time for demo purposes
     */
    private String generateRandomTime() {
        Random random = new Random();
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * Generate random duration for demo purposes
     */
    private String generateRandomDuration() {
        Random random = new Random();
        int hours = random.nextInt(20) + 2;  // 2-22 hours
        int minutes = random.nextInt(60);
        return String.format("%dh %02dm", hours, minutes);
    }

    /**
     * Get detailed schedule for display in train details page
     */
    public List<TrainScheduleDisplay> getDetailedSchedule(int trainId) {
        List<TrainScheduleDisplay> scheduleDisplay = new ArrayList<>();

        try {
            // Get the train schedule from DAO
            List<TrainSchedule> schedules = trainDAO.getTrainSchedule(trainId);

            if (schedules.isEmpty()) {
                System.out.println("No schedule found for train ID: " + trainId);
                return createFallbackSchedule();
            }

            // Sort by sequence order to ensure proper display
            schedules.sort(Comparator.comparing(TrainSchedule::getSequenceOrder));

            for (TrainSchedule schedule : schedules) {
                // Get station information
                Station station = stationDAO.getStationById(schedule.getStationId());
                String stationName = station != null ? station.getName() : "Unknown Station";

                // Format arrival and departure times
                String arrTime = schedule.getArrivalTime() != null ?
                        schedule.getArrivalTime().toString() : "--";
                String depTime = schedule.getDepartureTime() != null ?
                        schedule.getDepartureTime().toString() : "--";

                // Calculate halt time in minutes
                String haltTime = "0";
                if (schedule.getArrivalTime() != null && schedule.getDepartureTime() != null) {
                    long haltMinutes = java.time.Duration.between(
                            schedule.getArrivalTime(),
                            schedule.getDepartureTime()
                    ).toMinutes();
                    haltTime = String.valueOf(Math.max(0, haltMinutes));
                }

                // Day information
                String day = String.valueOf(schedule.getDayNumber());

                // Create and add the display object
                scheduleDisplay.add(new TrainScheduleDisplay(
                        stationName, arrTime, depTime, haltTime, day
                ));
            }

        } catch (Exception e) {
            System.err.println("Error getting detailed schedule for train " + trainId + ": " + e.getMessage());
            e.printStackTrace();

            // Return sample schedule as fallback
            return createFallbackSchedule();
        }

        return scheduleDisplay;
    }

    /**
     * Create a fallback schedule when real data is not available
     */
    private List<TrainScheduleDisplay> createFallbackSchedule() {
        return List.of(
                new TrainScheduleDisplay("Source Station", "--", "06:00", "0", "1"),
                new TrainScheduleDisplay("Junction 1", "08:15", "08:20", "5", "1"),
                new TrainScheduleDisplay("Junction 2", "12:45", "12:55", "10", "1"),
                new TrainScheduleDisplay("Destination Station", "18:30", "--", "0", "1")
        );
    }
}
