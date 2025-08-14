package trainapp.dao;

import trainapp.model.Journey;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JourneyDAO {
    /**
     * Get the next journey of a train on or after the given date.
     */
    public Journey getNextJourneyForTrain(int trainId, LocalDate fromDate) {
        String sql = """
            SELECT journey_id, train_id, departure_date, available_seats
            FROM journeys
            WHERE train_id = ? AND departure_date >= ?
            ORDER BY departure_date ASC
            LIMIT 1
            """;

        // ✅ FIXED: Use try-with-resources for automatic connection closing
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            stmt.setDate(2, Date.valueOf(fromDate));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Journey journey = new Journey();
                    journey.setJourneyId(rs.getLong("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureDate(rs.getDate("departure_date").toLocalDate());
                    journey.setAvailableSeatsJson(rs.getString("available_seats"));
                    return journey;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting next journey: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get the journey of a train on the exact date.
     */
    public Journey getJourneyForTrainAndDate(int trainId, LocalDate date) {
        String sql = """
            SELECT journey_id, train_id, departure_date, available_seats
            FROM journeys
            WHERE train_id = ? AND departure_date = ?
            """;

        // ✅ FIXED: Use try-with-resources for automatic connection closing
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Journey journey = new Journey();
                    journey.setJourneyId(rs.getLong("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureDate(rs.getDate("departure_date").toLocalDate());
                    journey.setAvailableSeatsJson(rs.getString("available_seats"));
                    return journey;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting journey for date: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Fetch journey by journey_id
     */
    public Journey getJourneyById(long journeyId) {
        String sql = """
            SELECT journey_id, train_id, departure_date, available_seats
            FROM journeys
            WHERE journey_id = ?
            """;

        // ✅ FIXED: Use try-with-resources for automatic connection closing
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, journeyId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Journey journey = new Journey();
                    journey.setJourneyId(rs.getLong("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureDate(rs.getDate("departure_date").toLocalDate());
                    journey.setAvailableSeatsJson(rs.getString("available_seats"));
                    return journey;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting journey by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Update available seats for a journey
     */
    public boolean updateAvailableSeats(long journeyId, String availableSeatsJson) {
        String sql = "UPDATE journeys SET available_seats = ? WHERE journey_id = ?";

        // ✅ FIXED: Use try-with-resources for automatic connection closing
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, availableSeatsJson);
            stmt.setLong(2, journeyId);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("Error updating seats: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create a new journey
     */
    public long createJourney(int trainId, LocalDate departureDate, String availableSeatsJson) {
        String sql = """
        INSERT INTO journeys (train_id, departure_date, available_seats)
        VALUES (?, ?, ?)
        """;

        // ✅ FIXED: Use try-with-resources for automatic connection closing
        // Added RETURN_GENERATED_KEYS to get the auto-generated journey ID
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, trainId);
            stmt.setDate(2, Date.valueOf(departureDate));
            stmt.setString(3, availableSeatsJson);

            int rowsInserted = stmt.executeUpdate();

            if (rowsInserted > 0) {
                // Get the generated journey ID
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }

            return -1; // Return -1 if insertion failed or no ID was generated

        } catch (SQLException e) {
            System.err.println("Error creating journey: " + e.getMessage());
            e.printStackTrace();
            return -1; // Return -1 on error
        }
    }

    /**
     * Get all journeys for a specific date range
     */
    public List<Journey> getJourneysInDateRange(LocalDate startDate, LocalDate endDate) {
        List<Journey> journeys = new ArrayList<>();

        String sql = """
            SELECT journey_id, train_id, departure_date, available_seats
            FROM journeys
            WHERE departure_date BETWEEN ? AND ?
            ORDER BY departure_date, train_id
            """;

        // ✅ FIXED: Use try-with-resources for automatic connection closing
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Journey journey = new Journey();
                    journey.setJourneyId(rs.getLong("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureDate(rs.getDate("departure_date").toLocalDate());
                    journey.setAvailableSeatsJson(rs.getString("available_seats"));
                    journeys.add(journey);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting journeys in date range: " + e.getMessage());
            e.printStackTrace();
        }

        return journeys;
    }

    // ✅ NEW: Additional CRUD methods for Fleet Management integration

    /**
     * Get all journeys for a specific train
     */
    public List<Journey> getAllJourneysForTrain(int trainId) {
        List<Journey> journeys = new ArrayList<>();

        String sql = """
            SELECT journey_id, train_id, departure_date, available_seats
            FROM journeys
            WHERE train_id = ?
            ORDER BY departure_date DESC
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Journey journey = new Journey();
                    journey.setJourneyId(rs.getLong("journey_id"));
                    journey.setTrainId(rs.getInt("train_id"));
                    journey.setDepartureDate(rs.getDate("departure_date").toLocalDate());
                    journey.setAvailableSeatsJson(rs.getString("available_seats"));
                    journeys.add(journey);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting journeys for train: " + e.getMessage());
            e.printStackTrace();
        }

        return journeys;
    }

    /**
     * Delete a specific journey
     */
    public boolean deleteJourney(long journeyId) {
        String sql = "DELETE FROM journeys WHERE journey_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, journeyId);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Successfully deleted journey with ID: " + journeyId);
                return true;
            } else {
                System.err.println("No journey found with ID: " + journeyId);
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting journey: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete all journeys for a specific train (used when deleting train)
     */
    public boolean deleteAllJourneysForTrain(int trainId) {
        String sql = "DELETE FROM journeys WHERE train_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            int rowsDeleted = stmt.executeUpdate();

            System.out.println("Deleted " + rowsDeleted + " journeys for train ID: " + trainId);
            return true; // Even 0 deletions is successful

        } catch (SQLException e) {
            System.err.println("Error deleting journeys for train: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update journey departure date
     */
    public boolean updateJourneyDate(long journeyId, LocalDate newDate) {
        String sql = "UPDATE journeys SET departure_date = ? WHERE journey_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(newDate));
            stmt.setLong(2, journeyId);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("Error updating journey date: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if journey exists for train and date
     */
    public boolean journeyExists(int trainId, LocalDate date) {
        String sql = "SELECT COUNT(*) FROM journeys WHERE train_id = ? AND departure_date = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking journey existence: " + e.getMessage());
        }

        return false;
    }

    /**
     * Get journey count for a specific train
     */
    public int getJourneyCountForTrain(int trainId) {
        String sql = "SELECT COUNT(*) FROM journeys WHERE train_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, trainId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting journey count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get total journey count
     */
    public int getTotalJourneyCount() {
        String sql = "SELECT COUNT(*) FROM journeys";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total journey count: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get upcoming journeys (future dates only)
     */
    public List<Journey> getUpcomingJourneys() {
        List<Journey> journeys = new ArrayList<>();

        String sql = """
            SELECT journey_id, train_id, departure_date, available_seats
            FROM journeys
            WHERE departure_date >= CURDATE()
            ORDER BY departure_date ASC
            LIMIT 50
            """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Journey journey = new Journey();
                journey.setJourneyId(rs.getLong("journey_id"));
                journey.setTrainId(rs.getInt("train_id"));
                journey.setDepartureDate(rs.getDate("departure_date").toLocalDate());
                journey.setAvailableSeatsJson(rs.getString("available_seats"));
                journeys.add(journey);
            }
        } catch (SQLException e) {
            System.err.println("Error getting upcoming journeys: " + e.getMessage());
            e.printStackTrace();
        }

        return journeys;
    }

    /**
     * Bulk create journeys for a train across multiple dates
     */
    public boolean createJourneysForDateRange(int trainId, LocalDate startDate, LocalDate endDate, String defaultSeatsJson) {
        String sql = """
            INSERT INTO journeys (train_id, departure_date, available_seats)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                LocalDate currentDate = startDate;
                int batchCount = 0;

                while (!currentDate.isAfter(endDate)) {
                    // Skip if journey already exists
                    if (!journeyExists(trainId, currentDate)) {
                        stmt.setInt(1, trainId);
                        stmt.setDate(2, Date.valueOf(currentDate));
                        stmt.setString(3, defaultSeatsJson);
                        stmt.addBatch();
                        batchCount++;

                        // Execute batch every 100 entries for performance
                        if (batchCount % 100 == 0) {
                            stmt.executeBatch();
                        }
                    }
                    currentDate = currentDate.plusDays(1);
                }

                // Execute remaining batch
                if (batchCount % 100 != 0) {
                    stmt.executeBatch();
                }

                conn.commit();
                System.out.println("Created " + batchCount + " journeys for train " + trainId);
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Error bulk creating journeys: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}