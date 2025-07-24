package trainapp.dao;

import trainapp.model.Booking;
import trainapp.model.Passenger;

import java.sql.*;
import java.util.*;

import static trainapp.util.DBConnection.getConnection;

public class BookingDAO {

    public void createBooking(Booking booking) {
        String sql = "INSERT INTO bookings (booking_id, user_id, journey_id, train_id, source_station_id, dest_station_id, booking_time, total_fare, status, pnr) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try{
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1,booking.getBookingId());
            ps.setInt(2, booking.getUserId());
            ps.setLong(3, booking.getJourneyId());
            ps.setInt(4, booking.getTrainId());
            ps.setInt(5, booking.getSourceStationId());
            ps.setInt(6, booking.getDestStationId());
            ps.setTimestamp(7, booking.getBookingTime());
            ps.setDouble(8, booking.getTotalFare());
            ps.setString(9, booking.getStatus());
            ps.setString(10, booking.getPnr());
            ps.executeQuery();
        } catch (SQLException e) {
            System.out.println(e);
        }
    }
    public List<Booking> getBookingsByUserId(int userId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE user_id = ?";
        try{
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(returnBooking(rs));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }
    public Booking getBookingByPNR(String pnr) {
        String sql = "SELECT * FROM bookings WHERE pnr = ?";
        try{
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, pnr);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return returnBooking(rs);
            }
            else{
                System.out.println("Booking not found.");
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return null;
    }
    public List<Booking> getBookingsByJourney(long journeyId) {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE journey_id = ?";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, journeyId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(returnBooking(rs));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }
    public List<Booking> getAllBookings() {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT * FROM bookings";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(returnBooking(rs));
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }
    public boolean updateBookingStatus(String pnr, String status) {
        String sql = "UPDATE bookings SET status = ? WHERE pnr = ?";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, pnr);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e);
            return false;
        }
    }
    public List<Passenger> getBookingPassengers(long bookingId) {
        List<Passenger> list = new ArrayList<>();
        String sql = "SELECT * FROM passengers WHERE booking_id = ?";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setLong(1, bookingId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Passenger p = new Passenger();
                p.setPassenger_id(rs.getInt("passenger_id"));
                p.setBooking_id(rs.getLong("booking_id"));
                p.setName(rs.getString("name"));
                p.setAge(rs.getInt("age"));
                p.setGender(rs.getString("gender"));
                p.setSeatNumber(rs.getString("seat_number"));
                p.setCoach_type(rs.getString("coach_type"));
                list.add(p);
            }
        } catch (SQLException e) {
            System.out.println(e);
        }
        return list;
    }
    public boolean deleteBooking(String pnr) {
        String sql = "DELETE FROM bookings WHERE pnr = ?";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, pnr);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return true;
            } else {
                return false;
            }
        } catch (SQLException e) {
            System.out.println(e);
            return false;
        }
    }
    public boolean isPNRExists(String pnr) {
        String sql = "SELECT 1 FROM bookings WHERE pnr = ?";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, pnr);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println(e);
            return false;
        }
    }
    private Booking returnBooking(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getLong("booking_id"));
        b.setUserId(rs.getInt("user_id"));
        b.setJourneyId(rs.getLong("journey_id"));
        b.setTrainId(rs.getInt("train_id"));
        b.setSourceStationId(rs.getInt("source_station_id"));
        b.setDestStationId(rs.getInt("dest_station_id"));
        b.setBookingTime(rs.getTimestamp("booking_time"));
        b.setTotalFare(rs.getDouble("total_fare"));
        b.setStatus(rs.getString("status"));
        b.setPnr(rs.getString("pnr"));
        return b;
    }
}