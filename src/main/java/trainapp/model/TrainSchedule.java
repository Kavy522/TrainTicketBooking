package trainapp.model;

import java.time.LocalTime;

public class TrainSchedule {
    private int scheduleId;
    private int trainId;
    private int stationId;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private int dayNumber;
    private int sequenceOrder;

    // Constructors
    public TrainSchedule() {}

    public TrainSchedule(int scheduleId, int trainId, int stationId, LocalTime arrivalTime,
                         LocalTime departureTime, int dayNumber, int sequenceOrder) {
        this.scheduleId = scheduleId;
        this.trainId = trainId;
        this.stationId = stationId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.dayNumber = dayNumber;
        this.sequenceOrder = sequenceOrder;
    }

    // Getters and Setters
    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }

    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }

    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }

    public int getDayNumber() { return dayNumber; }
    public void setDayNumber(int dayNumber) { this.dayNumber = dayNumber; }

    public int getSequenceOrder() { return sequenceOrder; }
    public void setSequenceOrder(int sequenceOrder) { this.sequenceOrder = sequenceOrder; }
}
