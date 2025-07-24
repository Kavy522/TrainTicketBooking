package trainapp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static String URL = "jdbc:mysql://192.168.1.15:3306/Irctc";
    private static String USERNAME = "project";
    private static String PASSWORD = "projectS2";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("Exception in DBConnection:");
            e.printStackTrace();
            return null;
        }catch (SQLException e){
            System.out.println("Exception in DBConnection:");
            e.printStackTrace();
            return null;
        }
    }
}
