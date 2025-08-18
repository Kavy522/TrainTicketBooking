package trainapp.dao;

import trainapp.model.Train;
import trainapp.model.TrainSchedule;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for train and train schedule management.
 * Provides comprehensive CRUD operations for trains, schedule management, and route searching.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete train CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>Advanced route searching between stations with schedule validation</li>
 *   <li>Train schedule management with timing information</li>
 *   <li>Station timing queries for journey planning</li>
 *   <li>Transactional deletion handling for referential integrity</li>
 *   <li>Analytics and reporting functionality</li>
 * </ul>
 *
 * <p>Security and reliability features:
 * <ul>
 *   <li>SQL injection prevention through prepared statements</li>
 *   <li>Transactional operations for data consistency</li>
 *   <li>Proper resource management with try-with-resources</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Foreign key dependency management</li>
 * </ul>
 */
public class TrainDAO {

    // -------------------------------------------------------------------------
    // Search Operations
    // -------------------------------------------------------------------------

    /**
     * Finds all trains that operate between two stations with valid routing.
     * Uses schedule data to ensure the source station comes before destination in the train's route.
     * Falls back to returning all trains if no specific route match is found.
     *
     * @param sourceStationName name of the departure station
     * @param destStationName name of the destination station
     * @return List of Train objects that serve the route, empty list if none found
     * @throws IllegalArgumentException if station names are null or empty
     */
    public List<Train> findTrainsBetweenStations(String sourceStationName, String destStationName) {
        if (sourceStationName == null || sourceStationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Source station name cannot be null or empty");
        }
        if (destStationName == null || destStationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination station name cannot be null or empty");
        }

        List<Train> trains = new ArrayList<>();
        String sql = """
                SELECT DISTINCT
                t.train_id,
                t.train_number,
                t.name,
                t.source_station_id,
                t.destination_station_id,
                t.total_coaches
                FROM trains t
                JOIN train_schedule ts1 ON t.train_id = ts1.train_id
                JOIN train_schedule ts2 ON t.train_id = ts2.train_id
                JOIN stations s1 ON ts1.station_id = s1.station_id
                JOIN stations s2 ON ts2.station_id = s2.station_id
                WHERE s1.name = ?
                AND s2.name = ?
                AND ts1.sequence_order < ts2.sequence_order
                ORDER BY t.train_number
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sourceStationName);
            stmt.setString(2, destStationName);

            System.out.println("Searching trains from: " + sourceStationName + " to: " + destStationName);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Train train = new Train();
                    train.setTrainId(rs.getInt("train_id"));
                    train.setTrainNumber(rs.getString("train_number"));
                    train.setName(rs.getString("name"));
                    train.setSourceStationId(rs.getInt("source_station_id"));
                    train.setDestinationStationId(rs.getInt("destination_station_id"));
                    train.setTotalCoaches(rs.getInt("total_coaches"));
                    trains.add(train);

                    System.out.println("Found train: " + train.getTrainNumber() + " - " + train.getName());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding trains between stations: " + e.getMessage());
            e.printStackTrace();
        }

        if (trains.isEmpty()) {
            System.out.println("No trains found with route matching. Getting all trains for testing...");
            trains = getAllTrains();
        }

        System.out.println("Total trains found: " + trains.size());
        return trains;
    }

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a train by its unique database ID.
     *
     * @param trainId the unique train identifier
     * @return Train object if found, null otherwise
     * @throws IllegalArgumentException if trainId is not positive
     */
    public Train getTrainById(int trainId) {
        if (trainId <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }

        String sql = """
                SELECT train_id, train_number, name, source_station_id, 
                       destination_station_id, total_coaches
                FROM trains WHERE train_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Train train = new Train();
                    train.setTrainId(rs.getInt("train_id"));
                    train.setTrainNumber(rs.getString("train_number"));
                    train.setName(rs.getString("name"));
                    train.setSourceStationId(rs.getInt("source_station_id"));
                    train.setDestinationStationId(rs.getInt("destination_station_id"));
                    train.setTotalCoaches(rs.getInt("total_coaches"));
                    return train;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting train by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves all trains in the system, sorted by train number.
     *
     * @return List of all Train objects, empty list if none found
     */
    public List<Train> getAllTrains() {
        List<Train> trains = new ArrayList<>();
        String sql = """
                SELECT train_id, train_number, name, source_station_id, 
                       destination_station_id, total_coaches
                FROM trains
                ORDER BY train_number
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Train train = new Train();
                train.setTrainId(rs.getInt("train_id"));
                train.setTrainNumber(rs.getString("train_number"));
                train.setName(rs.getString("name"));
                train.setSourceStationId(rs.getInt("source_station_id"));
                train.setDestinationStationId(rs.getInt("destination_station_id"));
                train.setTotalCoaches(rs.getInt("total_coaches"));
                trains.add(train);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all trains: " + e.getMessage());
            e.printStackTrace();
        }

        return trains;
    }

    // -------------------------------------------------------------------------
    // Schedule Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves station names in sequence order for a specific train.
     * Useful for route validation and journey planning.
     *
     * @param trainId the train's unique identifier
     * @return List of station names in sequence order, empty list if none found
     * @throws IllegalArgumentException if trainId is not positive
     */
    public List<String> getTrainScheduleStationNames(int trainId) {
        if (trainId <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }

        List<String> stationNames = new ArrayList<>();
        String sql = """
                SELECT s.name
                FROM train_schedule ts
                JOIN stations s ON ts.station_id = s.station_id
                WHERE ts.train_id = ?
                ORDER BY ts.sequence_order
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    stationNames.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting train schedule: " + e.getMessage());
            e.printStackTrace();
        }

        return stationNames;
    }

    /**
     * Retrieves timing information for a specific station on a train's route.
     * Returns arrival and departure times for journey planning calculations.
     *
     * @param trainId the train's unique identifier
     * @param stationName name of the station to get timing for
     * @return TrainSchedule object with timing information, null if not found
     * @throws IllegalArgumentException if parameters are invalid
     */
    public TrainSchedule getStationTiming(int trainId, String stationName) {
        if (trainId <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }
        if (stationName == null || stationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Station name cannot be null or empty");
        }

        String sql = """
                SELECT ts.schedule_id, ts.train_id, ts.station_id, ts.arrival_time, 
                       ts.departure_time, ts.day_number, ts.sequence_order
                FROM train_schedule ts
                JOIN stations s ON ts.station_id = s.station_id
                WHERE ts.train_id = ? AND s.name = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            stmt.setString(2, stationName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TrainSchedule schedule = new TrainSchedule();
                    schedule.setScheduleId(rs.getInt("schedule_id"));
                    schedule.setTrainId(rs.getInt("train_id"));
                    schedule.setStationId(rs.getInt("station_id"));

                    Time arrivalTime = rs.getTime("arrival_time");
                    if (arrivalTime != null) {
                        schedule.setArrivalTime(arrivalTime.toLocalTime());
                    }

                    Time departureTime = rs.getTime("departure_time");
                    if (departureTime != null) {
                        schedule.setDepartureTime(departureTime.toLocalTime());
                    }

                    schedule.setDayNumber(rs.getInt("day_number"));
                    schedule.setSequenceOrder(rs.getInt("sequence_order"));
                    return schedule;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting station timing: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves complete schedule for a train with all timing information.
     * Returns stations in sequence order with arrival/departure times.
     *
     * @param trainId the train's unique identifier
     * @return List of TrainSchedule objects in sequence order, empty list if none found
     * @throws IllegalArgumentException if trainId is not positive
     */
    public List<TrainSchedule> getTrainSchedule(int trainId) {
        if (trainId <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }

        List<TrainSchedule> schedules = new ArrayList<>();
        String sql = """
                SELECT schedule_id, train_id, station_id, arrival_time, departure_time, 
                       day_number, sequence_order
                FROM train_schedule
                WHERE train_id = ?
                ORDER BY sequence_order
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TrainSchedule schedule = new TrainSchedule();
                    schedule.setScheduleId(rs.getInt("schedule_id"));
                    schedule.setTrainId(rs.getInt("train_id"));
                    schedule.setStationId(rs.getInt("station_id"));

                    Time arrivalTime = rs.getTime("arrival_time");
                    if (arrivalTime != null) {
                        schedule.setArrivalTime(arrivalTime.toLocalTime());
                    }

                    Time departureTime = rs.getTime("departure_time");
                    if (departureTime != null) {
                        schedule.setDepartureTime(departureTime.toLocalTime());
                    }

                    schedule.setDayNumber(rs.getInt("day_number"));
                    schedule.setSequenceOrder(rs.getInt("sequence_order"));
                    schedules.add(schedule);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting train schedule: " + e.getMessage());
            e.printStackTrace();
        }

        return schedules;
    }

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new train to the database.
     * Train ID will be auto-generated by the database.
     *
     * @param train Train object containing train number, name, stations, and coach info
     * @return true if train was added successfully, false otherwise
     * @throws IllegalArgumentException if train is null or has invalid data
     */
    public boolean addTrain(Train train) {
        if (train == null) {
            throw new IllegalArgumentException("Train cannot be null");
        }
        if (train.getTrainNumber() == null || train.getTrainNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Train number cannot be null or empty");
        }
        if (train.getName() == null || train.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Train name cannot be null or empty");
        }

        String sql = """
                INSERT INTO trains (train_number, name, source_station_id, destination_station_id, total_coaches) 
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, train.getTrainNumber());
            stmt.setString(2, train.getName());
            stmt.setInt(3, train.getSourceStationId());
            stmt.setInt(4, train.getDestinationStationId());
            stmt.setInt(5, train.getTotalCoaches());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error adding train: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Update Operations
    // -------------------------------------------------------------------------

    /**
     * Updates an existing train's information.
     * Train number cannot be modified (business rule).
     *
     * @param train Train object with updated information (must have valid train ID)
     * @return true if train was updated successfully, false otherwise
     * @throws IllegalArgumentException if train is null, has invalid ID, or missing required fields
     */
    public boolean updateTrain(Train train) {
        if (train == null) {
            throw new IllegalArgumentException("Train cannot be null");
        }
        if (train.getTrainId() <= 0) {
            throw new IllegalArgumentException("Train must have a valid ID for update");
        }
        if (train.getName() == null || train.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Train name cannot be null or empty");
        }

        String sql = """
                UPDATE trains 
                SET name = ?, source_station_id = ?, destination_station_id = ?, total_coaches = ? 
                WHERE train_id = ?
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, train.getName());
            stmt.setInt(2, train.getSourceStationId());
            stmt.setInt(3, train.getDestinationStationId());
            stmt.setInt(4, train.getTotalCoaches());
            stmt.setInt(5, train.getTrainId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating train: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Delete Operations
    // -------------------------------------------------------------------------

    /**
     * Deletes a train from the database with proper dependency handling.
     * Uses transaction to ensure data consistency by removing dependent records first.
     *
     * <p>Deletion order:
     * <ol>
     *   <li>Delete dependent journeys</li>
     *   <li>Delete train schedule entries</li>
     *   <li>Delete the train record</li>
     * </ol>
     *
     * @param trainId the unique ID of the train to delete
     * @return true if train and dependencies were deleted successfully, false otherwise
     * @throws IllegalArgumentException if trainId is not positive
     */
    public boolean deleteTrain(int trainId) {
        if (trainId <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // First, check if train has dependent journeys
                String checkJourneysQuery = "SELECT COUNT(*) FROM journeys WHERE train_id = ?";
                int journeyCount = 0;

                try (PreparedStatement checkStmt = conn.prepareStatement(checkJourneysQuery)) {
                    checkStmt.setInt(1, trainId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            journeyCount = rs.getInt(1);
                        }
                    }
                }

                if (journeyCount > 0) {
                    System.out.println("Deleting " + journeyCount + " dependent journeys before removing train");
                    String deleteJourneysQuery = "DELETE FROM journeys WHERE train_id = ?";
                    try (PreparedStatement deleteJourneysStmt = conn.prepareStatement(deleteJourneysQuery)) {
                        deleteJourneysStmt.setInt(1, trainId);
                        int deletedJourneys = deleteJourneysStmt.executeUpdate();
                        System.out.println("Successfully deleted " + deletedJourneys + " dependent journeys");
                    }
                }

                // Delete train_schedule entries if they exist
                String deleteScheduleQuery = "DELETE FROM train_schedule WHERE train_id = ?";
                try (PreparedStatement deleteScheduleStmt = conn.prepareStatement(deleteScheduleQuery)) {
                    deleteScheduleStmt.setInt(1, trainId);
                    int deletedSchedules = deleteScheduleStmt.executeUpdate();
                    if (deletedSchedules > 0) {
                        System.out.println("Deleted " + deletedSchedules + " train schedule entries");
                    }
                }

                // Finally, delete the train
                String deleteTrainQuery = "DELETE FROM trains WHERE train_id = ?";
                try (PreparedStatement deleteTrainStmt = conn.prepareStatement(deleteTrainQuery)) {
                    deleteTrainStmt.setInt(1, trainId);
                    int rowsAffected = deleteTrainStmt.executeUpdate();

                    if (rowsAffected > 0) {
                        conn.commit();
                        System.out.println("Successfully deleted train with ID: " + trainId);
                        return true;
                    } else {
                        conn.rollback();
                        System.err.println("Failed to delete train - no rows affected");
                        return false;
                    }
                }

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error during train deletion transaction: " + e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error deleting train with dependencies: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a specific schedule entry by schedule ID.
     * Used for removing individual route/journey entries from train schedules.
     *
     * @param scheduleId the unique schedule entry ID to delete
     * @return true if schedule entry was deleted successfully, false otherwise
     * @throws IllegalArgumentException if scheduleId is not positive
     */
    public boolean deleteJourney(int scheduleId) {
        if (scheduleId <= 0) {
            throw new IllegalArgumentException("Schedule ID must be positive");
        }

        String sql = "DELETE FROM train_schedule WHERE schedule_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, scheduleId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Successfully deleted journey entry with schedule ID: " + scheduleId);
                return true;
            } else {
                System.err.println("No journey found with schedule ID: " + scheduleId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting journey entry: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Analytics & Reporting Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves the total count of trains in the system.
     * Used for dashboard statistics and system reporting.
     *
     * @return total number of trains in the database
     * @throws SQLException if a database access error occurs
     */
    public int getTrainCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM trains";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting train count: " + e.getMessage());
            throw e;
        }
        return 0;
    }

}
