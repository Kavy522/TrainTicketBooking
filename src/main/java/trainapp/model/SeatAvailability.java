package trainapp.model;

import java.util.HashMap;
import java.util.Map;

public class SeatAvailability {

    private Map<String, Integer> seatMap;

    public SeatAvailability() {
        this.seatMap = new HashMap<>();
    }

    public SeatAvailability(Map<String, Integer> seatMap) {
        this.seatMap = seatMap;
    }

    public void addCoachType(String coachType, int seats) {
        seatMap.put(coachType, seats);
    }

    public void updateSeats(String coachType, int delta) {
        seatMap.put(coachType, seatMap.getOrDefault(coachType, 0) + delta);
    }

    public int getAvailableSeats(String coachType) {
        return seatMap.getOrDefault(coachType, 0);
    }

    public boolean isAvailable(String coachType, int requestedSeats) {
        return seatMap.getOrDefault(coachType, 0) >= requestedSeats;
    }

    public void reduceSeats(String coachType, int count) {
        int available = seatMap.getOrDefault(coachType, 0);
        if (available >= count) {
            seatMap.put(coachType, available - count);
        }
    }

    public Map<String, Integer> getSeatMap() {
        return seatMap;
    }

    public void setSeatMap(Map<String, Integer> seatMap) {
        this.seatMap = seatMap;
    }

    @Override
    public String toString() {
        return "SeatAvailability{" +
                "seatMap=" + seatMap +
                '}';
    }
}
