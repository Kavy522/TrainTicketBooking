package trainapp.dao;

import trainapp.model.Booking;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Access Object (DAO) for Booking-related database operations.
 * Provides CRUD actions, search, and reporting methods for the `bookings` table.
 *
 * <p>Features:
 * <ul>
 *   <li>Create new bookings and passengers</li>
 *   <li>Update/Cancel/Delete bookings</li>
 *   <li>Find bookings by ID, user, PNR, or all</li>
 *   <li>Reporting: total count and revenue</li>
 *   <li>Handles database connection and statement resource management</li>
 * </ul>
 *
 * <p>All methods use prepared statements to prevent SQL injection.
 */
public class BookingDAO {

    // -------------------------------------------------------------------------
    // Booking Creation & Persistence
    // -------------------------------------------------------------------------

    /**
     * Creates a booking record in the database.
     *
     * @param booking Booking object containing all relevant information
     * @return Generated booking ID if successful, 0 otherwise
     */
    public long createBooking(Booking booking) {
        String sql = """
                INSERT INTO bookings (user_id, journey_id, train_id, source_station_id, dest_station_id,
                                     total_fare, status, pnr, booking_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, booking.getUserId());
            stmt.setLong(2, booking.getJourneyId());
            stmt.setInt(3, booking.getTrainId());
            stmt.setInt(4, booking.getSourceStationId());
            stmt.setInt(5, booking.getDestStationId());
            stmt.setDouble(6, booking.getTotalFare());
            stmt.setString(7, mapStatusToDbCode(booking.getStatus())); // Ensures DB enum mapping
            stmt.setString(8, booking.getPnr());
            stmt.setTimestamp(9, Timestamp.valueOf(booking.getBookingTime()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getLong(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating booking: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a booking by its unique database ID.
     *
     * @param bookingId the booking's unique ID
     * @return Booking object if found, null otherwise
     */
    public Booking getBookingById(long bookingId) {
        String sql = """
                SELECT booking_id, user_id, journey_id, train_id, source_station_id, dest_station_id,
                       booking_time, total_fare, status, pnr
                FROM bookings WHERE booking_id = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setUserId(rs.getInt("user_id"));
                    booking.setJourneyId(rs.getLong("journey_id"));
                    booking.setTrainId(rs.getInt("train_id"));
                    booking.setSourceStationId(rs.getInt("source_station_id"));
                    booking.setDestStationId(rs.getInt("dest_station_id"));
                    booking.setBookingTime(rs.getTimestamp("booking_time").toLocalDateTime());
                    booking.setTotalFare(rs.getDouble("total_fare"));
                    booking.setStatus(rs.getString("status"));
                    booking.setPnr(rs.getString("pnr"));
                    return booking;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting booking: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Finds all bookings placed by a specific user, ordered by most recent.
     *
     * @param userId user ID for which to retrieve bookings
     * @return List of bookings, or empty list if none found
     */
    public List<Booking> getBookingsByUserId(int userId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = """
                SELECT booking_id, user_id, journey_id, train_id, source_station_id, dest_station_id,
                       booking_time, total_fare, status, pnr
                FROM bookings
                WHERE user_id = ?
                ORDER BY booking_time DESC
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setUserId(rs.getInt("user_id"));
                    booking.setJourneyId(rs.getLong("journey_id"));
                    booking.setTrainId(rs.getInt("train_id"));
                    booking.setSourceStationId(rs.getInt("source_station_id"));
                    booking.setDestStationId(rs.getInt("dest_station_id"));
                    booking.setBookingTime(rs.getTimestamp("booking_time").toLocalDateTime());
                    booking.setTotalFare(rs.getDouble("total_fare"));
                    booking.setStatus(rs.getString("status"));
                    booking.setPnr(rs.getString("pnr"));
                    bookings.add(booking);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting bookings by user ID: " + e.getMessage());
            e.printStackTrace();
        }
        return bookings;
    }

    /**
     * Retrieves a booking using the Passenger Name Record (PNR) code.
     *
     * @param pnr the booking's PNR code
     * @return Booking object if found, null if not
     */
    public Booking getBookingByPNR(String pnr) {
        String sql = """
                SELECT booking_id, user_id, journey_id, train_id, source_station_id, dest_station_id,
                       booking_time, total_fare, status, pnr
                FROM bookings WHERE pnr = ?
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pnr);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Booking booking = new Booking();
                    booking.setBookingId(rs.getLong("booking_id"));
                    booking.setUserId(rs.getInt("user_id"));
                    booking.setJourneyId(rs.getLong("journey_id"));
                    booking.setTrainId(rs.getInt("train_id"));
                    booking.setSourceStationId(rs.getInt("source_station_id"));
                    booking.setDestStationId(rs.getInt("dest_station_id"));
                    booking.setBookingTime(rs.getTimestamp("booking_time").toLocalDateTime());
                    booking.setTotalFare(rs.getDouble("total_fare"));
                    booking.setStatus(rs.getString("status"));
                    booking.setPnr(rs.getString("pnr"));
                    return booking;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting booking by PNR: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves all bookings in the system, ordered by booking time (most recent first).
     * Includes user and train info for reporting purposes.
     *
     * @return List of all Booking objects (including user name, train number), empty if none
     */
    public List<Booking> getAllBookings() {
        List<Booking> bookings = new ArrayList<>();
        String sql = """
                SELECT b.booking_id, b.user_id, b.journey_id, b.train_id, b.source_station_id, b.dest_station_id,
                       b.booking_time, b.total_fare, b.status, b.pnr, u.name AS user_name, t.train_number AS train_number
                FROM bookings b
                JOIN users u ON b.user_id = u.user_id
                JOIN trains t ON b.train_id = t.train_id
                ORDER BY b.booking_time DESC
                """;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Booking booking = new Booking();
                booking.setBookingId(rs.getLong("booking_id"));
                booking.setUserId(rs.getInt("user_id"));
                booking.setJourneyId(rs.getLong("journey_id"));
                booking.setTrainId(rs.getInt("train_id"));
                booking.setSourceStationId(rs.getInt("source_station_id"));
                booking.setDestStationId(rs.getInt("dest_station_id"));
                booking.setBookingTime(rs.getTimestamp("booking_time").toLocalDateTime());
                booking.setTotalFare(rs.getDouble("total_fare"));
                booking.setStatus(rs.getString("status"));
                booking.setPnr(rs.getString("pnr"));
                booking.setUserName(rs.getString("user_name"));
                booking.setTrainNumber(rs.getString("train_number"));
                bookings.add(booking);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all bookings: " + e.getMessage());
            e.printStackTrace();
        }
        return bookings;
    }

    // -------------------------------------------------------------------------
    // Update Operations
    // -------------------------------------------------------------------------

    /**
     * Updates the status of an existing booking by its ID.
     * Maps status to allowed DB ENUM values to avoid errors.
     *
     * @param bookingId the unique booking ID to update
     * @param status    status string (human or DB format)
     * @return true if update successful, false otherwise
     */
    public boolean updateBookingStatus(long bookingId, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String dbStatus = mapStatusToDbCode(status);

            stmt.setString(1, dbStatus);
            stmt.setLong(2, bookingId);

            int rowsUpdated = stmt.executeUpdate();
            boolean success = rowsUpdated > 0;
            return success;
        } catch (SQLException e) {
            System.err.println("❌ Error updating booking status: " + e.getMessage());
            System.err.println("Attempted to set status: '" + status + "' for booking: " + bookingId);
            System.err.println("Available ENUM values: 'conformed', 'waiting', 'cancelled'");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cancels a booking by ID (sets status to 'cancelled').
     *
     * @param bookingId the booking to cancel
     * @return true if cancellation succeeded, false otherwise
     */
    public boolean cancelBooking(long bookingId) {
        String sql = "UPDATE bookings SET status = 'cancelled' WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookingId);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("Error canceling booking: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Delete Operations
    // -------------------------------------------------------------------------

    /**
     * Deletes a booking by its unique ID.
     *
     * @param bookingId the booking ID to delete
     * @return true if booking was deleted, false otherwise
     */
    public boolean deleteBooking(long bookingId) {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, bookingId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting booking: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Reporting & Statistics
    // -------------------------------------------------------------------------

    /**
     * Retrieves the total count of all bookings in the system.
     *
     * @return total number of bookings (0 if error)
     * @throws SQLException if a database access error occurs
     */
    public int getBookingCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting booking count: " + e.getMessage());
            throw e;
        }
        return 0;
    }

    /**
     * Computes the total confirmed revenue over all bookings.
     *
     * @return total fare from confirmed and conformed bookings
     * @throws SQLException if a database access error occurs
     */
    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT SUM(total_fare) FROM bookings WHERE status IN ('conformed', 'confirmed')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total revenue: " + e.getMessage());
            throw e;
        }
        return 0.0;
    }

    // -------------------------------------------------------------------------
    // Internal Helpers
    // -------------------------------------------------------------------------

    /**
     * Maps human-readable or frontend status strings to the database ENUM values.
     * Ensures no failures from misspelling or casing issues.
     *
     * @param status input status string
     * @return well-formed STRING for use in SQL update/insert.
     */
    private String mapStatusToDbCode(String status) {
        if (status == null) return "waiting";
        switch (status.toLowerCase().trim()) {
            case "confirmed":
            case "confirm":
            case "success":
            case "paid":
            case "completed":
            case "complete":
                return "conformed";
            case "waiting":
            case "pending":
            case "wait":
                return "waiting";
            case "cancelled":
            case "cancel":
            case "canceled":
            case "failed":
            case "fail":
            case "refunded":
            case "refund":
                return "cancelled";
            default:
                return "waiting";
        }
    }
}