package trainapp.dao;

import trainapp.model.TrainSchedule;
import trainapp.util.DBConnection;
import java.sql.*;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;

public class TrainScheduleDAO {

    /**
     * Add station with automatic sequence adjustment
     */
    public boolean addStationToJourneyWithSequenceAdjustment(TrainSchedule schedule) {
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
                        System.out.println("Successfully added station at sequence " + schedule.getSequenceOrder() +
                                " and shifted down existing stations");
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
     * Shift all sequence numbers down by 1 for sequences >= targetSequence
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
            System.out.println("Shifted " + shiftedRows + " stations down by 1 sequence position");
        }
    }

    /**
     * Update an existing station in a train's journey
     */
    public boolean updateStationInJourney(TrainSchedule schedule) {
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
                System.out.println("Successfully updated route entry with schedule ID: " + schedule.getScheduleId());
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

    /**
     * Get available sequence positions for a train (for UI dropdown)
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
     * Get current maximum sequence for a train
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

    /**
     * Check if sequence order already exists for a train
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
     * Check if station already exists in train's route
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

    /**
     * Simple add station method without sequence adjustment (for basic adding)
     */
    public boolean addStationToJourney(TrainSchedule schedule) {
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

}
