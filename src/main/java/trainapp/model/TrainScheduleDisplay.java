package trainapp.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TrainScheduleDisplay {
    private final StringProperty stationName;
    private final StringProperty arrivalTime;
    private final StringProperty departureTime;
    private final StringProperty haltTime;
    private final StringProperty day;

    public TrainScheduleDisplay(String stationName, String arrivalTime, String departureTime,
                                String haltTime, String day) {
        this.stationName = new SimpleStringProperty(stationName);
        this.arrivalTime = new SimpleStringProperty(arrivalTime);
        this.departureTime = new SimpleStringProperty(departureTime);
        this.haltTime = new SimpleStringProperty(haltTime);
        this.day = new SimpleStringProperty(day);
    }

    // Property getters
    public StringProperty stationNameProperty() { return stationName; }
    public StringProperty arrivalTimeProperty() { return arrivalTime; }
    public StringProperty departureTimeProperty() { return departureTime; }
    public StringProperty haltTimeProperty() { return haltTime; }
    public StringProperty dayProperty() { return day; }

    // Value getters
    public String getStationName() { return stationName.get(); }
    public String getArrivalTime() { return arrivalTime.get(); }
    public String getDepartureTime() { return departureTime.get(); }
    public String getHaltTime() { return haltTime.get(); }
    public String getDay() { return day.get(); }
}
