package trainapp.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

public class Journey {
    private long journeyId;
    private int trainId;
    private Date departureDate;
    private Map<String, Integer> availableSeats; // e.g., {"SL": 120, "3A": 48}
    private String status; // scheduled, departed, cancelled
    private Timestamp createdAt;


    public Journey() {}

    public Journey(long journeyId, int trainId, Date departureDate, Map<String, Integer> availableSeats,
                   String status, Timestamp createdAt) {
        this.journeyId = journeyId;
        this.trainId = trainId;
        this.departureDate = departureDate;
        this.availableSeats = availableSeats;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
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

    public Date getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(Date departureDate) {
        this.departureDate = departureDate;
    }

    public Map<String, Integer> getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Map<String, Integer> availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

