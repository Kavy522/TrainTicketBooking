package trainapp.dao;
import trainapp.model.Station;
import trainapp.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StationDAO {

    public boolean insertStation(Station station) {
        String sql = "INSERT IGNORE INTO stations (station_code, name, city, state) VALUES (?, ?, ?, ?)";

        try {

            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, station.getStationCode());
            stmt.setString(2, station.getName());
            stmt.setString(3, station.getCity());
            stmt.setString(4, station.getState());

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Station> getAllStations() {
        List<Station> stationList = new ArrayList<>();
        String sql = "SELECT * FROM stations";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Station station = new Station(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                );
                stationList.add(station);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stationList;
    }

    public Station getStationByCode(String code) {
        String sql = "SELECT * FROM stations WHERE station_code = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Station(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
