package trainapp.model;

import java.sql.Timestamp;

public class PaymentInfo {
    private long paymentId;
    private long bookingId;
    private String method;       // credit_card, debit_card, upi, netbanking, wallet
    private String transactionId;
    private double amount;
    private String status;       // success, pending, failed
    private String provider;     // razorpay, etc.
    private Timestamp paymentTime;

    // Constructors
    public PaymentInfo() {}

    public PaymentInfo(long paymentId, long bookingId, String method, String transactionId,
                       double amount, String status, String provider, Timestamp paymentTime) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.method = method;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.provider = provider;
        this.paymentTime = paymentTime;
    }

    // Getters and Setters
    public long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(long paymentId) {
        this.paymentId = paymentId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Timestamp getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(Timestamp paymentTime) {
        this.paymentTime = paymentTime;
    }
}
