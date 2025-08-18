package trainapp.dao;

import trainapp.model.Journey;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JourneyDAO {

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

}