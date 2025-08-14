package trainapp.dao;

import trainapp.model.Station;
import trainapp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StationDAO {

    /**
     * Get Station by station_id
     */
    public Station getStationById(int stationId) {
        String sql = "SELECT station_id, station_code, name, city, state FROM stations WHERE station_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stationId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Station station = new Station();
                    station.setStationId(rs.getInt("station_id"));
                    station.setStationCode(rs.getString("station_code"));
                    station.setName(rs.getString("name"));
                    station.setCity(rs.getString("city"));
                    station.setState(rs.getString("state"));
                    return station;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting station by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get Station by station code
     */
    public Station getStationByCode(String stationCode) {
        String sql = "SELECT station_id, station_code, name, city, state FROM stations WHERE station_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stationCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Station station = new Station();
                    station.setStationId(rs.getInt("station_id"));
                    station.setStationCode(rs.getString("station_code"));
                    station.setName(rs.getString("name"));
                    station.setCity(rs.getString("city"));
                    station.setState(rs.getString("state"));
                    return station;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting station by code: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get all stations
     */
    public List<Station> getAllStations() {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT station_id, station_code, name, city, state FROM stations ORDER BY name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Station station = new Station();
                station.setStationId(rs.getInt("station_id"));
                station.setStationCode(rs.getString("station_code"));
                station.setName(rs.getString("name"));
                station.setCity(rs.getString("city"));
                station.setState(rs.getString("state"));
                stations.add(station);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all stations: " + e.getMessage());
            e.printStackTrace();
        }

        return stations;
    }

    /**
     * Search stations by name pattern
     */
    public Station getStationByName(String namePattern) {
        Station station = new Station();
        String sql = "SELECT station_id, station_code, name, city, state FROM stations WHERE name LIKE ? ORDER BY name LIMIT 10";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + namePattern + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    station.setStationId(rs.getInt("station_id"));
                    station.setStationCode(rs.getString("station_code"));
                    station.setName(rs.getString("name"));
                    station.setCity(rs.getString("city"));
                    station.setState(rs.getString("state"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching stations: " + e.getMessage());
            e.printStackTrace();
        }

        return station;
    }

    /**
     * Add a new station to the database
     */
    public boolean addStation(Station station) {
        String sql = "INSERT INTO stations (station_code, name, city, state) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, station.getStationCode());
            stmt.setString(2, station.getName());
            stmt.setString(3, station.getCity());
            stmt.setString(4, station.getState());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error adding station: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Update an existing station
     */
    public boolean updateStation(Station station) {
        String sql = "UPDATE stations SET station_code = ?, name = ?, city = ?, state = ? WHERE station_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, station.getStationCode());
            stmt.setString(2, station.getName());
            stmt.setString(3, station.getCity());
            stmt.setString(4, station.getState());
            stmt.setInt(5, station.getStationId());

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error updating station: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Delete a station from the database
     */
    public boolean deleteStation(int stationId) {
        String sql = "DELETE FROM stations WHERE station_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, stationId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting station: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if station code already exists
     */
    public boolean stationCodeExists(String stationCode) {
        String sql = "SELECT COUNT(*) FROM stations WHERE station_code = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, stationCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking station code existence: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get stations by state
     */
    public List<Station> getStationsByState(String state) {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT station_id, station_code, name, city, state FROM stations WHERE state = ? ORDER BY name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, state);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Station station = new Station();
                    station.setStationId(rs.getInt("station_id"));
                    station.setStationCode(rs.getString("station_code"));
                    station.setName(rs.getString("name"));
                    station.setCity(rs.getString("city"));
                    station.setState(rs.getString("state"));
                    stations.add(station);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting stations by state: " + e.getMessage());
            e.printStackTrace();
        }

        return stations;
    }

    /**
     * Get all unique states from stations
     */
    public List<String> getAllStates() {
        List<String> states = new ArrayList<>();
        String sql = "SELECT DISTINCT state FROM stations WHERE state IS NOT NULL ORDER BY state";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                states.add(rs.getString("state"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all states: " + e.getMessage());
            e.printStackTrace();
        }

        return states;
    }

    /**
     * Get stations by city
     */
    public List<Station> getStationsByCity(String city) {
        List<Station> stations = new ArrayList<>();
        String sql = "SELECT station_id, station_code, name, city, state FROM stations WHERE city = ? ORDER BY name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, city);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Station station = new Station();
                    station.setStationId(rs.getInt("station_id"));
                    station.setStationCode(rs.getString("station_code"));
                    station.setName(rs.getString("name"));
                    station.setCity(rs.getString("city"));
                    station.setState(rs.getString("state"));
                    stations.add(station);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting stations by city: " + e.getMessage());
            e.printStackTrace();
        }

        return stations;
    }

    /**
     * Get station count
     */
    public int getStationCount() {
        String sql = "SELECT COUNT(*) FROM stations";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting station count: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }
}
