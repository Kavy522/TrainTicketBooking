package trainapp.dao;
import trainapp.model.Train;
import trainapp.model.Route;

import java.sql.*;
import java.util.*;

import static trainapp.util.DBConnection.getConnection;
public class TrainDAO {
    public boolean createTrain(Train train) {
        String sql = "INSERT INTO trains (train_number, name, source_station_id, destination_station_id, total_coaches, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, train.getTrainNumber());
            ps.setString(2, train.getName());
            ps.setInt(3, train.getSourceStationId());
            ps.setInt(4, train.getDestinationStationId());
            ps.setInt(5, train.getTotalCoaches());;
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }
            return false;
    }
    public Train getTrainById(int trainId) {
        String sql = "SELECT * FROM trains WHERE train_id = ?";
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, trainId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Train train=new Train(
                        rs.getInt("train_id"),
                        rs.getString("train_number"),
                        rs.getString("name"),
                        rs.getInt("source_station_id"),
                        rs.getInt("destination_station_id"),
                        rs.getInt("total_coaches")
                );
                return train;
            }

        } catch (SQLException e) {
            System.out.println(e);
        }
        return null;
    }
    public Train getTrainByNumber(String trainNumber) {
        String sql = "SELECT * FROM trains WHERE train_number = ?";

        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, trainNumber);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Train train=new Train(
                        rs.getInt("train_id"),
                        rs.getString("train_number"),
                        rs.getString("name"),
                        rs.getInt("source_station_id"),
                        rs.getInt("destination_station_id"),
                        rs.getInt("total_coaches")
                );
                return train;
            }
            return null;

        } catch (SQLException e) {
            System.out.println(e);
        }
        return null;
    }
    public List<Train> getAllTrains() {
        String sql = "SELECT * FROM trains";
        List<Train> trainList = new ArrayList<>();
        try {Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Train trn=new Train(
                        rs.getInt("train_id"),
                        rs.getString("train_number"),
                        rs.getString("name"),
                        rs.getInt("source_station_id"),
                        rs.getInt("destination_station_id"),
                        rs.getInt("total_coaches")
                );
                trainList.add(trn);
            }
        }catch (SQLException e) {
            System.out.println(e);
        }
        return trainList;
    }
    public List<Train> searchTrainsByName(String name) {
        String sql = "SELECT * FROM trains WHERE name LIKE ?";
        List<Train> trainList = new ArrayList<>();
        try {Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Train trn=new Train(
                        rs.getInt("train_id"),
                        rs.getString("train_number"),
                        rs.getString("name"),
                        rs.getInt("source_station_id"),
                        rs.getInt("destination_station_id"),
                        rs.getInt("total_coaches")
                );
                trainList.add(trn);
            }
        }catch (SQLException e) {
            System.out.println(e);
        }
        return trainList;
    }
    public List<Train> searchTrainsByRoute(int sourceId, int destId) {
        String sql = "SELECT * FROM trains WHERE source_station_id = ? AND destination_station_id = ?";
        List<Train> trainList = new ArrayList<>();
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM trains WHERE source_station_id = ? AND destination_station_id = ?");
            ps.setInt(1, sourceId);
            ps.setInt(2, destId);
            ResultSet rs = ps.executeQuery();
            List<Train> list = new ArrayList<>();
            while (rs.next()) {
                Train trn = new Train(
                        rs.getInt("train_id"),
                        rs.getString("train_number"),
                        rs.getString("name"),
                        rs.getInt("source_station_id"),
                        rs.getInt("destination_station_id"),
                        rs.getInt("total_coaches")
                );
                trainList.add(trn);
            }
        }
        catch (SQLException e) {
                System.out.println(e);
        }
            return trainList;
    }
    public boolean deleteTrain(int trainId) {
        String sql = "DELETE FROM trains WHERE train_id = ?";
        int rows=0;
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, trainId);
            rows = ps.executeUpdate();
        }catch (SQLException e) {
            System.out.println(e);
        }
        return rows > 0;
    }
    public List<Route> getTrainRoute(int trainId) {
        String sql = "SELECT * FROM train_schedule WHERE train_id = ? ORDER BY schedule_id";
        List<Route> scheduleList = new ArrayList<>();
        try {
            Connection con = getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, trainId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Route route=new Route(
                        rs.getInt("schedule_id"),
                        rs.getInt("train_id"),
                        rs.getInt("station_id"),
                        rs.getTime("arrival_time"),
                        rs.getTime("departure_time"),
                        rs.getInt("day_number"),
                        rs.getInt("sequence_order")
                );
                scheduleList.add(route);
            }
        }
        catch (SQLException e) {
            System.out.println(e);
        }
        return scheduleList;
    }
}
