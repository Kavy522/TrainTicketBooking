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

public class BookingService {

    // DAOs
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PassengerDAO passengerDAO = new PassengerDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final JourneyDAO journeyDAO = new JourneyDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final TrainDAO trainDAO = new TrainDAO();
    private final StationDAO stationDAO = new StationDAO();

    // Services and Utilities
    private final Razorpayclient razorpayClient = new Razorpayclient();
    private final EmailService emailService = new EmailService();
    private final SMSService smsService = new SMSService();
    private final PDFGenerator pdfGenerator = new PDFGenerator();
    private final TrainService trainService = new TrainService();
    private final AdminDataStructureService adminService = new AdminDataStructureService();  // For dynamic pricing

    // Utilities
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create booking with payment integration (Updated method name)
     */
    public BookingResult createBookingWithPayment(BookingRequest bookingRequest) {
        BookingResult result = new BookingResult();
        try {
            System.out.println("Starting booking process for user: " + bookingRequest.getUserId());

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

            // Step 3: Create booking record (initially pending)
            Booking booking = createInitialBooking(bookingRequest);
            if (booking == null) {
                result.setSuccess(false);
                result.setMessage("Failed to create booking record");
                return result;
            }

            // Step 4: Create passenger records
            if (!createPassengerRecords(booking.getBookingId(), bookingRequest.getPassengers(), bookingRequest.getSeatClass())) {
                result.setSuccess(false);
                result.setMessage("Failed to create passenger records");
                return result;
            }

            // Step 5: Create Razorpay order
            String razorpayOrderId = razorpayClient.createOrder(booking.getTotalFare(), "INR", booking.getPnr());
            if (razorpayOrderId == null) {
                result.setSuccess(false);
                result.setMessage("Failed to create payment order");
                return result;
            }

            // Return initial result with Razorpay order details
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
     * Handle successful payment and complete booking
     */
    public BookingResult handleSuccessfulPayment(PaymentSuccessRequest paymentRequest) {
        BookingResult result = new BookingResult();
        try {
            System.out.println("Processing successful payment for booking: " + paymentRequest.getBookingId());

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

            // Step 2: Get booking details
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

            booking.setStatus("confirmed"); // Update local object

            // Step 4: Create payment record
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

            // Step 5: Update seat availability
            updateSeatAvailabilityAfterBooking(booking);

            // Step 6: Generate PDF ticket and invoice
            byte[] ticketPdf = generateTicketPDF(booking);
            byte[] invoicePdf = generateInvoicePDF(booking);

            // Step 7: Send confirmation email with PDFs
            sendConfirmationEmail(booking, ticketPdf, invoicePdf);

            // Step 8: Send SMS confirmation
            sendConfirmationSMS(booking);

            // Step 9: Record notifications
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
     * Handle failed payment
     */
    public void handleFailedPayment(long bookingId, String reason) {
        try {
            System.out.println("Processing failed payment for booking: " + bookingId);

            // Update booking status to cancelled
            bookingDAO.updateBookingStatus(bookingId, "cancelled");

            // Create failed payment record
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

    // Private helper methods

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

    private Booking createInitialBooking(BookingRequest request) {
        try {
            Booking booking = new Booking();
            booking.setUserId(request.getUserId());

            // Get or create journey
            Journey journey = journeyDAO.getJourneyForTrainAndDate(request.getTrainId(), request.getJourneyDate());
            if (journey == null) {
                // Create journey if not exists (for admin-created trains)
                trainService.ensureJourneyExists(request.getTrainId(), request.getJourneyDate());
                journey = journeyDAO.getJourneyForTrainAndDate(request.getTrainId(), request.getJourneyDate());
            }

            if (journey == null) {
                return null;
            }

            booking.setJourneyId(journey.getJourneyId());
            booking.setTrainId(request.getTrainId());

            // Get station IDs from station names
            Station fromStation = stationDAO.getStationByName(request.getFromStation());
            Station toStation = stationDAO.getStationByName(request.getToStation());
            if (fromStation != null && toStation != null) {
                booking.setSourceStationId(fromStation.getStationId());
                booking.setDestStationId(toStation.getStationId());
            } else {
                System.err.println("Could not find station IDs for: " + request.getFromStation() + " -> " + request.getToStation());
                return null;
            }

            // Calculate dynamic fare
            double dynamicFare = calculateDynamicFare(request);
            booking.setTotalFare(dynamicFare > 0 ? dynamicFare : request.getTotalAmount());  // Use dynamic if available, else fallback

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
                    trainService.bookSeats(booking.getTrainId(),
                            journey.getDepartureDate(),
                            seatClass, passengerCount);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating seat availability: " + e.getMessage());
        }
    }

    private byte[] generateTicketPDF(Booking booking) {
        try {
            return pdfGenerator.generateTicketPDF(booking);
        } catch (Exception e) {
            System.err.println("Error generating PDF: " + e.getMessage());
            return null;
        }
    }

    private byte[] generateInvoicePDF(Booking booking) {
        try {
            return pdfGenerator.generateInvoicePDF(booking);
        } catch (Exception e) {
            System.err.println("Error generating invoice PDF: " + e.getMessage());
            return null;
        }
    }

    private void sendConfirmationEmail(Booking booking, byte[] ticketPdf, byte[] invoicePdf) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            Train train = trainDAO.getTrainById(booking.getTrainId());
            Station fromStation = stationDAO.getStationById(booking.getSourceStationId());
            Station toStation = stationDAO.getStationById(booking.getDestStationId());
            if (user != null) {
                // Prepare email content
                String trainDetails = (train != null) ?
                        train.getTrainNumber() + " - " + train.getName() :
                        "Train Details";
                String journeyDetails = (fromStation != null && toStation != null) ?
                        fromStation.getName() + " → " + toStation.getName() + " | " +
                                booking.getBookingTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                                " | Confirmed" :
                        "Journey Details";

                // Send email with both attachments
                boolean emailSent = emailService.sendBookingConfirmationWithAttachments(
                        user.getEmail(),
                        user.getName(),
                        booking.getPnr(),
                        trainDetails,
                        journeyDetails,
                        ticketPdf,
                        invoicePdf
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

    private void sendConfirmationSMS(Booking booking) {
        try {
            User user = userDAO.getUserById(booking.getUserId());
            if (user != null) {
                boolean smsSent = smsService.sendBookingConfirmation(
                        user.getPhone(),
                        booking.getPnr(),
                        booking.getTotalFare()
                );

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

    // Helper methods

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

    /**
     * Calculate dynamic fare based on class, distance, and admin-set pricing
     */
    private double calculateDynamicFare(BookingRequest request) {
        // Get distance (use your TrainService)
        Train train = trainDAO.getTrainById(request.getTrainId());
        int distance = new TrainService().getDistanceBetween(train, request.getFromStation(), request.getToStation());

        TrainClass trainClass = TrainClass.fromString(request.getSeatClass());
        TreeMap<Integer, Double> classFares = adminService.getFareStructureForClass(trainClass);

        if (classFares.isEmpty()) {
            return 0.0;  // Fallback
        }

        // Find fare for distance (use floorEntry for nearest lower)
        Map.Entry<Integer, Double> entry = classFares.floorEntry(distance);
        if (entry == null) {
            return 0.0;  // No fare set
        }

        double baseFare = entry.getValue();
        // Adjust for actual distance (e.g., proportional)
        double adjustedFare = baseFare * (distance / (double) entry.getKey());

        // Per passenger, multiply by count
        return adjustedFare * request.getPassengers().size();
    }

    // Inner classes for request/response

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
