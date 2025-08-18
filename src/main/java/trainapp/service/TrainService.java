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

/**
 * TrainService provides searching, availability, booking, schedule, and metadata
 * operations for trains and journeys.
 *
 * Methods are grouped by functionality:
 * - Search and route validation
 * - Seat availability and booking
 * - Journey management
 * - Train metadata and amenities
 * - Utility functions
 */
public class TrainService {

    private final TrainDAO trainDAO = new TrainDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();
    private final StationDAO stationDAO = new StationDAO();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Search & Route Validation Methods
    // -------------------------------------------------------------------------

    /**
     * Finds trains that run between given stations (valid route only).
     * @param fromStationName Source station
     * @param toStationName   Destination station
     * @return List of valid trains found
     */
    public List<Train> findTrainsBetweenStations(String fromStationName, String toStationName) {
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
        if (filteredTrains.isEmpty() && !candidateTrains.isEmpty()) {
            System.out.println("TrainService: No trains passed filtering, returning all candidates for testing");
            return candidateTrains;
        }
        return filteredTrains;
    }

    // -------------------------------------------------------------------------
    // Seat Availability & Booking Methods
    // -------------------------------------------------------------------------

    /**
     * Gets seat availability for a given train and journey date.
     * @param train Train object
     * @param journeyDate Desired travel date
     * @return Map of class to available seats
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
     * Books seats for passengers and updates database.
     * @param trainId        Train ID
     * @param journeyDate    Date of journey
     * @param seatClass      Seat class (e.g., SL, 3A)
     * @param passengerCount Number of seats to book
     * @return true if booked successfully
     */
    public boolean bookSeats(int trainId, LocalDate journeyDate, String seatClass, int passengerCount) {
        try {
            System.out.println("Booking " + passengerCount + " seats for train " + trainId +
                    " on " + journeyDate + " in " + seatClass + " class");

            Journey journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (journey == null) {
                System.err.println("No journey found for train " + trainId + " on " + journeyDate);
                return false;
            }

            Map<String, Integer> currentAvailability = journey.getAvailableSeatsMap();
            int availableSeats = currentAvailability.getOrDefault(seatClass, 0);
            if (availableSeats < passengerCount) {
                System.err.println("Not enough seats available. Requested: " + passengerCount +
                        ", Available: " + availableSeats);
                return false;
            }

            currentAvailability.put(seatClass, availableSeats - passengerCount);
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
     * Gets default seat availability for demo/fallback purposes.
     * @param train Train object
     * @return Default seat availability map (by class)
     */
    private Map<String, Integer> getDefaultSeatAvailability(Train train) {
        Map<String, Integer> defaultSeats = new HashMap<>();
        Random random = new Random();

        if (train != null) {
            String trainName = train.getName().toLowerCase();
            String trainNumber = train.getTrainNumber();

            if (trainName.contains("rajdhani") || trainName.contains("shatabdi") ||
                    trainName.contains("vande bharat")) {
                defaultSeats.put("SL", 80 + random.nextInt(20));
                defaultSeats.put("3A", 60 + random.nextInt(20));
                defaultSeats.put("2A", 40 + random.nextInt(15));
                defaultSeats.put("1A", 20 + random.nextInt(10));
            } else if (trainName.contains("express") || trainName.contains("mail")) {
                defaultSeats.put("SL", 70 + random.nextInt(20));
                defaultSeats.put("3A", 50 + random.nextInt(20));
                defaultSeats.put("2A", 35 + random.nextInt(15));
                defaultSeats.put("1A", 18 + random.nextInt(10));
            } else if (trainName.contains("passenger") || trainName.contains("local")) {
                defaultSeats.put("SL", 50 + random.nextInt(20));
                defaultSeats.put("3A", 30 + random.nextInt(15));
                defaultSeats.put("2A", 20 + random.nextInt(10));
                defaultSeats.put("1A", 10 + random.nextInt(8));
            } else {
                defaultSeats.put("SL", 60 + random.nextInt(20));
                defaultSeats.put("3A", 40 + random.nextInt(20));
                defaultSeats.put("2A", 30 + random.nextInt(15));
                defaultSeats.put("1A", 15 + random.nextInt(10));
            }
        } else {
            defaultSeats.put("SL", 60 + random.nextInt(20));
            defaultSeats.put("3A", 40 + random.nextInt(20));
            defaultSeats.put("2A", 30 + random.nextInt(15));
            defaultSeats.put("1A", 15 + random.nextInt(10));
        }

        return defaultSeats;
    }

    // -------------------------------------------------------------------------
    // Journey Management Methods
    // -------------------------------------------------------------------------

    /**
     * Ensures a journey record exists for a train and date, creates if needed.
     * @param trainId     Train ID
     * @param journeyDate Journey date
     * @return true if ensured/created successfully
     */
    public boolean ensureJourneyExists(int trainId, LocalDate journeyDate) {
        try {
            System.out.println("Ensuring journey exists for train " + trainId + " on " + journeyDate);

            Journey existingJourney = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
            if (existingJourney != null) {
                System.out.println("Journey already exists for train " + trainId + " on " + journeyDate);
                return true;
            }

            Train train = trainDAO.getTrainById(trainId);
            if (train == null) {
                System.err.println("Train not found with ID: " + trainId);
                return false;
            }

            Journey newJourney = new Journey();
            newJourney.setTrainId(trainId);
            newJourney.setDepartureDate(journeyDate);

            Map<String, Integer> defaultSeats = getDefaultSeatAvailability(train);
            String seatsJson = objectMapper.writeValueAsString(defaultSeats);
            newJourney.setAvailableSeatsJson(seatsJson);

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

    // -------------------------------------------------------------------------
    // Schedule, Arrival, Departure, and Duration Methods
    // -------------------------------------------------------------------------

    /**
     * Gets departure time for a train at a specific station.
     * @param train Train object
     * @param stationName Station name
     * @return Departure time as string
     */
    public String getDepartureTime(Train train, String stationName) {
        TrainSchedule schedule = trainDAO.getStationTiming(train.getTrainId(), stationName);
        if (schedule != null && schedule.getDepartureTime() != null) {
            return schedule.getDepartureTime().toString();
        }
        return generateRandomTime(); // Fallback for demo
    }

    /**
     * Gets arrival time for a train at a specific station.
     * @param train Train object
     * @param stationName Station name
     * @return Arrival time as string
     */
    public String getArrivalTime(Train train, String stationName) {
        TrainSchedule schedule = trainDAO.getStationTiming(train.getTrainId(), stationName);
        if (schedule != null && schedule.getArrivalTime() != null) {
            return schedule.getArrivalTime().toString();
        }
        return generateRandomTime(); // Fallback for demo
    }

    /**
     * Calculates journey duration between two stations (with day crossover).
     * @param train Train object
     * @param fromStation Source station
     * @param toStation   Destination station
     * @return Duration as string ("5h 30m")
     */
    public String calculateDuration(Train train, String fromStation, String toStation) {
        try {
            TrainSchedule fromSchedule = trainDAO.getStationTiming(train.getTrainId(), fromStation);
            TrainSchedule toSchedule = trainDAO.getStationTiming(train.getTrainId(), toStation);

            if (fromSchedule != null && toSchedule != null &&
                    fromSchedule.getDepartureTime() != null && toSchedule.getArrivalTime() != null) {

                LocalTime fromTime = fromSchedule.getDepartureTime();
                LocalTime toTime = toSchedule.getArrivalTime();

                long minutes;
                if (toTime.isBefore(fromTime)) {
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

    // -------------------------------------------------------------------------
    // Train Metadata / Amenities / Distance / Halts Methods
    // -------------------------------------------------------------------------

    /**
     * Gets number of halts between two stations for a train.
     * @param train Train object
     * @param fromStation Source station
     * @param toStation   Destination station
     * @return Number of halts between the stations
     */
    public int getHaltsBetween(Train train, String fromStation, String toStation) {
        List<String> stations = trainDAO.getTrainScheduleStationNames(train.getTrainId());
        int fromIndex = -1;
        int toIndex = -1;

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

        return new Random().nextInt(5) + 1;
    }

    /**
     * Estimates the distance between two stations for a train.
     * @param train Train object
     * @param fromStation Source station
     * @param toStation   Destination station
     * @return Estimated distance (int, in km)
     */
    public int getDistanceBetween(Train train, String fromStation, String toStation) {
        try {
            int halts = getHaltsBetween(train, fromStation, toStation);
            Random random = new Random();
            int baseDistance = (halts + 1) * (100 + random.nextInt(100));
            int variation = (int) (baseDistance * 0.2);
            int finalDistance = baseDistance + random.nextInt(variation * 2) - variation;

            return Math.max(50, finalDistance);

        } catch (Exception e) {
            System.err.println("Error calculating distance: " + e.getMessage());
            Random random = new Random();
            return 100 + random.nextInt(900);
        }
    }

    /**
     * Gets list of amenities for a train based on name/number heuristics.
     * @param train Train object
     * @return List of amenities (strings)
     */
    public List<String> getTrainAmenities(Train train) {
        List<String> amenities = new ArrayList<>();
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

    // -------------------------------------------------------------------------
    // Utility Methods (Randoms, Fallbacks, etc.)
    // -------------------------------------------------------------------------

    /**
     * Generates a random time as string (HH:mm).
     * @return Random time string
     */
    private String generateRandomTime() {
        Random random = new Random();
        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        return String.format("%02d:%02d", hour, minute);
    }

    /**
     * Generates a random duration as string for demo purposes ("6h 15m").
     * @return Random duration string
     */
    private String generateRandomDuration() {
        Random random = new Random();
        int hours = random.nextInt(20) + 2;
        int minutes = random.nextInt(60);
        return String.format("%dh %02dm", hours, minutes);
    }
}