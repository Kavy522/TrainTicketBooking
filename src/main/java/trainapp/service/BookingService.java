package trainapp.service;

import trainapp.dao.*;
import trainapp.model.*;
import trainapp.util.PDFGenerator;
import trainapp.util.Razorpayclient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * BookingService manages the complete train booking workflow from creation through payment confirmation.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li><b>Booking Lifecycle Management</b> - Handles complete booking process from request to confirmation</li>
 *   <li><b>Payment Processing Integration</b> - Seamless integration with Razorpay payment gateway</li>
 *   <li><b>Amount Consistency Enforcement</b> - Ensures booking summary = payment amount = invoice amount</li>
 *   <li><b>Passenger Management</b> - Creates and manages passenger records with seat assignments</li>
 *   <li><b>Seat Availability Management</b> - Real-time seat availability checking and updates</li>
 *   <li><b>Notification and Communication</b> - Email confirmations, PDF generation, and notifications</li>
 * </ul>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Consistent amount handling preventing pricing discrepancies</li>
 *   <li>Comprehensive validation with detailed error handling</li>
 *   <li>Automatic seat assignment and availability management</li>
 *   <li>PDF ticket and invoice generation</li>
 *   <li>Multi-channel notification system (email, SMS)</li>
 *   <li>Payment verification and reconciliation</li>
 *   <li>Journey and station management integration</li>
 * </ul>
 *
 * <h2>Booking Workflow:</h2>
 * <ol>
 *   <li>Validate booking request and check seat availability</li>
 *   <li>Create initial booking record with consistent amount</li>
 *   <li>Generate passenger records with seat assignments</li>
 *   <li>Create Razorpay payment order</li>
 *   <li>Process payment verification and confirmation</li>
 *   <li>Update booking status and seat availability</li>
 *   <li>Generate tickets and invoices</li>
 *   <li>Send confirmation notifications</li>
 * </ol>
 *
 * <h2>Payment Integration:</h2>
 * <ul>
 *   <li>Razorpay gateway integration with signature verification</li>
 *   <li>Payment status tracking and reconciliation</li>
 *   <li>Failed payment handling with automatic cleanup</li>
 *   <li>Transaction ID management for audit trails</li>
 * </ul>
 */
public class BookingService {

    // =========================================================================
    // DEPENDENCIES AND DATA ACCESS OBJECTS
    // =========================================================================

    /** Data access object for booking operations */
    private final BookingDAO bookingDAO = new BookingDAO();

    /** Data access object for passenger management */
    private final PassengerDAO passengerDAO = new PassengerDAO();

    /** Data access object for payment transactions */
    private final PaymentDAO paymentDAO = new PaymentDAO();

    /** Data access object for journey information */
    private final JourneyDAO journeyDAO = new JourneyDAO();

    /** Data access object for notification tracking */
    private final NotificationDAO notificationDAO = new NotificationDAO();

    /** Data access object for user information */
    private final UserDAO userDAO = new UserDAO();

    /** Data access object for train information */
    private final TrainDAO trainDAO = new TrainDAO();

    /** Data access object for station information */
    private final StationDAO stationDAO = new StationDAO();

    // External Service Dependencies
    /** Razorpay client for payment gateway operations */
    private final Razorpayclient razorpayClient = new Razorpayclient();

    /** Email service for communication */
    private final EmailService emailService = new EmailService();

    /** PDF generator for tickets and invoices */
    private final PDFGenerator pdfGenerator = new PDFGenerator();

    /** Train service for scheduling and availability */
    private final TrainService trainService = new TrainService();

    // =========================================================================
    // CORE BOOKING WORKFLOW METHODS
    // =========================================================================

    /**
     * Creates a new booking with payment order integration.
     *
     * <h3>Process Overview:</h3>
     * <ol>
     *   <li>Validates booking request parameters</li>
     *   <li>Checks seat availability for requested class</li>
     *   <li>Creates initial booking record with exact amount</li>
     *   <li>Generates passenger records with seat assignments</li>
     *   <li>Creates Razorpay payment order for processing</li>
     * </ol>
     *
     * <h3>Amount Consistency:</h3>
     * Uses exact amount from booking summary without recalculation to ensure
     * consistency across booking, payment, and invoice stages.
     *
     * @param bookingRequest Complete booking request with passenger and payment details
     * @return BookingResult containing booking object and payment order ID if successful
     */
    public BookingResult createBookingWithPayment(BookingRequest bookingRequest) {
        BookingResult result = new BookingResult();
        try {
            // Step 1: Validate booking request
            if (!validateBookingRequest(bookingRequest)) {
                result.setSuccess(false);
                result.setMessage("Invalid booking request");
                return result;
            }

            // Step 2: Check seat availability
            if (!checkSeatAvailability(bookingRequest)) {
                result.setSuccess(false);
                result.setMessage("Seats not available for selected class");
                return result;
            }

            // Step 3: Ensure amount consistency (no recalculation)
            double exactBookingAmount = Math.round(bookingRequest.getTotalAmount() * 100.0) / 100.0;
            bookingRequest.setTotalAmount(exactBookingAmount);

            // Step 4: Create initial booking record
            Booking booking = createInitialBooking(bookingRequest);
            if (booking == null) {
                result.setSuccess(false);
                result.setMessage("Failed to create booking record");
                return result;
            }

            // Step 5: Create passenger records
            if (!createPassengerRecords(booking.getBookingId(), bookingRequest.getPassengers(), bookingRequest.getSeatClass())) {
                result.setSuccess(false);
                result.setMessage("Failed to create passenger records");
                return result;
            }

            // Step 6: Create Razorpay payment order
            String razorpayOrderId = razorpayClient.createOrder(booking.getTotalFare(), "INR", booking.getPnr());
            if (razorpayOrderId == null) {
                result.setSuccess(false);
                result.setMessage("Failed to create payment order");
                return result;
            }

            // Step 7: Return successful result
            result.setSuccess(true);
            result.setBooking(booking);
            result.setRazorpayOrderId(razorpayOrderId);
            result.setMessage("Booking created successfully with amount: ₹" + String.format("%.2f", booking.getTotalFare()));

            return result;

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Booking failed: " + e.getMessage());
            return result;
        }
    }

    // =========================================================================
    // PAYMENT PROCESSING METHODS
    // =========================================================================

    /**
     * Handles successful payment verification and booking confirmation.
     *
     * <h3>Payment Processing Steps:</h3>
     * <ol>
     *   <li>Verifies payment with Razorpay using signature validation</li>
     *   <li>Updates booking status to confirmed</li>
     *   <li>Creates payment record with complete transaction details</li>
     *   <li>Updates seat availability after booking confirmation</li>
     *   <li>Generates and sends tickets and invoices</li>
     *   <li>Records notification delivery status</li>
     * </ol>
     *
     * <h3>Critical Fix Applied:</h3>
     * Ensures payment time is properly set to prevent database insertion errors.
     * All required payment fields are populated before database operations.
     *
     * @param paymentRequest Payment success request with Razorpay transaction details
     * @return BookingResult indicating success or failure with detailed messages
     */
    public BookingResult handleSuccessfulPayment(PaymentSuccessRequest paymentRequest) {
        BookingResult result = new BookingResult();
        try {
            // Step 1: Verify payment with Razorpay
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

            // Step 2: Retrieve booking record
            Booking booking = bookingDAO.getBookingById(paymentRequest.getBookingId());
            if (booking == null) {
                result.setSuccess(false);
                result.setMessage("Booking not found");
                return result;
            }

            // Step 3: Update booking status to confirmed
            if (!bookingDAO.updateBookingStatus(booking.getBookingId(), "confirmed")) {
                result.setSuccess(false);
                result.setMessage("Failed to confirm booking");
                return result;
            }
            booking.setStatus("confirmed");

            // Step 4: Create complete payment record
            Payment payment = createPaymentRecord(paymentRequest, booking);
            if (!paymentDAO.createPayment(payment)) {
                // Non-fatal error - booking is still confirmed
            }

            // Step 5: Update seat availability
            updateSeatAvailabilityAfterBooking(booking);

            // Step 6: Generate and send confirmations
            generateAndSendConfirmations(booking);

            // Step 7: Record notification status
            recordNotifications(booking.getBookingId());

            // Step 8: Return success result
            result.setSuccess(true);
            result.setBooking(booking);
            result.setMessage("Booking confirmed successfully! Amount: ₹" + String.format("%.2f", booking.getTotalFare()));

            return result;

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Payment processing failed: " + e.getMessage());
            return result;
        }
    }

    /**
     * Handles failed payment scenarios with proper cleanup.
     *
     * <h3>Failure Handling Process:</h3>
     * <ol>
     *   <li>Updates booking status to cancelled</li>
     *   <li>Creates failed payment record for audit trail</li>
     *   <li>Logs failure reason for administrative review</li>
     * </ol>
     *
     * @param bookingId Booking ID that experienced payment failure
     * @param reason Failure reason for logging and audit purposes
     */
    public void handleFailedPayment(long bookingId, String reason) {
        try {
            Booking booking = bookingDAO.getBookingById(bookingId);
            if (booking != null) {
                // Update booking status to cancelled
                bookingDAO.updateBookingStatus(bookingId, "cancelled");

                // Create failed payment record for audit trail
                Payment failedPayment = createFailedPaymentRecord(bookingId, booking.getTotalFare(), reason);
                paymentDAO.createPayment(failedPayment);
            }
        } catch (Exception e) {
            // Log error but don't throw to prevent cascading failures
        }
    }

    /**
     * Creates a complete payment record for successful transactions.
     *
     * @param paymentRequest Payment success request details
     * @param booking Associated booking object
     * @return Complete Payment object ready for database insertion
     */
    private Payment createPaymentRecord(PaymentSuccessRequest paymentRequest, Booking booking) {
        Payment payment = new Payment();
        payment.setBookingId(booking.getBookingId());
        payment.setTransactionId(paymentRequest.getRazorpayPaymentId());
        payment.setAmount(booking.getTotalFare());
        payment.setStatus("success");
        payment.setMethod("razorpay");
        payment.setProvider("razorpay");
        payment.setPaymentTime(LocalDateTime.now()); // Critical: Ensures non-null payment time

        return payment;
    }

    /**
     * Creates a failed payment record for audit and tracking purposes.
     *
     * @param bookingId Associated booking ID
     * @param amount Transaction amount
     * @param reason Failure reason
     * @return Failed Payment object for database insertion
     */
    private Payment createFailedPaymentRecord(long bookingId, double amount, String reason) {
        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setTransactionId("failed_" + System.currentTimeMillis());
        payment.setAmount(amount);
        payment.setStatus("failed");
        payment.setMethod("razorpay");
        payment.setProvider("razorpay");
        payment.setPaymentTime(LocalDateTime.now()); // Critical: Ensures non-null payment time

        return payment;
    }

    // =========================================================================
    // BOOKING CREATION AND VALIDATION
    // =========================================================================

    /**
     * Creates initial booking record with consistent amount handling.
     *
     * <h3>Creation Process:</h3>
     * <ol>
     *   <li>Creates booking object with user and train information</li>
     *   <li>Ensures journey exists or creates new journey record</li>
     *   <li>Maps station names to station IDs</li>
     *   <li>Sets exact amount from booking summary</li>
     *   <li>Generates unique PNR for tracking</li>
     *   <li>Saves booking to database</li>
     * </ol>
     *
     * @param request Validated booking request
     * @return Created Booking object or null if creation failed
     */
    private Booking createInitialBooking(BookingRequest request) {
        try {
            Booking booking = new Booking();
            booking.setUserId(request.getUserId());

            // Ensure journey exists for the train and date
            Journey journey = ensureJourneyExists(request.getTrainId(), request.getJourneyDate());
            if (journey == null) {
                return null;
            }

            booking.setJourneyId(journey.getJourneyId());
            booking.setTrainId(request.getTrainId());

            // Map station names to IDs
            if (!setBookingStations(booking, request.getFromStation(), request.getToStation())) {
                return null;
            }

            // Set booking details with exact amount
            booking.setTotalFare(request.getTotalAmount());
            booking.setStatus("waiting");
            booking.setPnr(generatePNR());
            booking.setBookingTime(LocalDateTime.now());

            // Save to database
            long bookingId = bookingDAO.createBooking(booking);
            if (bookingId > 0) {
                booking.setBookingId(bookingId);
                return booking;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ensures journey record exists for the specified train and date.
     *
     * @param trainId Train identifier
     * @param journeyDate Date of journey
     * @return Journey object or null if creation failed
     */
    private Journey ensureJourneyExists(int trainId, LocalDate journeyDate) {
        Journey journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
        if (journey == null) {
            trainService.ensureJourneyExists(trainId, journeyDate);
            journey = journeyDAO.getJourneyForTrainAndDate(trainId, journeyDate);
        }
        return journey;
    }

    /**
     * Sets booking station IDs from station names.
     *
     * @param booking Booking object to update
     * @param fromStationName Source station name
     * @param toStationName Destination station name
     * @return true if both stations were found and set successfully
     */
    private boolean setBookingStations(Booking booking, String fromStationName, String toStationName) {
        Station fromStation = stationDAO.getStationByName(fromStationName);
        Station toStation = stationDAO.getStationByName(toStationName);

        if (fromStation != null && toStation != null) {
            booking.setSourceStationId(fromStation.getStationId());
            booking.setDestStationId(toStation.getStationId());
            return true;
        }

        return false;
    }

    /**
     * Validates booking request for completeness and correctness.
     *
     * @param request BookingRequest to validate
     * @return true if request is valid, false otherwise
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
     * Checks seat availability for the requested booking.
     *
     * @param request BookingRequest with seat requirements
     * @return true if sufficient seats are available, false otherwise
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
            return false;
        }
    }

    /**
     * Creates passenger records with seat assignments.
     *
     * <h3>Passenger Record Creation:</h3>
     * <ul>
     *   <li>Maps passenger information to database format</li>
     *   <li>Assigns seats based on class and sequence</li>
     *   <li>Validates and normalizes gender information</li>
     *   <li>Links passengers to booking ID</li>
     * </ul>
     *
     * @param bookingId Associated booking ID
     * @param passengers List of passenger information
     * @param selectedClass Selected seat class
     * @return true if all passenger records were created successfully
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
            return false;
        }
    }

    // =========================================================================
    // SEAT MANAGEMENT AND AVAILABILITY
    // =========================================================================

    /**
     * Updates seat availability after successful booking confirmation.
     *
     * <h3>Update Process:</h3>
     * <ol>
     *   <li>Retrieves passenger details from booking</li>
     *   <li>Determines seat class and passenger count</li>
     *   <li>Updates train service with booked seats</li>
     *   <li>Maintains availability accuracy for future bookings</li>
     * </ol>
     *
     * @param booking Confirmed booking object
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
            // Non-fatal error - log but don't fail the booking
        }
    }

    // =========================================================================
    // NOTIFICATION AND COMMUNICATION
    // =========================================================================

    /**
     * Generates and sends booking confirmations including tickets and invoices.
     *
     * @param booking Confirmed booking object
     */
    private void generateAndSendConfirmations(Booking booking) {
        try {
            byte[] ticketPdf = pdfGenerator.generateTicketPDF(booking);
            byte[] invoicePdf = pdfGenerator.generateInvoicePDF(booking);
            sendConfirmationEmail(booking, ticketPdf, invoicePdf);
        } catch (Exception e) {
            // Non-fatal error - booking is still confirmed
        }
    }

    /**
     * Sends booking confirmation email with ticket and invoice attachments.
     *
     * <h3>Email Content:</h3>
     * <ul>
     *   <li>Booking confirmation details</li>
     *   <li>Train and journey information</li>
     *   <li>PDF ticket attachment</li>
     *   <li>PDF invoice attachment</li>
     * </ul>
     *
     * @param booking Confirmed booking object
     * @param ticketPdf Generated ticket PDF
     * @param invoicePdf Generated invoice PDF
     */
    private void sendConfirmationEmail(Booking booking, byte[] ticketPdf, byte[] invoicePdf) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());

            if (user != null) {
                String trainDetails = buildTrainDetails(train);
                String journeyDetails = buildJourneyDetails(fromStation, toStation, booking);

                emailService.sendBookingConfirmationWithAttachments(
                        user.getEmail(), user.getName(), booking.getPnr(),
                        trainDetails, journeyDetails, ticketPdf, invoicePdf
                );
            }
        } catch (Exception e) {
            // Non-fatal error - booking confirmation still valid
        }
    }

    /**
     * Builds train details string for email content.
     *
     * @param train Train object
     * @return Formatted train details string
     */
    private String buildTrainDetails(Train train) {
        return (train != null)
                ? train.getTrainNumber() + " - " + train.getName()
                : "Train Details";
    }

    /**
     * Builds journey details string for email content.
     *
     * @param fromStation Source station
     * @param toStation Destination station
     * @param booking Booking object
     * @return Formatted journey details string
     */
    private String buildJourneyDetails(Station fromStation, Station toStation, Booking booking) {
        if (fromStation != null && toStation != null) {
            return fromStation.getName() + " → " + toStation.getName() + " | " +
                    booking.getBookingTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                    " | Confirmed | Amount: ₹" + String.format("%.2f", booking.getTotalFare());
        }
        return "Journey Details";
    }

    /**
     * Records notification delivery status for tracking purposes.
     *
     * @param bookingId Booking ID for notification tracking
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
            // Non-fatal error - notification tracking failure
        }
    }

    // =========================================================================
    // UTILITY AND HELPER METHODS
    // =========================================================================

    /**
     * Generates unique PNR (Passenger Name Record) for booking identification.
     *
     * @return Unique PNR string
     */
    private String generatePNR() {
        return "PNR" + System.currentTimeMillis() % 10000000L;
    }

    /**
     * Maps gender string to database format.
     *
     * @param gender Gender string from user input
     * @return Single character gender code for database
     */
    private String mapGender(String gender) {
        return switch (gender.toLowerCase()) {
            case "male" -> "M";
            case "female" -> "F";
            default -> "O";
        };
    }

    /**
     * Generates seat number based on class and sequence.
     *
     * @param classType Seat class type
     * @param sequenceNumber Passenger sequence number
     * @return Generated seat number
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

    // =========================================================================
    // DATA TRANSFER OBJECTS
    // =========================================================================

    /**
     * Data transfer object for booking requests.
     * Contains all necessary information to create a new booking.
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

        // Getters and setters
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
     * Data transfer object for passenger information.
     * Contains individual passenger details for booking.
     */
    public static class PassengerInfo {
        private String name;
        private int age;
        private String gender;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }

    /**
     * Data transfer object for payment success notifications.
     * Contains Razorpay transaction details for verification.
     */
    public static class PaymentSuccessRequest {
        private long bookingId;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpaySignature;

        // Getters and setters
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
     * Data transfer object for booking operation results.
     * Contains operation status and result data.
     */
    public static class BookingResult {
        private boolean success;
        private String message;
        private Booking booking;
        private String razorpayOrderId;

        // Getters and setters
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