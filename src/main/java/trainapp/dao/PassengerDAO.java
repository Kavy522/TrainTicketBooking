package trainapp.dao;

import trainapp.model.Passenger;
import trainapp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for passenger record management.
 * Handles creation and retrieval of passenger data linked to train bookings.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Create new passenger records tied to bookings</li>
 *   <li>Retrieve all passengers for a given booking</li>
 *   <li>Manages auto-generated passenger IDs</li>
 *   <li>Prevents SQL injection via prepared statements</li>
 *   <li>Ensures resources are released with try-with-resources</li>
 * </ul>
 *
 * <p>Useful for reporting, ticketing, and analytics around passenger travel.
 */
public class PassengerDAO {

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Creates a passenger record in the database for a given booking.
     * Assigns the auto-generated passenger ID to the entity upon success.
     *
     * @param passenger Passenger object to persist (must have valid booking ID, name, etc.)
     * @return true if creation was successful and passengerId is assigned, false otherwise
     * @throws IllegalArgumentException if passenger is null or has invalid data
     */
    public boolean createPassenger(Passenger passenger) {
        if (passenger == null) {
            throw new IllegalArgumentException("Passenger cannot be null");
        }
        if (passenger.getBookingId() <= 0 || passenger.getName() == null || passenger.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Passenger must have a valid booking ID and name");
        }

        String sql = """
                INSERT INTO passengers (booking_id, name, age, gender, seat_number, coach_type)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, passenger.getBookingId());
            stmt.setString(2, passenger.getName());
            stmt.setInt(3, passenger.getAge());
            stmt.setString(4, passenger.getGender());
            stmt.setString(5, passenger.getSeatNumber());
            stmt.setString(6, passenger.getCoachType());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        passenger.setPassengerId(generatedKeys.getLong(1));
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating passenger: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves all passenger records associated with a specific booking.
     *
     * @param bookingId Booking ID for which to retrieve passengers
     * @return List of Passenger objects (empty if none found)
     */
    public List<Passenger> getPassengersByBookingId(long bookingId) {
        List<Passenger> passengers = new ArrayList<>();
        String sql = """
                SELECT passenger_id, booking_id, name, age, gender, seat_number, coach_type
                FROM passengers WHERE booking_id = ?
                ORDER BY passenger_id
                """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, bookingId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Passenger passenger = new Passenger();
                    passenger.setPassengerId(rs.getLong("passenger_id"));
                    passenger.setBookingId(rs.getLong("booking_id"));
                    passenger.setName(rs.getString("name"));
                    passenger.setAge(rs.getInt("age"));
                    passenger.setGender(rs.getString("gender"));
                    passenger.setSeatNumber(rs.getString("seat_number"));
                    passenger.setCoachType(rs.getString("coach_type"));
                    passengers.add(passenger);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting passengers: " + e.getMessage());
            e.printStackTrace();
        }

        return passengers;
    }
}
