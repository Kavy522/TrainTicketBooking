package trainapp.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SMSService {

    // Replace with your actual SMS API credentials
    private static final String SMS_API_URL = "https://api.textlocal.in/send/";
    private static final String SMS_API_KEY = "your_textlocal_api_key";
    private static final String SMS_SENDER = "TAILYATRI";

    public boolean sendSMS(String phoneNumber, String message) {
        try {
            System.out.println("Sending SMS to: " + phoneNumber);

            // Clean phone number (remove any spaces, special characters)
            phoneNumber = cleanPhoneNumber(phoneNumber);

            // For demo purposes, just log the SMS
            System.out.println("=== SMS SENT ===");
            System.out.println("To: " + phoneNumber);
            System.out.println("Message: " + message);
            System.out.println("===============");

            return true; // Simulate successful SMS sending

        } catch (Exception e) {
            System.err.println("Error sending SMS: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendBookingConfirmation(String phoneNumber, String pnr, double amount) {
        String message = String.format("Booking Confirmed! PNR: %s. Amount: ₹%.2f. E-ticket sent to email. Safe journey! -Tailyatri", pnr, amount);
        return sendSMS(phoneNumber, message);
    }

    public boolean sendCancellationSMS(String phoneNumber, String pnr) {
        String message = String.format("Booking cancelled for PNR: %s. Refund will be processed within 7 working days. -Tailyatri", pnr);
        return sendSMS(phoneNumber, message);
    }

    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";

        // Remove all non-digit characters
        phoneNumber = phoneNumber.replaceAll("[^0-9]", "");

        // Add country code if not present (assuming India +91)
        if (phoneNumber.length() == 10) {
            phoneNumber = "91" + phoneNumber;
        } else if (phoneNumber.length() == 11 && phoneNumber.startsWith("0")) {
            phoneNumber = "91" + phoneNumber.substring(1);
        }

        return phoneNumber;
    }
}