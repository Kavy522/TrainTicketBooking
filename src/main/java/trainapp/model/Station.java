package trainapp.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Station {
    private final IntegerProperty stationId = new SimpleIntegerProperty();
    private final StringProperty stationCode = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty state = new SimpleStringProperty();  // Changed from zone

    public Station() {}

    public Station(int stationId, String stationCode, String name, String city, String state) {
        this.stationId.set(stationId);
        this.stationCode.set(stationCode);
        this.name.set(name);
        this.city.set(city);
        this.state.set(state);
    }

    // Getters, Setters, Properties
    public int getStationId() { return stationId.get(); }
    public void setStationId(int id) { stationId.set(id); }
    public IntegerProperty stationIdProperty() { return stationId; }

    public String getStationCode() { return stationCode.get(); }
    public void setStationCode(String code) { stationCode.set(code); }
    public StringProperty stationCodeProperty() { return stationCode; }

    public String getName() { return name.get(); }
    public void setName(String name) { this.name.set(name); }
    public StringProperty nameProperty() { return name; }

    public String getCity() { return city.get(); }
    public void setCity(String city) { this.city.set(city); }
    public StringProperty cityProperty() { return city; }

    public String getState() { return state.get(); }
    public void setState(String state) { this.state.set(state); }
    public StringProperty stateProperty() { return state; }
}
