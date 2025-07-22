package trainapp.model;

public class Station {

    private int stationId;
    private String stationCode;
    private String name;
    private String city;
    private String state;

    // Constructors
    public Station() {}

    public Station(int stationId, String stationCode, String name, String city, String state) {
        this.stationId = stationId;
        this.stationCode = stationCode;
        this.name = name;
        this.city = city;
        this.state = state;
    }

    // Getters and Setters
    public int getStationId() {
        return stationId;
    }

    public void setStationId(int stationId) {
        this.stationId = stationId;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    @Override
    public String toString() {
        return stationCode + " - " + name + ", " + city + ", " + state;
    }
}

