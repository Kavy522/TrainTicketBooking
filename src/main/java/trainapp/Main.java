package trainapp;

import trainapp.util.DBConnection;

import java.sql.Connection;

public class Main {
    public static void main(String[] args){
        Connection connection = DBConnection.getConnection();
        System.out.println(connection);
    }
}