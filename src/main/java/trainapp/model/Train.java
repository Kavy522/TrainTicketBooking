package trainapp.model;

import trainapp.dao.StationDAO;

import java.util.concurrent.TransferQueue;

public class Train {

    StationDAO stationDAO = new StationDAO();

    private int trainId;
    private String trainNumber;
    private String name;
    private int sourceStationId;
    private int destinationStationId;
    private int totalCoaches;

    // Constructors
    public Train() {}

    public Train(int trainId, String trainNumber, String name, int sourceStationId, int destinationStationId, int totalCoaches) {
        this.trainId = trainId;
        this.trainNumber = trainNumber;
        this.name = name;
        this.sourceStationId = sourceStationId;
        this.destinationStationId = destinationStationId;
        this.totalCoaches = totalCoaches;
    }

    // Getters and Setters
    public int getTrainId() {
        return trainId;
    }

    public void setTrainId(int trainId) {
        this.trainId = trainId;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSourceStationId() {
        return sourceStationId;
    }

    public void setSourceStationId(int sourceStationId) {
        this.sourceStationId = sourceStationId;
    }

    public int getDestinationStationId() {
        return destinationStationId;
    }

    public void setDestinationStationId(int destinationStationId) {
        this.destinationStationId = destinationStationId;
    }

    public int getTotalCoaches() {
        return totalCoaches;
    }

    public void setTotalCoaches(int totalCoaches) {
        this.totalCoaches = totalCoaches;
    }

    @Override
    public String toString() {
        return trainNumber + " - " + name;
    }


    public String getSourceStationName(Train train) {
        return stationDAO.getStationByCode(String.valueOf(train.getSourceStationId())).getName();
    }

    public String getDestinationStationName(Train train) {
        return stationDAO.getStationByCode(String.valueOf(train.getDestinationStationId())).getName();
    }
}
