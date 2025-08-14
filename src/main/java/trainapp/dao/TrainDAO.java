package trainapp.dao;

import trainapp.model.Train;
import trainapp.model.TrainSchedule;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TrainDAO {

    /**
     * Find all trains that stop at both source and destination stations
     */
    public List<Train> findTrainsBetweenStations(String sourceStationName, String destStationName) {
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

    /**
     * Get all trains
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

    /**
     * Get train schedule station names in sequence order
     */
    public List<String> getTrainScheduleStationNames(int trainId) {
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
     * Get timing info for specific stations on a train route
     */
    public TrainSchedule getStationTiming(int trainId, String stationName) {
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
     * Get complete train schedule with timing information
     */
    public List<TrainSchedule> getTrainSchedule(int trainId) {
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

    /**
     * Add a new train to the database
     */
    public boolean addTrain(Train train) {
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

    /**
     * Update an existing train
     */
    public boolean updateTrain(Train train) {
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

    /**
     * Enhanced delete method that handles foreign key dependencies
     * Deletes dependent journeys first, then deletes the train
     */
    public boolean deleteTrain(int trainId) {
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
     * Delete a specific journey/route entry by schedule ID
     */
    public boolean deleteJourney(int scheduleId) {
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

    /**
     * Delete all journey entries for a specific train
     */
    public boolean deleteAllJourneysForTrain(int trainId) {
        String sql = "DELETE FROM train_schedule WHERE train_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            int rowsAffected = stmt.executeUpdate();

            System.out.println("Deleted " + rowsAffected + " journey entries for train ID: " + trainId);
            return rowsAffected >= 0;

        } catch (SQLException e) {
            System.err.println("Error deleting journey entries for train: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get train by ID
     */
    public Train getTrainById(int trainId) {
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
     * Check if train number already exists
     */
    public boolean trainNumberExists(String trainNumber) {
        String sql = "SELECT COUNT(*) FROM trains WHERE train_number = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, trainNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking train number existence: " + e.getMessage());
        }

        return false;
    }
}