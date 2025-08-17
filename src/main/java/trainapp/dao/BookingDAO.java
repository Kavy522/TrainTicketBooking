package trainapp.dao;

import trainapp.model.Booking;
import trainapp.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

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
            stmt.setString(7, mapStatusToDbCode(booking.getStatus())); // Fixed: Apply mapping here too
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
                    booking.setStatus(rs.getString("status")); // Keep DB value as-is
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
     * FIXED: Update booking status - maps to your DB ENUM values exactly
     */
    public boolean updateBookingStatus(long bookingId, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String dbStatus = mapStatusToDbCode(status);

            System.out.println("Updating booking " + bookingId + " status from '" + status + "' to '" + dbStatus + "'");

            stmt.setString(1, dbStatus);
            stmt.setLong(2, bookingId);

            int rowsUpdated = stmt.executeUpdate();
            boolean success = rowsUpdated > 0;

            if (success) {
                System.out.println("✅ Successfully updated booking status to: " + dbStatus);
            } else {
                System.err.println("❌ No rows updated for booking ID: " + bookingId);
            }

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
     * FIXED: Map status names to your exact DB ENUM values
     * Your DB has: enum('conformed','waiting','cancelled')
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
                return "conformed"; // ⚠️ Maps to your misspelled DB enum value

            case "waiting":
            case "pending":
            case "wait":
                return "waiting"; // ✅ Exact match

            case "cancelled":
            case "cancel":
            case "canceled":
            case "failed":
            case "fail":
            case "refunded":
            case "refund":
                return "cancelled"; // ✅ Exact match

            default:
                return "waiting"; // Safe default
        }
    }

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
     * Check if PNR exists (essential for uniqueness validation)
     */
    public boolean pnrExists(String pnr) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE pnr = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, pnr);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error checking PNR existence: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get booking by PNR number
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
}