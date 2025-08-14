package trainapp.model;

import java.time.LocalDateTime;

public class Payment {
    private long paymentId;
    private long bookingId;
    private String method;
    private String transactionId;
    private double amount;
    private String status;
    private String provider;
    private LocalDateTime paymentTime;

    // Constructors
    public Payment() {
        this.paymentTime = LocalDateTime.now();
    }

    public Payment(long bookingId, String method, String transactionId,
                   double amount, String status, String provider) {
        this.bookingId = bookingId;
        this.method = method;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.provider = provider;
        this.paymentTime = LocalDateTime.now();
    }

    // Getters and Setters
    public long getPaymentId() { return paymentId; }
    public void setPaymentId(long paymentId) { this.paymentId = paymentId; }

    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", bookingId=" + bookingId +
                ", method='" + method + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", amount=" + amount +
                ", status='" + status + '\'' +
                ", provider='" + provider + '\'' +
                ", paymentTime=" + paymentTime +
                '}';
    }
}
