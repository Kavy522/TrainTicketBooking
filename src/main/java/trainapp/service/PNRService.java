package trainapp.service;

import trainapp.dao.*;
import trainapp.model.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class PNRService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();

    /**
     * Get complete PNR status information
     */
    public PNRStatusResult getPNRStatus(String pnrNumber) {
        try {
            System.out.println("Checking PNR status for: " + pnrNumber);

            // Get booking by PNR
            Booking booking = getBookingByPNR(pnrNumber);
            if (booking == null) {
                return PNRStatusResult.notFound("PNR not found. Please check your PNR number and try again.");
            }

            // Get additional information
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());
            List<Passenger> passengers = passengerDAO.getPassengersByBookingId(booking.getBookingId());
            Payment payment = paymentDAO.getPaymentByBookingId(booking.getBookingId());
            Journey journey = journeyDAO.getJourneyById(booking.getJourneyId());

            // Create comprehensive PNR status
            PNRStatusInfo pnrInfo = new PNRStatusInfo();
            pnrInfo.setPnr(pnrNumber);
            pnrInfo.setBooking(booking);
            pnrInfo.setTrain(train);
            pnrInfo.setFromStation(fromStation);
            pnrInfo.setToStation(toStation);
            pnrInfo.setPassengers(passengers);
            pnrInfo.setPayment(payment);
            pnrInfo.setJourney(journey);

            return PNRStatusResult.success("PNR details retrieved successfully.", pnrInfo);

        } catch (Exception e) {
            System.err.println("Error getting PNR status: " + e.getMessage());
            e.printStackTrace();
            return PNRStatusResult.error("Failed to retrieve PNR status. Please try again later.");
        }
    }

    /**
     * Get booking by PNR number with custom query
     */
    private Booking getBookingByPNR(String pnr) {
        // Use existing BookingDAO method or implement custom query
        try {
            return bookingDAO.getBookingByPNR(pnr);
        } catch (Exception e) {
            System.err.println("Error getting booking by PNR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get formatted booking status for display
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
     * Get status color class for UI styling
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

    // Inner classes
    public static class PNRStatusInfo {
        private String pnr;
        private Booking booking;
        private Train train;
        private Station fromStation;
        private Station toStation;
        private List<Passenger> passengers;
        private Payment payment;
        private Journey journey;

        // Getters and setters
        public String getPnr() { return pnr; }
        public void setPnr(String pnr) { this.pnr = pnr; }

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

        // Helper methods
        public String getFormattedJourneyDate() {
            if (journey != null && journey.getDepartureDate() != null) {
                return journey.getDepartureDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, EEEE"));
            }
            return "Date not available";
        }

        public String getTrainDetails() {
            if (train != null) {
                return train.getTrainNumber() + " - " + train.getName();
            }
            return "Train details not available";
        }

        public String getRouteDetails() {
            if (fromStation != null && toStation != null) {
                return fromStation.getName() + " → " + toStation.getName();
            }
            return "Route details not available";
        }
    }

    public static class PNRStatusResult {
        private final boolean success;
        private final String message;
        private final PNRStatusInfo data;
        private final PNRResultType type;

        private PNRStatusResult(boolean success, String message, PNRStatusInfo data, PNRResultType type) {
            this.success = success;
            this.message = message;
            this.data = data;
            this.type = type;
        }

        public static PNRStatusResult success(String message, PNRStatusInfo data) {
            return new PNRStatusResult(true, message, data, PNRResultType.SUCCESS);
        }

        public static PNRStatusResult notFound(String message) {
            return new PNRStatusResult(false, message, null, PNRResultType.NOT_FOUND);
        }

        public static PNRStatusResult error(String message) {
            return new PNRStatusResult(false, message, null, PNRResultType.ERROR);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public PNRStatusInfo getData() { return data; }
        public PNRResultType getType() { return type; }
    }

    public enum PNRResultType {
        SUCCESS, NOT_FOUND, ERROR
    }
}
