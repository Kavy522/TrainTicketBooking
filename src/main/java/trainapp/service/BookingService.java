package trainapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import trainapp.dao.*;
import trainapp.model.*;
import trainapp.util.PDFGenerator;
import trainapp.util.Razorpayclient;
import trainapp.util.SMSService;
import trainapp.model.TrainClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FIXED BookingService - Ensures consistent amount handling across booking, payment, and invoice.
 * The booking amount is always exactly what comes from the booking summary, saved to DB,
 * and used throughout payment processing and invoice generation.
 */
public class BookingService {

    // Dependencies
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    private final Razorpayclient razorpayClient = new Razorpayclient();
    private final EmailService emailService = new EmailService();
    private final SMSService smsService = new SMSService();
    private final PDFGenerator pdfGenerator = new PDFGenerator();
    private final TrainService trainService = new TrainService();
    private final AdminDataStructureService adminService = new AdminDataStructureService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * FIXED: Creates booking with exact amount from booking summary request.
     * No recalculation - uses the exact amount that was shown in booking summary.
     */
    public BookingResult createBookingWithPayment(BookingRequest bookingRequest) {
        BookingResult result = new BookingResult();
        try {
            System.out.println("=== CREATING BOOKING WITH CONSISTENT AMOUNT ===");
            System.out.println("Request amount from booking summary: ₹" + bookingRequest.getTotalAmount());

            if (!validateBookingRequest(bookingRequest)) {
                result.setSuccess(false);
                result.setMessage("Invalid booking request");
                return result;
            }

            if (!checkSeatAvailability(bookingRequest)) {
                result.setSuccess(false);
                result.setMessage("Seats not available for selected class");
                return result;
            }

            // CRITICAL FIX: Use the exact amount from booking summary (no recalculation)
            double exactBookingAmount = Math.round(bookingRequest.getTotalAmount() * 100.0) / 100.0;
            bookingRequest.setTotalAmount(exactBookingAmount);

            Booking booking = createInitialBooking(bookingRequest);
            if (booking == null) {
                result.setSuccess(false);
                result.setMessage("Failed to create booking record");
                return result;
            }

            if (!createPassengerRecords(booking.getBookingId(), bookingRequest.getPassengers(), bookingRequest.getSeatClass())) {
                result.setSuccess(false);
                result.setMessage("Failed to create passenger records");
                return result;
            }

            String razorpayOrderId = razorpayClient.createOrder(booking.getTotalFare(), "INR", booking.getPnr());
            if (razorpayOrderId == null) {
                result.setSuccess(false);
                result.setMessage("Failed to create payment order");
                return result;
            }

            result.setSuccess(true);
            result.setBooking(booking);
            result.setRazorpayOrderId(razorpayOrderId);
            result.setMessage("Booking created successfully with amount: ₹" + String.format("%.2f", booking.getTotalFare()));

            System.out.println("=== BOOKING CREATED SUCCESSFULLY ===");
            System.out.println("Booking ID: " + booking.getBookingId());
            System.out.println("Amount saved to DB: ₹" + booking.getTotalFare());
            System.out.println("PNR: " + booking.getPnr());
            System.out.println("This amount matches booking summary exactly!");

            return result;

        } catch (Exception e) {
            System.err.println("Error in booking process: " + e.getMessage());
            e.printStackTrace();
            result.setSuccess(false);
            result.setMessage("Booking failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * FIXED: Handle successful payment using booking's saved amount as authoritative.
     */
    public BookingResult handleSuccessfulPayment(PaymentSuccessRequest paymentRequest) {
        BookingResult result = new BookingResult();
        try {
            System.out.println("=== PROCESSING PAYMENT SUCCESS ===");
            System.out.println("Booking ID: " + paymentRequest.getBookingId());

            boolean paymentVerified = razorpayClient.verifyPayment(
                    paymentRequest.getRazorpayOrderId(),
                    paymentRequest.getRazorpayPaymentId(),
                    paymentRequest.getRazorpaySignature()
            );
            if (!paymentVerified) {
                result.setSuccess(false);
                result.setMessage("Payment verification failed");
                return result;
            }

            Booking booking = bookingDAO.getBookingById(paymentRequest.getBookingId());
            if (booking == null) {
                result.setSuccess(false);
                result.setMessage("Booking not found");
                return result;
            }

            // Update booking status to confirmed
            if (!bookingDAO.updateBookingStatus(booking.getBookingId(), "confirmed")) {
                result.setSuccess(false);
                result.setMessage("Failed to confirm booking");
                return result;
            }
            booking.setStatus("confirmed");

            // CRITICAL FIX: Create payment record using booking's saved amount
            Payment payment = new Payment();
            payment.setBookingId(booking.getBookingId());
            payment.setTransactionId(paymentRequest.getRazorpayPaymentId());
            payment.setAmount(booking.getTotalFare()); // Use booking's saved amount
            payment.setStatus("success");
            payment.setMethod("razorpay");
            payment.setProvider("razorpay");

            if (!paymentDAO.createPayment(payment)) {
                System.err.println("Warning: Failed to record payment details");
            }

            updateSeatAvailabilityAfterBooking(booking);

            // Generate PDFs and send notifications with consistent amounts
            byte[] ticketPdf = pdfGenerator.generateTicketPDF(booking);
            byte[] invoicePdf = pdfGenerator.generateInvoicePDF(booking);
            sendConfirmationEmail(booking, ticketPdf, invoicePdf);
            sendConfirmationSMS(booking);
            recordNotifications(booking.getBookingId());

            result.setSuccess(true);
            result.setBooking(booking);
            result.setMessage("Booking confirmed successfully! Amount: ₹" + String.format("%.2f", booking.getTotalFare()));

            System.out.println("=== PAYMENT PROCESSED SUCCESSFULLY ===");
            System.out.println("Booking: " + booking.getPnr());
            System.out.println("Payment amount: ₹" + booking.getTotalFare());
            System.out.println("Invoice will show this same amount!");

            return result;

        } catch (Exception e) {
            System.err.println("Error processing successful payment: " + e.getMessage());
            e.printStackTrace();
            result.setSuccess(false);
            result.setMessage("Payment processing failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Handle failed payment with proper cleanup
     */
    public void handleFailedPayment(long bookingId, String reason) {
        try {
            System.out.println("Processing failed payment for booking: " + bookingId);

            Booking booking = bookingDAO.getBookingById(bookingId);
            if (booking != null) {
                bookingDAO.updateBookingStatus(bookingId, "cancelled");

                // Create payment record for failed payment
                Payment payment = new Payment();
                payment.setBookingId(bookingId);
                payment.setAmount(booking.getTotalFare());
                payment.setStatus("failed");
                payment.setMethod("razorpay");
                payment.setProvider("razorpay");
                paymentDAO.createPayment(payment);

                System.out.println("Failed payment processed for booking: " + bookingId +
                        " with amount: ₹" + booking.getTotalFare());
            }
        } catch (Exception e) {
            System.err.println("Error handling failed payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ======================= BOOKING CREATION METHODS =======================

    /**
     * FIXED: Create initial booking with exact amount from request (booking summary amount)
     */
    private Booking createInitialBooking(BookingRequest request) {
        try {
            System.out.println("=== CREATING INITIAL BOOKING ===");
            System.out.println("Amount to save to DB: ₹" + request.getTotalAmount());

            Booking booking = new Booking();
            booking.setUserId(request.getUserId());

            Journey journey = journeyDAO.getJourneyForTrainAndDate(request.getTrainId(), request.getJourneyDate());
            if (journey == null) {
                trainService.ensureJourneyExists(request.getTrainId(), request.getJourneyDate());
                journey = journeyDAO.getJourneyForTrainAndDate(request.getTrainId(), request.getJourneyDate());
            }
            if (journey == null) {
                return null;
            }

            booking.setJourneyId(journey.getJourneyId());
            booking.setTrainId(request.getTrainId());

            Station fromStation = stationDAO.getStationByName(request.getFromStation());
            Station toStation = stationDAO.getStationByName(request.getToStation());
            if (fromStation != null && toStation != null) {
                booking.setSourceStationId(fromStation.getStationId());
                booking.setDestStationId(toStation.getStationId());
            } else {
                System.err.println("Could not find station IDs for: " + request.getFromStation() + " -> " + request.getToStation());
                return null;
            }

            // CRITICAL: Use exact amount from booking summary request
            booking.setTotalFare(request.getTotalAmount());
            booking.setStatus("waiting");
            booking.setPnr(generatePNR());
            booking.setBookingTime(LocalDateTime.now());

            long bookingId = bookingDAO.createBooking(booking);
            if (bookingId > 0) {
                booking.setBookingId(bookingId);
                System.out.println("Booking saved to DB with amount: ₹" + booking.getTotalFare());
                System.out.println("This matches booking summary amount exactly!");
                return booking;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error creating initial booking: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ======================= HELPER METHODS =======================

    private boolean validateBookingRequest(BookingRequest request) {
        return request.getUserId() > 0 &&
                request.getTrainId() > 0 &&
                request.getJourneyDate() != null &&
                request.getPassengers() != null &&
                !request.getPassengers().isEmpty() &&
                request.getSeatClass() != null &&
                request.getFromStation() != null &&
                request.getToStation() != null;
    }

    private boolean checkSeatAvailability(BookingRequest request) {
        try {
            Journey journey = journeyDAO.getJourneyForTrainAndDate(request.getTrainId(), request.getJourneyDate());
            if (journey != null) {
                Train train = trainDAO.getTrainById(request.getTrainId());
                Map<String, Integer> availability = trainService.getAvailableSeatsForDate(train, request.getJourneyDate());
                int availableSeats = availability.getOrDefault(request.getSeatClass(), 0);
                return availableSeats >= request.getPassengers().size();
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error checking seat availability: " + e.getMessage());
            return false;
        }
    }

    private boolean createPassengerRecords(long bookingId, List<PassengerInfo> passengers, String selectedClass) {
        try {
            for (int i = 0; i < passengers.size(); i++) {
                PassengerInfo passenger = passengers.get(i);
                trainapp.model.Passenger dbPassenger = new trainapp.model.Passenger();
                dbPassenger.setBookingId(bookingId);
                dbPassenger.setName(passenger.getName());
                dbPassenger.setAge(passenger.getAge());
                dbPassenger.setGender(mapGender(passenger.getGender()));
                dbPassenger.setCoachType(selectedClass);
                dbPassenger.setSeatNumber(generateSeatNumber(selectedClass, i + 1));
                if (!passengerDAO.createPassenger(dbPassenger)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error creating passenger records: " + e.getMessage());
            return false;
        }
    }

    private void updateSeatAvailabilityAfterBooking(Booking booking) {
        try {
            List<Passenger> passengers = passengerDAO.getPassengersByBookingId(booking.getBookingId());
            if (!passengers.isEmpty()) {
                String seatClass = passengers.get(0).getCoachType();
                int passengerCount = passengers.size();
                Journey journey = journeyDAO.getJourneyById(booking.getJourneyId());
                if (journey != null) {
                    trainService.bookSeats(booking.getTrainId(), journey.getDepartureDate(), seatClass, passengerCount);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating seat availability: " + e.getMessage());
        }
    }

    /**
     * FIXED: Send confirmation email with consistent booking amount
     */
    private void sendConfirmationEmail(Booking booking, byte[] ticketPdf, byte[] invoicePdf) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());

            if (user != null) {
                String trainDetails = (train != null)
                        ? train.getTrainNumber() + " - " + train.getName()
                        : "Train Details";

                String journeyDetails = (fromStation != null && toStation != null)
                        ? fromStation.getName() + " → " + toStation.getName() + " | " +
                        booking.getBookingTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                        " | Confirmed | Amount: ₹" + String.format("%.2f", booking.getTotalFare())
                        : "Journey Details";

                boolean emailSent = emailService.sendBookingConfirmationWithAttachments(
                        user.getEmail(), user.getName(), booking.getPnr(),
                        trainDetails, journeyDetails, ticketPdf, invoicePdf
                );

                if (emailSent) {
                    System.out.println("Confirmation email sent with amount: ₹" + booking.getTotalFare());
                } else {
                    System.err.println("Failed to send booking confirmation email");
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending confirmation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * FIXED: Send SMS with consistent booking amount
     */
    private void sendConfirmationSMS(Booking booking) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            if (user != null) {
                boolean smsSent = smsService.sendBookingConfirmation(
                        user.getPhone(), booking.getPnr(), booking.getTotalFare());

                if (smsSent) {
                    System.out.println("Confirmation SMS sent with amount: ₹" + booking.getTotalFare());
                } else {
                    System.err.println("Failed to send booking confirmation SMS");
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending confirmation SMS: " + e.getMessage());
        }
    }

    private void recordNotifications(long bookingId) {
        try {
            Notification notification = new Notification();
            notification.setBookingId(bookingId);
            notification.setEmailSent(true);
            notification.setSmsSent(true);
            notification.setSentAt(LocalDateTime.now());
            notificationDAO.createNotification(notification);
        } catch (Exception e) {
            System.err.println("Error recording notifications: " + e.getMessage());
        }
    }

    private String generatePNR() {
        return "PNR" + System.currentTimeMillis() % 10000000L;
    }

    private String mapGender(String gender) {
        switch (gender.toLowerCase()) {
            case "male": return "M";
            case "female": return "F";
            default: return "O";
        }
    }

    private String generateSeatNumber(String classType, int sequenceNumber) {
        String prefix = switch (classType) {
            case "SL" -> "S";
            case "3A" -> "A";
            case "2A" -> "B";
            case "1A" -> "H";
            default -> "S";
        };
        return prefix + sequenceNumber;
    }

    // ======================= DATA MODEL CLASSES =======================

    public static class BookingRequest {
        private int userId;
        private int trainId;
        private LocalDate journeyDate;
        private List<PassengerInfo> passengers;
        private String seatClass;
        private String fromStation;
        private String toStation;
        private int passengerCount;
        private double totalAmount;

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }
        public int getTrainId() { return trainId; }
        public void setTrainId(int trainId) { this.trainId = trainId; }
        public LocalDate getJourneyDate() { return journeyDate; }
        public void setJourneyDate(LocalDate journeyDate) { this.journeyDate = journeyDate; }
        public List<PassengerInfo> getPassengers() { return passengers; }
        public void setPassengers(List<PassengerInfo> passengers) { this.passengers = passengers; }
        public String getSeatClass() { return seatClass; }
        public void setSeatClass(String seatClass) { this.seatClass = seatClass; }
        public String getFromStation() { return fromStation; }
        public void setFromStation(String fromStation) { this.fromStation = fromStation; }
        public String getToStation() { return toStation; }
        public void setToStation(String toStation) { this.toStation = toStation; }
        public int getPassengerCount() { return passengerCount; }
        public void setPassengerCount(int passengerCount) { this.passengerCount = passengerCount; }
        public double getTotalAmount() { return totalAmount; }
        public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    }

    public static class PassengerInfo {
        private String name;
        private int age;
        private String gender;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }

    public static class PaymentSuccessRequest {
        private long bookingId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;

        public long getBookingId() { return bookingId; }
        public void setBookingId(long bookingId) { this.bookingId = bookingId; }
        public String getRazorpayOrderId() { return razorpayOrderId; }
        public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
        public String getRazorpayPaymentId() { return razorpayPaymentId; }
        public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
        public String getRazorpaySignature() { return razorpaySignature; }
        public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
    }

    public static class BookingResult {
        private boolean success;
        private String message;
        private Booking booking;
        private String razorpayOrderId;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Booking getBooking() { return booking; }
        public void setBooking(Booking booking) { this.booking = booking; }
        public String getRazorpayOrderId() { return razorpayOrderId; }
        public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    }
}