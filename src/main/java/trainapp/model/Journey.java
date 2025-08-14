package trainapp.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Journey {
    private long journeyId;
    private int trainId;
    private LocalDate departureDate;
    private String availableSeatsJson;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructors
    public Journey() {}

    public Journey(int trainId, LocalDate departureDate, String availableSeatsJson) {
        this.trainId = trainId;
        this.departureDate = departureDate;
        this.availableSeatsJson = availableSeatsJson;
    }

    // Getters and Setters
    public long getJourneyId() { return journeyId; }
    public void setJourneyId(long journeyId) { this.journeyId = journeyId; }

    public int getTrainId() { return trainId; }
    public void setTrainId(int trainId) { this.trainId = trainId; }

    public LocalDate getDepartureDate() { return departureDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }

    public String getAvailableSeatsJson() { return availableSeatsJson; }
    public void setAvailableSeatsJson(String availableSeatsJson) { this.availableSeatsJson = availableSeatsJson; }

    // Helper methods to work with JSON seat data
    public Map<String, Integer> getAvailableSeatsMap() {
        try {
            if (availableSeatsJson != null && !availableSeatsJson.trim().isEmpty()) {
                TypeReference<Map<String, Integer>> typeRef = new TypeReference<Map<String, Integer>>() {};
                return objectMapper.readValue(availableSeatsJson, typeRef);
            }
        } catch (Exception e) {
            System.err.println("Error parsing available seats JSON: " + e.getMessage());
        }
        return getDefaultSeatMap();
    }

    public void setAvailableSeatsMap(Map<String, Integer> seatMap) {
        try {
            this.availableSeatsJson = objectMapper.writeValueAsString(seatMap);
        } catch (Exception e) {
            System.err.println("Error converting seat map to JSON: " + e.getMessage());
            this.availableSeatsJson = "{\"SL\":0,\"3A\":0,\"2A\":0,\"1A\":0}";
        }
    }

    private Map<String, Integer> getDefaultSeatMap() {
        Map<String, Integer> defaultMap = new HashMap<>();
        defaultMap.put("SL", 0);
        defaultMap.put("3A", 0);
        defaultMap.put("2A", 0);
        defaultMap.put("1A", 0);
        return defaultMap;
    }

    @Override
    public String toString() {
        return "Journey{" +
                "journeyId=" + journeyId +
                ", trainId=" + trainId +
                ", departureDate=" + departureDate +
                ", availableSeats='" + availableSeatsJson + '\'' +
                '}';
    }
}