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
 * BookingService orchestrates the workflow of train booking, including creation,
 * payment integration, seat allocation, dynamic fare calculation, notification,
 * passenger management, and record persistence.
 *
 * <p>
 * Major responsibilities:
 * <ul>
 * <li>Validates and creates bookings with seat verification</li>
 * <li>Handles online payment and payment results</li>
 * <li>Allocates seats and generates tickets/invoices (PDF)</li>
 * <li>Notifies user by email and SMS</li>
 * <li>Records booking, payment, and notification data</li>
 * <li>Supports dynamic fare pricing based on admin structures</li>
 * </ul>
 *
 * All model and request/response classes are included as static inner classes.
 */
public class BookingService {

    // ================== Dependencies & Data Access =====================

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

    // ================== Booking Creation & Payment =====================

    /**
     * Creates a new booking and prepares for payment by generating a Razorpay order.
     *
     * <p>Validates request, checks seat availability, saves booking (pending), generates PNR,
     * persists passengers, and creates payment order.
     *
     * @param bookingRequest Encapsulates user, train, date, route and passengers
     * @return BookingResult carrying status, PNR, and Razorpay order ID (for client frontend)
     */
    public BookingResult createBookingWithPayment(BookingRequest bookingRequest) {
        BookingResult result = new BookingResult();
        try {
            System.out.println("Starting booking process for user: " + bookingRequest.getUserId());

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
            result.setMessage("Booking created successfully. Please proceed with payment.");
            System.out.println("Booking process initiated successfully. Booking ID: " + booking.getBookingId());
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
     * Handles the callback/payments successful event for Razorpay integration.
     *
     * <p>After online payment is completed, verifies payment, updates status,
     * persists the payment, updates seats, generates PDFs, notifies user, records notification.
     *
     * @param paymentRequest Payment request/response payload from Razorpay.
     * @return BookingResult including updated booking details and status.
     */
    public BookingResult handleSuccessfulPayment(PaymentSuccessRequest paymentRequest) {
        BookingResult result = new BookingResult();
        try {
            System.out.println("Processing successful payment for booking: " + paymentRequest.getBookingId());

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
            if (!bookingDAO.updateBookingStatus(booking.getBookingId(), "confirmed")) {
                result.setSuccess(false);
                result.setMessage("Failed to confirm booking");
                return result;
            }
            booking.setStatus("confirmed");

            Payment payment = new Payment();
            payment.setBookingId(booking.getBookingId());
            payment.setTransactionId(paymentRequest.getRazorpayPaymentId());
            payment.setAmount(booking.getTotalFare());
            payment.setStatus("success");
            payment.setMethod("razorpay");
            payment.setProvider("razorpay");
            if (!paymentDAO.createPayment(payment)) {
                System.err.println("Warning: Failed to record payment details");
            }

            updateSeatAvailabilityAfterBooking(booking);
            byte[] ticketPdf = generateTicketPDF(booking);
            byte[] invoicePdf = generateInvoicePDF(booking);
            sendConfirmationEmail(booking, ticketPdf, invoicePdf);
            sendConfirmationSMS(booking);
            recordNotifications(booking.getBookingId());

            result.setSuccess(true);
            result.setBooking(booking);
            result.setMessage("Booking confirmed successfully! Confirmation details sent to your email and phone.");
            System.out.println("Booking completed successfully: " + booking.getPnr());
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
     * Handles booking cancellation and recording of failed payment.
     * Updates booking to 'cancelled' and records failed payment.
     *
     * @param bookingId Booking record
     * @param reason Why the payment failed (for audit/logging)
     */
    public void handleFailedPayment(long bookingId, String reason) {
        try {
            System.out.println("Processing failed payment for booking: " + bookingId);
            bookingDAO.updateBookingStatus(bookingId, "cancelled");

            Payment payment = new Payment();
            payment.setBookingId(bookingId);
            payment.setAmount(0.0);
            payment.setStatus("failed");
            payment.setMethod("razorpay");
            payment.setProvider("razorpay");
            paymentDAO.createPayment(payment);
            System.out.println("Failed payment processed for booking: " + bookingId);
        } catch (Exception e) {
            System.err.println("Error handling failed payment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================== PRIVATE STEP HELPERS =====================

    /**
     * Validates the completeness of a booking request.
     */
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

    /**
     * Checks if required seats are available for the requested class and journey.
     */
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

    /**
     * Constructs and persists the new initial booking (status 'waiting').
     * Looks up journey, station, computes dynamic fare and creates PNR.
     */
    private Booking createInitialBooking(BookingRequest request) {
        try {
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

            double dynamicFare = calculateDynamicFare(request);
            booking.setTotalFare(dynamicFare > 0 ? dynamicFare : request.getTotalAmount());

            booking.setStatus("waiting");
            booking.setPnr(generatePNR());
            booking.setBookingTime(LocalDateTime.now());

            long bookingId = bookingDAO.createBooking(booking);
            if (bookingId > 0) {
                booking.setBookingId(bookingId);
                return booking;
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error creating initial booking: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Persists each passenger for this booking, with assigned seat numbers and class.
     */
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

    /**
     * After booking confirmed, reduces seat count accordingly.
     */
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
     * Generates an e-ticket (PDF) for confirmed booking.
     * @param booking Booking to generate ticket for
     * @return Byte array of PDF data or null on error
     */
    private byte[] generateTicketPDF(Booking booking) {
        try {
            return pdfGenerator.generateTicketPDF(booking);
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a payment invoice as PDF.
     * @param booking Booking for which to generate invoice
     * @return Byte array PDF or null on error
     */
    private byte[] generateInvoicePDF(Booking booking) {
        try {
            return pdfGenerator.generateInvoicePDF(booking);
        } catch (Exception e) {
            System.err.println("Error generating invoice PDF: " + e.getMessage());
            return null;
        }
    }

    /**
     * Sends ETA, journey, and ticket/invoice by email after confirmation.
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
                        " | Confirmed"
                        : "Journey Details";
                boolean emailSent = emailService.sendBookingConfirmationWithAttachments(
                        user.getEmail(), user.getName(), booking.getPnr(),
                        trainDetails, journeyDetails, ticketPdf, invoicePdf
                );
                if (emailSent) {
                    System.out.println("Booking confirmation email sent successfully with attachments");
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
     * Sends confirmation SMS for booking.
     */
    private void sendConfirmationSMS(Booking booking) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            if (user != null) {
                boolean smsSent = smsService.sendBookingConfirmation(
                        user.getPhone(), booking.getPnr(), booking.getTotalFare());
                if (smsSent) {
                    System.out.println("Booking confirmation SMS sent successfully");
                } else {
                    System.err.println("Failed to send booking confirmation SMS");
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending confirmation SMS: " + e.getMessage());
        }
    }

    /**
     * Records notification record after confirmation (email and SMS).
     */
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

    /**
     * Generates a unique PNR number using current timestamp.
     */
    private String generatePNR() {
        return "PNR" + System.currentTimeMillis() % 10000000L;
    }

    /**
     * Maps gender inputs to canonical values ("M", "F", "O").
     */
    private String mapGender(String gender) {
        switch (gender.toLowerCase()) {
            case "male": return "M";
            case "female": return "F";
            default: return "O";
        }
    }

    /**
     * Generates seat numbers per class, incremented for each passenger.
     * @param classType Seat class code
     * @param sequenceNumber index for seat assignment
     * @return Seat number string (e.g. S1, A2)
     */
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

    /**
     * Enhanced dynamic fare calculation using time-based distance.
     */
    private double calculateDynamicFare(BookingRequest request) {
        try {
            Train train = trainDAO.getTrainById(request.getTrainId());
            if (train == null) {
                System.err.println("Train not found for fare calculation");
                return 0.0;
            }

            // Use enhanced time-based distance calculation
            int distance = trainService.getDistanceBetween(train, request.getFromStation(), request.getToStation());
            System.out.println("Using time-based distance: " + distance + " km for fare calculation");

            TrainClass trainClass = TrainClass.fromString(request.getSeatClass());

            // Calculate per-passenger fare using enhanced pricing
            double farePerPassenger = adminService.calculateDynamicFare(trainClass, distance);

            if (farePerPassenger <= 0) {
                System.err.println("Invalid fare calculated, using fallback");
                return calculateFallbackFare(trainClass, distance, request.getPassengers().size());
            }

            // Calculate total fare for all passengers
            double totalFare = farePerPassenger * request.getPassengers().size();

            System.out.println("Dynamic fare calculation: ₹" + farePerPassenger + " × " +
                    request.getPassengers().size() + " passengers = ₹" + totalFare);

            return totalFare;

        } catch (Exception e) {
            System.err.println("Error in dynamic fare calculation: " + e.getMessage());
            return calculateFallbackFare(TrainClass._3A, 500, request.getPassengers().size());
        }
    }

    /**
     * Fallback fare calculation when dynamic pricing fails.
     */
    private double calculateFallbackFare(TrainClass trainClass, int distance, int passengerCount) {
        double baseFarePerKm = switch (trainClass) {
            case SL -> 0.75;
            case _3A -> 2.25;
            case _2A -> 3.50;
            case _1A -> 5.50;
            default -> 2.25;
        };

        double reservationCharge = switch (trainClass) {
            case SL -> 30;
            case _3A -> 50;
            case _2A -> 75;
            case _1A -> 125;
            default -> 50;
        };

        double farePerPassenger = (distance * baseFarePerKm) + reservationCharge;
        return Math.max(farePerPassenger * passengerCount, 200 * passengerCount);
    }


    // ================== Data Models & Request/Result Wrappers =====================

    /**
     * Booking request object for frontend/backend interaction and workflow.
     */
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

        // Getters and setters...
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

    /**
     * Simple passenger info for booking request inbound payloads.
     */
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

    /**
     * Payment callback request model for Razorpay/webhook.
     */
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

    /**
     * Results returned from booking creation or confirmation process.
     */
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
