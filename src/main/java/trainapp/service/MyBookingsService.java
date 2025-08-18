package trainapp.service;

import trainapp.dao.*;
import trainapp.model.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MyBookingsService handles user booking retrieval, statistics calculation,
 * detailed booking information creation, and status formatting.
 *
 * Sections organized by: booking retrieval, statistics, status formatting,
 * helper methods, and data model classes.
 */
public class MyBookingsService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();

    // -------------------------------------------------------------------------
    // Booking Retrieval Methods
    // -------------------------------------------------------------------------

    /**
     * Retrieves all bookings for a user with complete details.
     * Includes train, station, passenger, payment, and journey information.
     *
     * @param userId User's database ID
     * @return MyBookingsResult containing detailed booking list or error
     */
    public MyBookingsResult getAllBookings(int userId) {
        try {
            System.out.println("Loading all bookings for user: " + userId);

            List<Booking> bookings = bookingDAO.getBookingsByUserId(userId);
            if (bookings.isEmpty()) {
                return MyBookingsResult.noBookings("No bookings found. Book your first train journey!");
            }

            // Convert to detailed booking info
            List<DetailedBookingInfo> detailedBookings = new ArrayList<>();
            for (Booking booking : bookings) {
                DetailedBookingInfo detailedInfo = createDetailedBookingInfo(booking);
                if (detailedInfo != null) {
                    detailedBookings.add(detailedInfo);
                }
            }

            // Sort by booking time (newest first)
            detailedBookings.sort((a, b) -> b.getBooking().getBookingTime().compareTo(a.getBooking().getBookingTime()));

            return MyBookingsResult.success("Bookings loaded successfully", detailedBookings);

        } catch (Exception e) {
            System.err.println("Error loading bookings: " + e.getMessage());
            e.printStackTrace();
            return MyBookingsResult.error("Failed to load bookings. Please try again later.");
        }
    }

    /**
     * Creates detailed booking information by gathering related data.
     * Assembles complete booking details from multiple tables.
     *
     * @param booking Base booking object
     * @return DetailedBookingInfo with all related data or null on error
     */
    private DetailedBookingInfo createDetailedBookingInfo(Booking booking) {
        try {
            DetailedBookingInfo info = new DetailedBookingInfo();
            info.setBooking(booking);

            // Get train information
            Train train = trainDAO.getTrainById(booking.getTrainId());
            info.setTrain(train);

            // Get station information
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());
            info.setFromStation(fromStation);
            info.setToStation(toStation);

            // Get passengers
            List<Passenger> passengers = passengerDAO.getPassengersByBookingId(booking.getBookingId());
            info.setPassengers(passengers);

            // Get payment information
            Payment payment = paymentDAO.getPaymentByBookingId(booking.getBookingId());
            info.setPayment(payment);

            // Get journey information
            Journey journey = journeyDAO.getJourneyById(booking.getJourneyId());
            info.setJourney(journey);

            return info;

        } catch (Exception e) {
            System.err.println("Error creating detailed booking info: " + e.getMessage());
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Statistics & Analytics Methods
    // -------------------------------------------------------------------------

    /**
     * Calculates comprehensive booking statistics for a user.
     * Includes total bookings, status breakdown, and total spending.
     *
     * @param userId User's database ID
     * @return BookingStatistics object with calculated metrics
     */
    public BookingStatistics getBookingStatistics(int userId) {
        try {
            List<Booking> allBookings = bookingDAO.getBookingsByUserId(userId);

            BookingStatistics stats = new BookingStatistics();
            stats.setTotalBookings(allBookings.size());

            int confirmed = 0, pending = 0, cancelled = 0;
            double totalSpent = 0.0;

            for (Booking booking : allBookings) {
                switch (booking.getStatus().toLowerCase()) {
                    case "conformed":
                    case "confirmed":
                        confirmed++;
                        totalSpent += booking.getTotalFare();
                        break;
                    case "waiting":
                    case "pending":
                        pending++;
                        break;
                    case "cancelled":
                        cancelled++;
                        break;
                }
            }

            stats.setConfirmedBookings(confirmed);
            stats.setPendingBookings(pending);
            stats.setCancelledBookings(cancelled);
            stats.setTotalSpent(totalSpent);

            return stats;

        } catch (Exception e) {
            System.err.println("Error getting booking statistics: " + e.getMessage());
            return new BookingStatistics();
        }
    }

    // -------------------------------------------------------------------------
    // Status Formatting & Display Helpers
    // -------------------------------------------------------------------------

    /**
     * Formats database status for user-friendly display.
     * Handles status variations and provides consistent formatting.
     *
     * @param dbStatus Raw status from database
     * @return Formatted status string for display
     */
    public String getFormattedStatus(String dbStatus) {
        if (dbStatus == null) return "Unknown";

        switch (dbStatus.toLowerCase()) {
            case "conformed":
            case "confirmed":
                return "CONFIRMED";
            case "waiting":
            case "pending":
                return "PENDING";
            case "cancelled":
                return "CANCELLED";
            default:
                return dbStatus.toUpperCase();
        }
    }

    /**
     * Provides CSS class name for status styling in UI.
     * Used for consistent visual representation of booking statuses.
     *
     * @param status Booking status
     * @return CSS class name for styling
     */
    public String getStatusColorClass(String status) {
        switch (status.toLowerCase()) {
            case "confirmed":
            case "conformed":
                return "status-confirmed";
            case "pending":
            case "waiting":
                return "status-pending";
            case "cancelled":
                return "status-cancelled";
            default:
                return "status-unknown";
        }
    }

    // -------------------------------------------------------------------------
    // Data Model Classes
    // -------------------------------------------------------------------------

    /**
     * Comprehensive booking information container with all related data.
     * Includes booking, train, stations, passengers, payment, and journey details.
     */
    public static class DetailedBookingInfo {
        private Booking booking;
        private Train train;
        private Station fromStation;
        private Station toStation;
        private List<Passenger> passengers;
        private Payment payment;
        private Journey journey;

        // Getters and setters
        public Booking getBooking() { return booking; }
        public void setBooking(Booking booking) { this.booking = booking; }

        public Train getTrain() { return train; }
        public void setTrain(Train train) { this.train = train; }

        public Station getFromStation() { return fromStation; }
        public void setFromStation(Station fromStation) { this.fromStation = fromStation; }

        public Station getToStation() { return toStation; }
        public void setToStation(Station toStation) { this.toStation = toStation; }

        public List<Passenger> getPassengers() { return passengers; }
        public void setPassengers(List<Passenger> passengers) { this.passengers = passengers; }

        public Payment getPayment() { return payment; }
        public void setPayment(Payment payment) { this.payment = payment; }

        public Journey getJourney() { return journey; }
        public void setJourney(Journey journey) { this.journey = journey; }

        /** @return formatted journey date for display */
        public String getFormattedJourneyDate() {
            if (journey != null && journey.getDepartureDate() != null) {
                return journey.getDepartureDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE"));
            }
            return "Date not available";
        }

        /** @return train details as display string */
        public String getTrainDetails() {
            if (train != null) {
                return train.getTrainNumber() + " - " + train.getName();
            }
            return "Train details not available";
        }

        /** @return route details as display string */
        public String getRouteDetails() {
            if (fromStation != null && toStation != null) {
                return fromStation.getName() + " → " + toStation.getName();
            }
            return "Route details not available";
        }

        /** @return passenger count summary */
        public String getPassengerSummary() {
            if (passengers != null && !passengers.isEmpty()) {
                int count = passengers.size();
                return count + " Passenger" + (count > 1 ? "s" : "");
            }
            return "No passengers";
        }

        /** @return true if journey is in the future */
        public boolean isUpcomingJourney() {
            if (journey != null && journey.getDepartureDate() != null) {
                return journey.getDepartureDate().isAfter(java.time.LocalDate.now());
            }
            return false;
        }

        /** @return true if journey is in the past */
        public boolean isPastJourney() {
            if (journey != null && journey.getDepartureDate() != null) {
                return journey.getDepartureDate().isBefore(java.time.LocalDate.now());
            }
            return false;
        }
    }

    /**
     * Statistical summary of user's booking history.
     */
    public static class BookingStatistics {
        private int totalBookings;
        private int confirmedBookings;
        private int pendingBookings;
        private int cancelledBookings;
        private double totalSpent;

        // Getters and setters
        public int getTotalBookings() { return totalBookings; }
        public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

        public int getConfirmedBookings() { return confirmedBookings; }
        public void setConfirmedBookings(int confirmedBookings) { this.confirmedBookings = confirmedBookings; }

        public int getPendingBookings() { return pendingBookings; }
        public void setPendingBookings(int pendingBookings) { this.pendingBookings = pendingBookings; }

        public int getCancelledBookings() { return cancelledBookings; }
        public void setCancelledBookings(int cancelledBookings) { this.cancelledBookings = cancelledBookings; }

        public double getTotalSpent() { return totalSpent; }
        public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }
    }

    /**
     * Result wrapper for booking operations with status and data.
     */
    public static class MyBookingsResult {
        private final boolean success;
        private final String message;
        private final List<DetailedBookingInfo> bookings;
        private final MyBookingsResultType type;

        private MyBookingsResult(boolean success, String message, List<DetailedBookingInfo> bookings, MyBookingsResultType type) {
            this.success = success;
            this.message = message;
            this.bookings = bookings;
            this.type = type;
        }

        public static MyBookingsResult success(String message, List<DetailedBookingInfo> bookings) {
            return new MyBookingsResult(true, message, bookings, MyBookingsResultType.SUCCESS);
        }

        public static MyBookingsResult noBookings(String message) {
            return new MyBookingsResult(false, message, new ArrayList<>(), MyBookingsResultType.NO_BOOKINGS);
        }

        public static MyBookingsResult error(String message) {
            return new MyBookingsResult(false, message, new ArrayList<>(), MyBookingsResultType.ERROR);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<DetailedBookingInfo> getBookings() { return bookings; }
        public MyBookingsResultType getType() { return type; }
    }

    /**
     * Classification for result types from booking operations.
     */
    public enum MyBookingsResultType {
        SUCCESS, NO_BOOKINGS, ERROR
    }
}