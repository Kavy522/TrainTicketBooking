package trainapp.model;

import java.sql.Timestamp;

public class Booking {
    private long bookingId;
    private int userId;
    private long journeyId;
    private int trainId;
    private int sourceStationId;
    private int destStationId;
    private Timestamp bookingTime;
    private double totalFare;
    private String status; // confirmed, waiting, cancelled
    private String pnr;

    // Constructors
    public Booking() {}

    public Booking(long bookingId, int userId, long journeyId, int trainId, int sourceStationId,
                   int destStationId, Timestamp bookingTime, double totalFare,
                   String status, String pnr) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.journeyId = journeyId;
        this.trainId = trainId;
        this.sourceStationId = sourceStationId;
        this.destStationId = destStationId;
        this.bookingTime = bookingTime;
        this.totalFare = totalFare;
        this.status = status;
        this.pnr = pnr;
    }

    // Getters and Setters
    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getJourneyId() {
        return journeyId;
    }

    public void setJourneyId(long journeyId) {
        this.journeyId = journeyId;
    }

    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
    }

    public int getSourceStationId() {
        return sourceStationId;
    }

    public void setSourceStationId(int sourceStationId) {
        this.sourceStationId = sourceStationId;
    }

    public int getDestStationId() {
        return destStationId;
    }

    public void setDestStationId(int destStationId) {
        this.destStationId = destStationId;
    }

    public Timestamp getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(Timestamp bookingTime) {
        this.bookingTime = bookingTime;
    }

    public double getTotalFare() {
        return totalFare;
    }

    public void setTotalFare(double totalFare) {
        this.totalFare = totalFare;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPnr() {
        return pnr;
    }

    public void setPnr(String pnr) {
        this.pnr = pnr;
    }
}
