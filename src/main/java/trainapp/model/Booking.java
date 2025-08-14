package trainapp.model;

import java.time.LocalDateTime;

public class Booking {
    private long bookingId;
    private int userId;
    private long journeyId;
    private int trainId;
    private int sourceStationId;
    private int destStationId;
    private LocalDateTime bookingTime;
    private double totalFare;
    private String status; // waiting, conformed, cancelled
    private String pnr;

    // Constructors
    public Booking() {}

    public Booking(int userId, long journeyId, int trainId, int sourceStationId,
                   int destStationId, double totalFare, String status, String pnr) {
        this.userId = userId;
        this.journeyId = journeyId;
        this.trainId = trainId;
        this.sourceStationId = sourceStationId;
        this.destStationId = destStationId;
        this.totalFare = totalFare;
        this.status = status;
        this.pnr = pnr;
        this.bookingTime = LocalDateTime.now();
    }

    // Getters and Setters
    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public long getJourneyId() { return journeyId; }
    public void setJourneyId(long journeyId) { this.journeyId = journeyId; }

    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }

    public int getSourceStationId() { return sourceStationId; }
    public void setSourceStationId(int sourceStationId) { this.sourceStationId = sourceStationId; }

    public int getDestStationId() { return destStationId; }
    public void setDestStationId(int destStationId) { this.destStationId = destStationId; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public double getTotalFare() { return totalFare; }
    public void setTotalFare(double totalFare) { this.totalFare = totalFare; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPnr() { return pnr; }
    public void setPnr(String pnr) { this.pnr = pnr; }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", userId=" + userId +
                ", trainId=" + trainId +
                ", pnr='" + pnr + '\'' +
                ", status='" + status + '\'' +
                ", totalFare=" + totalFare +
                '}';
    }
}