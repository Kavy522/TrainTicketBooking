package trainapp.dao;

import trainapp.model.Journey;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for train journey management.
 * Provides methods to create, find, and update journeys by train, date, and ID.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Fetch journey by train & date or by ID</li>
 *   <li>Update available seat JSON for a journey</li>
 *   <li>Create new journey records with seats and departure date</li>
 *   <li>Ensures proper resource management via try-with-resources</li>
 * </ul>
 * <p>
 * All operations use prepared statements to prevent SQL injection.
 */
public class JourneyDAO {

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a journey for a given train on a specific date.
     *
     * @param trainId the train's unique ID
     * @param date    the journey's departure date
     * @return Journey object if found, otherwise null
     */
    public Journey getJourneyForTrainAndDate(int trainId, LocalDate date) {
        String sql = """
                SELECT journey_id, train_id, departure_date, available_seats
                FROM journeys
                WHERE train_id = ? AND departure_date = ?
                """;
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
     * Retrieves a journey by its unique journey ID.
     *
     * @param journeyId the unique journey ID
     * @return Journey object if found, else null
     */
    public Journey getJourneyById(long journeyId) {
        String sql = """
                SELECT journey_id, train_id, departure_date, available_seats
                FROM journeys
                WHERE journey_id = ?
                """;
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

    // -------------------------------------------------------------------------
    // Update Operations
    // -------------------------------------------------------------------------

    /**
     * Updates available seats JSON for a specific journey.
     *
     * @param journeyId          journey whose seat availability is to be updated
     * @param availableSeatsJson new available seats JSON mapping
     * @return true if update succeeded, false otherwise
     */
    public boolean updateAvailableSeats(long journeyId, String availableSeatsJson) {
        String sql = "UPDATE journeys SET available_seats = ? WHERE journey_id = ?";
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

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new journey record in the database with given train, date, and seat mapping.
     *
     * @param trainId            ID of the train for the journey
     * @param departureDate      Departure date of the journey
     * @param availableSeatsJson JSON string with seat availability info
     * @return generated journey ID if successful, -1 if failed
     */
    public long createJourney(int trainId, LocalDate departureDate, String availableSeatsJson) {
        String sql = """
                INSERT INTO journeys (train_id, departure_date, available_seats)
                VALUES (?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, trainId);
            stmt.setDate(2, Date.valueOf(departureDate));
            stmt.setString(3, availableSeatsJson);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("Error creating journey: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}