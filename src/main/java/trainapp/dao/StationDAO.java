package trainapp.dao;

import trainapp.model.Station;
import trainapp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for railway station management.
 * Provides comprehensive CRUD operations and search functionality for station records.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Complete CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>Search stations by ID, name, city, or state</li>
 *   <li>Retrieve all stations with alphabetical sorting</li>
 *   <li>Get unique lists of cities and states for filtering</li>
 *   <li>Support for station codes and geographical information</li>
 * </ul>
 *
 * <p>Security and reliability features:
 * <ul>
 *   <li>SQL injection prevention through prepared statements</li>
 *   <li>Proper resource management with try-with-resources</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Input validation for data integrity</li>
 * </ul>
 */
public class StationDAO {

    // -------------------------------------------------------------------------
    // Read Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a station by its unique database ID.
     *
     * @param stationId the unique station identifier
     * @return Station object if found, null otherwise
     * @throws IllegalArgumentException if stationId is not positive
     */
    public Station getStationById(int stationId) {
        if (stationId <= 0) {
            throw new IllegalArgumentException("Station ID must be positive");
        }

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
     * Retrieves a station by its exact name.
     * Performs case-sensitive matching against the station name.
     *
     * @param name the exact station name to search for
     * @return Station object if found, null otherwise
     * @throws IllegalArgumentException if name is null or empty
     */
    public Station getStationByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Station name cannot be null or empty");
        }

        String sql = "SELECT station_id, station_code, name, city, state FROM stations WHERE name = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

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
            System.err.println("Error getting station by name: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieves all stations in the system, sorted alphabetically by name.
     *
     * @return List of all Station objects, empty list if none found
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

    // -------------------------------------------------------------------------
    // Create Operations
    // -------------------------------------------------------------------------

    /**
     * Adds a new station to the database.
     * The station ID will be auto-generated by the database.
     *
     * @param station Station object containing station code, name, city, and state
     * @return true if station was added successfully, false otherwise
     * @throws IllegalArgumentException if station is null or has invalid data
     */
    public boolean addStation(Station station) {
        if (station == null) {
            throw new IllegalArgumentException("Station cannot be null");
        }
        if (station.getName() == null || station.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Station name cannot be null or empty");
        }
        if (station.getStationCode() == null || station.getStationCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Station code cannot be null or empty");
        }

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

    // -------------------------------------------------------------------------
    // Update Operations
    // -------------------------------------------------------------------------

    /**
     * Updates an existing station's information.
     * All fields except the station ID can be modified.
     *
     * @param station Station object with updated information (must have valid station ID)
     * @return true if station was updated successfully, false otherwise
     * @throws IllegalArgumentException if station is null, has invalid ID, or missing required fields
     */
    public boolean updateStation(Station station) {
        if (station == null) {
            throw new IllegalArgumentException("Station cannot be null");
        }
        if (station.getStationId() <= 0) {
            throw new IllegalArgumentException("Station must have a valid ID for update");
        }
        if (station.getName() == null || station.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Station name cannot be null or empty");
        }
        if (station.getStationCode() == null || station.getStationCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Station code cannot be null or empty");
        }

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

    // -------------------------------------------------------------------------
    // Delete Operations
    // -------------------------------------------------------------------------

    /**
     * Deletes a station from the database by its ID.
     *
     * <p><b>Warning:</b> This operation may fail if the station is referenced
     * by existing bookings, schedules, or other records due to foreign key constraints.
     *
     * @param stationId the unique ID of the station to delete
     * @return true if station was deleted successfully, false otherwise
     * @throws IllegalArgumentException if stationId is not positive
     */
    public boolean deleteStation(int stationId) {
        if (stationId <= 0) {
            throw new IllegalArgumentException("Station ID must be positive");
        }

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

    // -------------------------------------------------------------------------
    // Analytics & Utility Operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves all unique states from the stations database.
     * Useful for creating state filter dropdowns or geographic analysis.
     *
     * @return List of unique state names, sorted alphabetically, empty list if none found
     */
    public List<String> getAllUniqueStates() {
        List<String> states = new ArrayList<>();
        String sql = "SELECT DISTINCT state FROM stations WHERE state IS NOT NULL ORDER BY state";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                states.add(rs.getString("state"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all unique states: " + e.getMessage());
            e.printStackTrace();
        }

        return states;
    }

    /**
     * Retrieves all unique cities from the stations database.
     * Useful for creating city filter dropdowns or route planning features.
     *
     * @return List of unique city names, sorted alphabetically, empty list if none found
     */
    public List<String> getAllUniqueCities() {
        List<String> cities = new ArrayList<>();
        String sql = "SELECT DISTINCT city FROM stations WHERE city IS NOT NULL ORDER BY city";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cities.add(rs.getString("city"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all unique cities: " + e.getMessage());
            e.printStackTrace();
        }

        return cities;
    }
}
