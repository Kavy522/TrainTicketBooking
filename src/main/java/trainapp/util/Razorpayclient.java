package trainapp.util;

import com.razorpay.Order;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Razorpayclient {

    // Replace with your actual Razorpay credentials
    private static final String RAZORPAY_KEY_ID = "rzp_test_R5ATDyKY49alUF";
    private static final String RAZORPAY_KEY_SECRET = "5JHY6hPtBAGJ8n8R9iQ9TRtE";

    private final RazorpayClient client;
    private PaymentClient payments;

    public Razorpayclient() {
        try {
            this.client = new RazorpayClient(RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET);
        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to initialize Razorpay client", e);
        }
    }

    public String createOrder(double amount, String currency, String receipt) {
        try {
            System.out.println("Creating Razorpay order for amount: " + amount);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int)(amount * 100)); // Amount in paise
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1); // Auto capture

            Order order = client.orders.create(orderRequest);
            String orderId = order.get("id");

            System.out.println("Razorpay order created successfully: " + orderId);
            return orderId;

        } catch (RazorpayException e) {
            System.err.println("Error creating Razorpay order: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            System.out.println("Verifying payment signature...");

            String generatedSignature = generateSignature(orderId, paymentId, RAZORPAY_KEY_SECRET);
            boolean isValid = generatedSignature.equals(signature);

            System.out.println("Payment verification result: " + (isValid ? "SUCCESS" : "FAILED"));
            return isValid;

        } catch (Exception e) {
            System.err.println("Error verifying payment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String generateSignature(String orderId, String paymentId, String secret)
            throws NoSuchAlgorithmException, InvalidKeyException {

        String payload = orderId + "|" + paymentId;

        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);

        byte[] signatureBytes = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : signatureBytes) {
            result.append(String.format("%02x", b));
        }

        return result.toString();
    }

    public boolean createRefund(String paymentId, double amount, String reason) {
        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", (int)(amount * 100)); // Amount in paise
            refundRequest.put("notes", new JSONObject().put("reason", reason));

            client.payments.refund(paymentId, refundRequest);
            System.out.println("Refund created successfully for payment: " + paymentId);
            return true;

        } catch (RazorpayException e) {
            System.err.println("Error creating refund: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}