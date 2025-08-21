package trainapp.dao;

import trainapp.model.TrainSchedule;
import trainapp.util.DBConnection;
import java.sql.*;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Data Access Object (DAO) for train schedule management.
 * Provides advanced operations for managing train routes, station sequences, and timing information.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Add stations to train routes with automatic sequence management</li>
 *   <li>Update existing schedule entries with timing and sequence changes</li>
 *   <li>Intelligent sequence positioning and conflict resolution</li>
 *   <li>Route validation and duplicate station prevention</li>
 *   <li>Transactional operations for data consistency</li>
 * </ul>
 *
 * <p>Advanced sequence management:
 * <ul>
 *   <li>Automatic sequence number adjustment when inserting stations</li>
 *   <li>Validation to prevent duplicate stations in routes</li>
 *   <li>Dynamic sequence position calculation for UI dropdowns</li>
 *   <li>Conflict detection and resolution for existing sequences</li>
 * </ul>
 *
 * <p>Security and reliability features:
 * <ul>
 *   <li>Transactional operations with rollback capability</li>
 *   <li>SQL injection prevention through prepared statements</li>
 *   <li>Proper resource management with try-with-resources</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 */
public class TrainScheduleDAO {

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Adds a station to a train's route with intelligent sequence management.
     * Automatically shifts existing stations when inserting at a specific position.
     * Uses transaction to ensure data consistency during sequence adjustments.
     *
     * <p>Process:
     * <ol>
     *   <li>Shifts all existing stations with sequence >= target sequence down by 1</li>
     *   <li>Inserts the new station at the specified sequence position</li>
     *   <li>Commits transaction if successful, rollback if any step fails</li>
     * </ol>
     *
     * @param schedule TrainSchedule object with train ID, station ID, timing, and desired sequence
     * @return true if station was added successfully with sequence adjustment, false otherwise
     * @throws IllegalArgumentException if schedule is null or has invalid data
     */
    public boolean addStationToJourneyWithSequenceAdjustment(TrainSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (schedule.getTrainId() <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }
        if (schedule.getStationId() <= 0) {
            throw new IllegalArgumentException("Station ID must be positive");
        }
        if (schedule.getSequenceOrder() <= 0) {
            throw new IllegalArgumentException("Sequence order must be positive");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // First, shift all sequences that are >= the new sequence
                shiftSequencesDown(conn, schedule.getTrainId(), schedule.getSequenceOrder());

                // Then insert the new station
                String insertSql = """
                    INSERT INTO train_schedule (train_id, station_id, arrival_time, 
                                              departure_time, day_number, sequence_order) 
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                    stmt.setInt(1, schedule.getTrainId());
                    stmt.setInt(2, schedule.getStationId());

                    if (schedule.getArrivalTime() != null) {
                        stmt.setTime(3, Time.valueOf(schedule.getArrivalTime()));
                    } else {
                        stmt.setNull(3, Types.TIME);
                    }

                    if (schedule.getDepartureTime() != null) {
                        stmt.setTime(4, Time.valueOf(schedule.getDepartureTime()));
                    } else {
                        stmt.setNull(4, Types.TIME);
                    }

                    stmt.setInt(5, schedule.getDayNumber());
                    stmt.setInt(6, schedule.getSequenceOrder());

                    boolean success = stmt.executeUpdate() > 0;

                    if (success) {
                        conn.commit();
                        return true;
                    } else {
                        conn.rollback();
                        return false;
                    }
                }

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error adding station with sequence adjustment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a station to a train's route without sequence adjustment.
     * Simple insertion method that requires manual sequence management.
     * Use this when you're certain the sequence position is available.
     *
     * @param schedule TrainSchedule object with all required information
     * @return true if station was added successfully, false otherwise
     * @throws IllegalArgumentException if schedule is null or has invalid data
     */
    public boolean addStationToJourney(TrainSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (schedule.getTrainId() <= 0) {
            throw new IllegalArgumentException("Train ID must be positive");
        }
        if (schedule.getStationId() <= 0) {
            throw new IllegalArgumentException("Station ID must be positive");
        }
        if (schedule.getSequenceOrder() <= 0) {
            throw new IllegalArgumentException("Sequence order must be positive");
        }

        String sql = """
        INSERT INTO train_schedule (train_id, station_id, arrival_time, 
                                  departure_time, day_number, sequence_order) 
        VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, schedule.getTrainId());
            stmt.setInt(2, schedule.getStationId());

            if (schedule.getArrivalTime() != null) {
                stmt.setTime(3, Time.valueOf(schedule.getArrivalTime()));
            } else {
                stmt.setNull(3, Types.TIME);
            }

            if (schedule.getDepartureTime() != null) {
                stmt.setTime(4, Time.valueOf(schedule.getDepartureTime()));
            } else {
                stmt.setNull(4, Types.TIME);
            }

            stmt.setInt(5, schedule.getDayNumber());
            stmt.setInt(6, schedule.getSequenceOrder());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error adding station to journey: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Update Operations
    // -------------------------------------------------------------------------

    /**
     * Updates an existing station entry in a train's schedule.
     * Allows modification of station, timing, day number, and sequence position.
     *
     * @param schedule TrainSchedule object with updated information (must have valid schedule ID)
     * @return true if schedule entry was updated successfully, false otherwise
     * @throws IllegalArgumentException if schedule is null, has invalid ID, or missing required fields
     */
    public boolean updateStationInJourney(TrainSchedule schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (schedule.getScheduleId() <= 0) {
            throw new IllegalArgumentException("Schedule must have a valid ID for update");
        }
        if (schedule.getStationId() <= 0) {
            throw new IllegalArgumentException("Station ID must be positive");
        }
        if (schedule.getSequenceOrder() <= 0) {
            throw new IllegalArgumentException("Sequence order must be positive");
        }

        String sql = """
            UPDATE train_schedule 
            SET station_id = ?, arrival_time = ?, departure_time = ?, 
                day_number = ?, sequence_order = ?
            WHERE schedule_id = ?
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, schedule.getStationId());

            if (schedule.getArrivalTime() != null) {
                stmt.setTime(2, Time.valueOf(schedule.getArrivalTime()));
            } else {
                stmt.setNull(2, Types.TIME);
            }

            if (schedule.getDepartureTime() != null) {
                stmt.setTime(3, Time.valueOf(schedule.getDepartureTime()));
            } else {
                stmt.setNull(3, Types.TIME);
            }

            stmt.setInt(4, schedule.getDayNumber());
            stmt.setInt(5, schedule.getSequenceOrder());
            stmt.setInt(6, schedule.getScheduleId());

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                return true;
            } else {
                System.err.println("No route entry found with schedule ID: " + schedule.getScheduleId());
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error updating route entry: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Sequence Management Operations
    // -------------------------------------------------------------------------

    /**
     * Shifts all station sequences down by 1 for positions >= target sequence.
     * Internal helper method used during transactional insertions.
     * Orders by sequence DESC to avoid unique constraint violations.
     *
     * @param conn database connection (must be in transaction)
     * @param trainId train whose sequences to adjust
     * @param targetSequence sequence position where new station will be inserted
     * @throws SQLException if database operation fails
     */
    private void shiftSequencesDown(Connection conn, int trainId, int targetSequence) throws SQLException {
        String shiftSql = """
            UPDATE train_schedule 
            SET sequence_order = sequence_order + 1 
            WHERE train_id = ? AND sequence_order >= ?
            ORDER BY sequence_order DESC
            """;

        try (PreparedStatement stmt = conn.prepareStatement(shiftSql)) {
            stmt.setInt(1, trainId);
            stmt.setInt(2, targetSequence);

            int shiftedRows = stmt.executeUpdate();
        }
    }

    /**
     * Retrieves available sequence positions for a train route.
     * Useful for populating UI dropdowns when adding new stations.
     * Returns positions 1 through (current max + 1).
     *
     * @param trainId train ID to get available positions for
     * @return List of available sequence positions, always contains at least position 1
     */
    public List<Integer> getAvailableSequencePositions(int trainId) {
        List<Integer> positions = new ArrayList<>();

        try {
            int currentMaxSequence = getCurrentMaxSequence(trainId);

            // Add positions 1 through max+1 (can insert at end)
            for (int i = 1; i <= currentMaxSequence + 1; i++) {
                positions.add(i);
            }

            // If no stations exist, start with position 1
            if (positions.isEmpty()) {
                positions.add(1);
            }

        } catch (Exception e) {
            System.err.println("Error getting available positions: " + e.getMessage());
            positions.add(1); // Fallback
        }

        return positions;
    }

    /**
     * Retrieves the current maximum sequence number for a train's route.
     * Used for determining where new stations can be appended.
     *
     * @param trainId train ID to check
     * @return current maximum sequence number, 0 if no stations exist
     * @throws SQLException if database operation fails
     */
    public int getCurrentMaxSequence(int trainId) throws SQLException {
        String maxSql = "SELECT MAX(sequence_order) FROM train_schedule WHERE train_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(maxSql)) {

            stmt.setInt(1, trainId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Validation Operations
    // -------------------------------------------------------------------------

    /**
     * Checks if a specific sequence order already exists for a train.
     * Used to prevent duplicate sequence numbers during manual insertions.
     *
     * @param trainId train ID to check
     * @param sequenceOrder sequence position to validate
     * @return true if sequence already exists, false if available
     */
    public boolean sequenceExists(int trainId, int sequenceOrder) {
        String sql = "SELECT COUNT(*) FROM train_schedule WHERE train_id = ? AND sequence_order = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            stmt.setInt(2, sequenceOrder);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking sequence existence: " + e.getMessage());
        }

        return false;
    }

    /**
     * Checks if a station already exists in a train's route.
     * Prevents adding duplicate stations to the same train route.
     *
     * @param trainId train ID to check
     * @param stationId station ID to validate
     * @return true if station already exists in route, false if can be added
     */
    public boolean stationExistsInRoute(int trainId, int stationId) {
        String sql = "SELECT COUNT(*) FROM train_schedule WHERE train_id = ? AND station_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            stmt.setInt(2, stationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking station existence in route: " + e.getMessage());
        }

        return false;
    }
}
