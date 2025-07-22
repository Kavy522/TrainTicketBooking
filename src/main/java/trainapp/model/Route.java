package trainapp.model;

import java.sql.Time;

public class Route {
    private int scheduleId;
    private int trainId;
    private int stationId;
    private Time arrivalTime;
    private Time departureTime;
    private int dayNumber;           // e.g., Day 1, Day 2
    private int sequenceOrder;       // Order in the journey

    public Route() {}

    public Route(int scheduleId, int trainId, int stationId, Time arrivalTime,
                 Time departureTime, int dayNumber, int sequenceOrder) {
        this.scheduleId = scheduleId;
        this.trainId = trainId;
        this.stationId = stationId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.dayNumber = dayNumber;
        this.sequenceOrder = sequenceOrder;
    }

    // Getters and Setters
    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
    }

    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public Time getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Time arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public Time getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Time departureTime) {
        this.departureTime = departureTime;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public void setSequenceOrder(int sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

}
